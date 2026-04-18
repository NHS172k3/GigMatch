package com.gigmatch.provider;

import com.gigmatch.match.dto.JobRequest;
import com.gigmatch.match.dto.ProviderBid;

import java.util.concurrent.CompletableFuture;

/**
 * Implemented by each of the 5 simulated provider beans.
 * Spring collects all beans implementing this interface into a List<BidderService>
 * which is injected into {@link ProviderRegistry}.
 */
public interface BidderService {

    /** Unique string identifier, e.g. "expert-dev". Must match providers.provider_key. */
    String getProviderKey();

    /** FK to providers.id — must match the seeded row in V5__seed_data.sql */
    long getProviderId();

    /**
     * Fast synchronous eligibility check before fanning out async bids.
     * Returns true if this provider can serve the given job category/skills.
     */
    boolean isEligible(JobRequest request);

    /**
     * Asynchronously submit a bid for the given job request.
     * Runs on the {@code bidderExecutor} thread pool.
     * Returns null if the provider decides to pass after internal checks.
     */
    CompletableFuture<ProviderBid> bid(JobRequest request);
}
