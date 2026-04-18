package com.gigmatch.provider.impl;

import com.gigmatch.portfolio.PortfolioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Simulated provider: DesignPro Creative.
 * Targets design and branding work.
 * High quality (4.7/5), medium quote ($80–$200), fast response (10–40ms).
 */
@Service
public class DesignProProvider extends AbstractBidder {

    public DesignProProvider(PortfolioRepository portfolioRepository,
                              @Qualifier("bidderExecutor") Executor bidderExecutor) {
        super(portfolioRepository, bidderExecutor);
    }

    @Override public String getProviderKey() { return "design-pro"; }
    @Override public long   getProviderId()  { return 2L; }

    @Override protected List<String> targetCategories() {
        return List.of("design", "branding", "ui-ux");
    }
    @Override protected int minQuoteCents() { return 8000; }
    @Override protected int maxQuoteCents() { return 20000; }
    @Override protected int minLatencyMs()  { return 10; }
    @Override protected int maxLatencyMs()  { return 40; }
}
