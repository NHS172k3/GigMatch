package com.gigmatch.metrics;

import com.gigmatch.match.dto.MatchResult;
import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Prometheus metrics for the matching engine.
 *
 * Metrics exposed:
 *   gigmatch_match_duration_ms_bucket  — histogram of auction latency (p50/p95/p99)
 *   gigmatch_matches_total             — counter by outcome (MATCHED|NO_MATCH|TIMEOUT)
 *   gigmatch_clearing_price_cents      — distribution summary of clearing prices
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchMetrics {

    private final MeterRegistry registry;

    private Timer      matchTimer;
    private Counter    matchedCounter;
    private Counter    noMatchCounter;
    private Counter    timeoutCounter;
    private DistributionSummary clearingPriceSummary;

    @PostConstruct
    void init() {
        matchTimer = Timer.builder("gigmatch.match.duration.ms")
                .description("End-to-end matching engine latency in milliseconds")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        matchedCounter = Counter.builder("gigmatch.matches.total")
                .tag("outcome", "MATCHED")
                .description("Total successful matches")
                .register(registry);

        noMatchCounter = Counter.builder("gigmatch.matches.total")
                .tag("outcome", "NO_MATCH")
                .description("Total requests with no eligible provider")
                .register(registry);

        timeoutCounter = Counter.builder("gigmatch.matches.total")
                .tag("outcome", "TIMEOUT")
                .description("Total requests where all bids timed out")
                .register(registry);

        clearingPriceSummary = DistributionSummary.builder("gigmatch.clearing.price.cents")
                .description("Distribution of clearing prices in cents")
                .baseUnit("cents")
                .register(registry);
    }

    public void recordMatch(MatchResult result, long durationMs) {
        matchTimer.record(durationMs, TimeUnit.MILLISECONDS);

        switch (result.outcome()) {
            case "MATCHED" -> {
                matchedCounter.increment();
                clearingPriceSummary.record(result.clearingPriceCents());
            }
            case "NO_MATCH" -> noMatchCounter.increment();
            case "TIMEOUT"  -> timeoutCounter.increment();
            default -> log.warn("Unknown match outcome: {}", result.outcome());
        }
    }

    /**
     * Register a per-provider capacity gauge. Called when stats endpoint is hit.
     */
    public void registerCapacityGauge(long providerId, String providerKey,
                                       java.util.function.Supplier<Number> valueSupplier) {
        Gauge.builder("gigmatch.provider.capacity.remaining", valueSupplier, s -> s.get().doubleValue())
                .tag("providerId", String.valueOf(providerId))
                .tag("providerKey", providerKey)
                .description("Remaining token bucket capacity for this provider")
                .register(registry);
    }
}
