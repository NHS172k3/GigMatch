package com.gigmatch.engine;

import com.gigmatch.match.MatchLog;
import com.gigmatch.match.MatchLogRepository;
import com.gigmatch.match.dto.JobRequest;
import com.gigmatch.match.dto.MatchResult;
import com.gigmatch.match.dto.ProviderBid;
import com.gigmatch.metrics.MatchMetrics;
import com.gigmatch.pacing.CapacityPacer;
import com.gigmatch.pacing.PacingService;
import com.gigmatch.portfolio.Portfolio;
import com.gigmatch.portfolio.PortfolioRepository;
import com.gigmatch.provider.BidderService;
import com.gigmatch.provider.Provider;
import com.gigmatch.provider.ProviderRepository;
import com.gigmatch.provider.ProviderRegistry;
import com.gigmatch.score.QualityScoreModel;
import com.gigmatch.skill.SkillMatchService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Core matching engine — orchestrates the full real-time auction flow.
 *
 * Flow:
 *   1. Find eligible providers by skill category (DB query)
 *   2. Filter by capacity tokens (Redis read-only check)
 *   3. Deduplication filter (Redis — skip providers seen for this client recently)
 *   4. Fan-out to provider beans with 100ms deadline (CompletableFuture)
 *   5. Apply quality score: predictedSuccess × competitiveness
 *   6. Second-price auction: winner = highest effectiveScore
 *   7. Atomic capacity deduction (Redis Lua)
 *   8. Async fire-and-forget DB writes (match log + activeJobs counter)
 *   9. Record Prometheus metrics
 *  10. Return MatchResult
 */
@Service
@Slf4j
public class MatchingEngine {

    private final ProviderRepository   providerRepository;
    private final PortfolioRepository  portfolioRepository;
    private final MatchLogRepository   matchLogRepository;
    private final ProviderRegistry     providerRegistry;
    private final QualityScoreModel    qualityScoreModel;
    private final SkillMatchService    skillMatchService;
    private final CapacityPacer        capacityPacer;
    private final PacingService        pacingService;
    private final MatchingAuction      matchingAuction;
    private final MatchMetrics         matchMetrics;
    private final Executor             dbWriteExecutor;

    @Value("${gigmatch.matching.timeout-ms:100}")
    private long timeoutMs;

    /** Cached provider snapshot — loaded once at startup, refreshed on admin writes. */
    private volatile Map<Long, Provider> providerCache = new ConcurrentHashMap<>();

    @PostConstruct
    void loadProviderCache() {
        providerCache = providerRepository.findAll().stream()
                .collect(Collectors.toMap(Provider::getId, p -> p));
        log.info("Provider cache loaded: {} providers", providerCache.size());
    }

    /** Call this after any provider create/update so the cache stays consistent. */
    public void refreshProviderCache() {
        loadProviderCache();
    }

    public MatchingEngine(ProviderRepository providerRepository,
                          PortfolioRepository portfolioRepository,
                          MatchLogRepository matchLogRepository,
                          ProviderRegistry providerRegistry,
                          QualityScoreModel qualityScoreModel,
                          SkillMatchService skillMatchService,
                          CapacityPacer capacityPacer,
                          PacingService pacingService,
                          MatchingAuction matchingAuction,
                          MatchMetrics matchMetrics,
                          @Qualifier("dbWriteExecutor") Executor dbWriteExecutor) {
        this.providerRepository = providerRepository;
        this.portfolioRepository = portfolioRepository;
        this.matchLogRepository = matchLogRepository;
        this.providerRegistry = providerRegistry;
        this.qualityScoreModel = qualityScoreModel;
        this.skillMatchService = skillMatchService;
        this.capacityPacer = capacityPacer;
        this.pacingService = pacingService;
        this.matchingAuction = matchingAuction;
        this.matchMetrics = matchMetrics;
        this.dbWriteExecutor = dbWriteExecutor;
    }

    public MatchResult runMatch(JobRequest request) {
        long start = System.currentTimeMillis();

        // Urgency adjusts how long we wait for provider systems to respond.
        // HIGH: maximise provider participation — worth waiting longer for the best match.
        // LOW:  quick-and-good-enough — grab whatever's available fast.
        long effectiveTimeout = switch (request.urgencyLevel() != null ? request.urgencyLevel().toUpperCase() : "MEDIUM") {
            case "HIGH" -> (long)(timeoutMs * 1.5);  // 150ms — more company systems respond
            case "LOW"  -> (long)(timeoutMs * 0.7);  // 70ms  — fast, accepts fewer bids
            default     -> timeoutMs;                 // 100ms — standard
        };

        // 1. Find eligible providers — isEligible() on each bean handles both targeted and
        //    broad-targeting providers (GeneralistProvider, BudgetProvider return true for all).
        List<BidderService> finalBidders = providerRegistry.getEligibleBidders(request);

        // 2. Filter by capacity (Redis read-only)
        List<BidderService> paced = finalBidders.stream()
                .filter(b -> {
                    Provider cached = providerCache.get(b.getProviderId());
                    int cap = cached != null ? cached.getDailyJobCapacity() : 10;
                    return capacityPacer.hasCapacity(b.getProviderId(), cap);
                })
                .toList();

        // 3. Deduplication
        List<BidderService> deduped = paced.stream()
                .filter(b -> !pacingService.isRecentMatch(request.clientId(), b.getProviderId()))
                .toList();

        // 4. Fan-out with deadline
        List<CompletableFuture<ProviderBid>> futures = deduped.stream()
                .map(b -> b.bid(request))
                .toList();

        boolean timedOut = false;
        List<ProviderBid> bids = collectWithTimeout(futures, effectiveTimeout);
        if (bids.isEmpty() && !futures.isEmpty()) timedOut = true;

        // 5. Score each bid — use in-memory cache, not a DB query per request
        Map<Long, Provider> providerMap = providerCache;

        List<ScoredBid> scoredBids = bids.stream()
                .filter(Objects::nonNull)
                .map(b -> scoreBid(b, request, providerMap))
                .filter(sb -> sb.effectiveScore() > 0)
                .toList();

        // 6. Second-price auction
        Optional<AuctionWinner> winnerOpt = matchingAuction.selectWinner(scoredBids);
        long duration = System.currentTimeMillis() - start;

        List<String> participants = deduped.stream().map(BidderService::getProviderKey).toList();

        if (winnerOpt.isPresent()) {
            AuctionWinner winner = winnerOpt.get();
            ProviderBid   winBid = winner.winner().original();
            Provider      winProvider = providerMap.get(winBid.providerId());

            // 7. Atomic capacity deduction
            capacityPacer.deductCapacity(winBid.providerId(),
                    winProvider != null ? winProvider.getDailyJobCapacity() : 10);

            // 8. Async DB writes (fire and forget)
            final long finalDuration = duration;
            final String outcome = "MATCHED";
            CompletableFuture.runAsync(() ->
                providerRepository.incrementActiveJobs(winBid.providerId()), dbWriteExecutor);
            CompletableFuture.runAsync(() ->
                matchLogRepository.save(buildLog(request, winner, participants, finalDuration, outcome)),
                dbWriteExecutor);

            // Resolve portfolio info
            Portfolio portfolio = portfolioRepository.findById(winBid.portfolioId()).orElse(null);
            String sampleUrl = portfolio != null ? portfolio.getSampleUrl() : null;
            String title     = portfolio != null ? portfolio.getTitle()     : winBid.providerKey();

            MatchResult result = new MatchResult(
                request.requestId(), true,
                winBid.providerKey(), winBid.providerId(), winBid.portfolioId(),
                sampleUrl, title,
                winner.clearingPriceCents(),
                winner.winner().predictedSuccessRate(),
                winner.winner().effectiveScore(),
                winBid.estimatedDays(),
                duration, outcome, participants
            );

            // 9. Metrics
            matchMetrics.recordMatch(result, duration);
            return result;
        }

        String outcome = timedOut ? "TIMEOUT" : "NO_MATCH";
        final long finalDuration = duration;
        CompletableFuture.runAsync(() ->
            matchLogRepository.save(buildLog(request, null, participants, finalDuration, outcome)),
            dbWriteExecutor);

        MatchResult result = new MatchResult(
            request.requestId(), false,
            null, null, null, null, null,
            0, 0.0, 0.0, 0,
            duration, outcome, participants
        );
        matchMetrics.recordMatch(result, duration);
        return result;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private ScoredBid scoreBid(ProviderBid bid, JobRequest request, Map<Long, Provider> providerMap) {
        Provider provider = providerMap.get(bid.providerId());
        if (provider == null) return new ScoredBid(bid, 0, 0, 0);

        double skillsMatch   = skillMatchService.computeSkillsMatch(request.requiredSkills(), bid.providerId());
        double ratingNorm    = provider.getAvgRating() / 5.0;
        double successRate   = qualityScoreModel.predictSuccess(
                provider.getCompletionRate(), ratingNorm, skillsMatch);
        double competitiveness = Math.max(0.0,
                (double)(request.budgetCents() - bid.quoteCents()) / request.budgetCents());
        double effectiveScore  = successRate * competitiveness;

        return new ScoredBid(bid, successRate, competitiveness, effectiveScore);
    }

    private List<ProviderBid> collectWithTimeout(List<CompletableFuture<ProviderBid>> futures, long timeoutMs) {
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allOf.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // Timeout or interruption — collect whatever completed
        }
        return futures.stream()
                .filter(f -> f.isDone() && !f.isCompletedExceptionally())
                .map(f -> {
                    try { return f.getNow(null); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private MatchLog buildLog(JobRequest request, AuctionWinner winner,
                               List<String> participants, long duration, String outcome) {
        MatchLog.MatchLogBuilder log = MatchLog.builder()
                .requestId(request.requestId())
                .clientId(request.clientId())
                .jobCategory(request.jobCategory())
                .requiredSkills(String.join(",", request.requiredSkills()))
                .budgetCents(request.budgetCents())
                .urgencyLevel(request.urgencyLevel())
                .durationMs(duration)
                .outcome(outcome)
                .participatingProviders(String.join(",", participants));

        if (winner != null) {
            ProviderBid wb = winner.winner().original();
            log.winnerProviderKey(wb.providerKey())
               .winnerProviderId(wb.providerId())
               .clearingPriceCents(winner.clearingPriceCents())
               .predictedSuccessRate(winner.winner().predictedSuccessRate())
               .effectiveScore(winner.winner().effectiveScore());
        }

        return log.build();
    }

}
