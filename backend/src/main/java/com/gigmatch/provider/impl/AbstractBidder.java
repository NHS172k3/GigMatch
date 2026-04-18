package com.gigmatch.provider.impl;

import com.gigmatch.match.dto.JobRequest;
import com.gigmatch.match.dto.ProviderBid;
import com.gigmatch.portfolio.PortfolioRepository;
import com.gigmatch.provider.BidderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Base class for the 5 simulated bidder beans.
 * Each subclass declares its target categories, quote range, simulated latency range,
 * and seeded provider/portfolio IDs.
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractBidder implements BidderService {

    protected final PortfolioRepository portfolioRepository;
    protected final Executor bidderExecutor;

    /** Skill/job categories this provider targets. */
    protected abstract List<String> targetCategories();

    /** Quote range in cents (inclusive). */
    protected abstract int minQuoteCents();
    protected abstract int maxQuoteCents();

    /** Simulated network/processing latency range in ms. */
    protected abstract int minLatencyMs();
    protected abstract int maxLatencyMs();

    @Override
    public boolean isEligible(JobRequest request) {
        List<String> targets = targetCategories();
        if (targets.isEmpty()) return true; // broad targeting
        boolean categoryMatch = targets.contains(request.jobCategory());
        boolean skillMatch = request.requiredSkills().stream().anyMatch(targets::contains);
        return categoryMatch || skillMatch;
    }

    @Override
    public CompletableFuture<ProviderBid> bid(JobRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            simulateLatency();
            int quote = randomQuote();
            // Provider internally decides to pass if quote would exceed budget
            if (quote > request.budgetCents()) {
                log.debug("{} passing — quote {} > budget {}", getProviderKey(), quote, request.budgetCents());
                return null;
            }
            long portfolioId = resolvePortfolioId();
            int days = ThreadLocalRandom.current().nextInt(3, 15);
            return new ProviderBid(
                getProviderKey(),
                getProviderId(),
                portfolioId,
                quote,
                days,
                "category:" + request.jobCategory()
            );
        }, bidderExecutor);
    }

    private int randomQuote() {
        return ThreadLocalRandom.current().nextInt(minQuoteCents(), maxQuoteCents() + 1);
    }

    private void simulateLatency() {
        try {
            int ms = ThreadLocalRandom.current().nextInt(minLatencyMs(), maxLatencyMs() + 1);
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected long resolvePortfolioId() {
        return portfolioRepository.findByProviderIdAndActiveTrue(getProviderId())
                .stream()
                .findFirst()
                .map(p -> p.getId())
                .orElse((long) getProviderId()); // fallback to provider ID as placeholder
    }
}
