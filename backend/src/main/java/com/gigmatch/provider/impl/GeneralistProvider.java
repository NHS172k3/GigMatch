package com.gigmatch.provider.impl;

import com.gigmatch.portfolio.PortfolioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Simulated provider: Generalist Works.
 * Broad targeting — bids on any category.
 * Medium quality (4.2/5), medium quote ($50–$150), fast response (5–30ms).
 */
@Service
public class GeneralistProvider extends AbstractBidder {

    public GeneralistProvider(PortfolioRepository portfolioRepository,
                               @Qualifier("bidderExecutor") Executor bidderExecutor) {
        super(portfolioRepository, bidderExecutor);
    }

    @Override public String getProviderKey() { return "generalist"; }
    @Override public long   getProviderId()  { return 3L; }

    /** Empty = always eligible (broad targeting) */
    @Override protected List<String> targetCategories() { return List.of(); }
    @Override protected int minQuoteCents() { return 5000; }
    @Override protected int maxQuoteCents() { return 15000; }
    @Override protected int minLatencyMs()  { return 5; }
    @Override protected int maxLatencyMs()  { return 30; }
}
