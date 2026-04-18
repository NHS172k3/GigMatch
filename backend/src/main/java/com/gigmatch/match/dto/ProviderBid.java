package com.gigmatch.match.dto;

/**
 * Internal DTO: a bid submitted by one of the 5 simulated provider beans.
 * null means the provider passed on this job request.
 *
 * @param providerKey    unique string key, e.g. "expert-dev"
 * @param providerId     FK to providers.id
 * @param portfolioId    FK to portfolios.id (the sample work to show client)
 * @param quoteCents     provider's price in integer cents (e.g. 25000 = $250.00)
 * @param estimatedDays  estimated delivery time
 * @param matchReason    debug/logging string, e.g. "skill:software-dev"
 */
public record ProviderBid(
    String providerKey,
    long   providerId,
    long   portfolioId,
    int    quoteCents,
    int    estimatedDays,
    String matchReason
) {}
