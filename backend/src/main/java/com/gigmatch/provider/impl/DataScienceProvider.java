package com.gigmatch.provider.impl;

import com.gigmatch.portfolio.PortfolioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Simulated provider: DataScience Experts.
 * Niche ML/data-science targeting.
 * Highest quality (5.0/5), highest quote ($200–$500), slower response (50–90ms).
 */
@Service
public class DataScienceProvider extends AbstractBidder {

    public DataScienceProvider(PortfolioRepository portfolioRepository,
                                @Qualifier("bidderExecutor") Executor bidderExecutor) {
        super(portfolioRepository, bidderExecutor);
    }

    @Override public String getProviderKey() { return "data-science"; }
    @Override public long   getProviderId()  { return 5L; }

    @Override protected List<String> targetCategories() {
        return List.of("data-science", "ml", "analytics", "data");
    }
    @Override protected int minQuoteCents() { return 20000; }
    @Override protected int maxQuoteCents() { return 50000; }
    @Override protected int minLatencyMs()  { return 50; }
    @Override protected int maxLatencyMs()  { return 90; }
}
