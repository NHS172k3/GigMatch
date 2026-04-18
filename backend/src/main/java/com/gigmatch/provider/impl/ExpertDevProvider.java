package com.gigmatch.provider.impl;

import com.gigmatch.portfolio.PortfolioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Simulated provider: Expert Dev Studio.
 * Targets software development and backend work.
 * High quality (4.9/5), high quote ($150–$300), medium latency (20–60ms).
 */
@Service
public class ExpertDevProvider extends AbstractBidder {

    public ExpertDevProvider(PortfolioRepository portfolioRepository,
                              @Qualifier("bidderExecutor") Executor bidderExecutor) {
        super(portfolioRepository, bidderExecutor);
    }

    @Override public String getProviderKey() { return "expert-dev"; }
    @Override public long   getProviderId()  { return 1L; }

    @Override protected List<String> targetCategories() {
        return List.of("software-dev", "backend", "api");
    }
    @Override protected int minQuoteCents() { return 15000; }
    @Override protected int maxQuoteCents() { return 30000; }
    @Override protected int minLatencyMs()  { return 20; }
    @Override protected int maxLatencyMs()  { return 60; }
}
