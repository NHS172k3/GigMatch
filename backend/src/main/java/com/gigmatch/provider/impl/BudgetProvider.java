package com.gigmatch.provider.impl;

import com.gigmatch.portfolio.PortfolioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Simulated provider: Budget Builders.
 * Broad targeting, low prices.
 * Lower quality (3.8/5), low quote ($20–$80), very fast response (5–20ms).
 */
@Service
public class BudgetProvider extends AbstractBidder {

    public BudgetProvider(PortfolioRepository portfolioRepository,
                          @Qualifier("bidderExecutor") Executor bidderExecutor) {
        super(portfolioRepository, bidderExecutor);
    }

    @Override public String getProviderKey() { return "budget"; }
    @Override public long   getProviderId()  { return 4L; }

    /** Empty = always eligible (broad targeting) */
    @Override protected List<String> targetCategories() { return List.of(); }
    @Override protected int minQuoteCents() { return 2000; }
    @Override protected int maxQuoteCents() { return 8000; }
    @Override protected int minLatencyMs()  { return 5; }
    @Override protected int maxLatencyMs()  { return 20; }
}
