package com.gigmatch.match.dto;

import java.util.List;

/**
 * The response returned from POST /api/v1/matches.
 * Always returns 200; check {@code hasMatch} to know if a provider was found.
 */
public record MatchResult(
    String requestId,
    boolean hasMatch,
    String winnerProviderKey,
    Long   winnerProviderId,
    Long   winnerPortfolioId,
    String portfolioSampleUrl,
    String portfolioTitle,
    int    clearingPriceCents,
    double predictedSuccessRate,
    double effectiveScore,
    int    estimatedDays,
    long   matchDurationMs,
    String outcome,                      // MATCHED | NO_MATCH | TIMEOUT
    List<String> participatingProviders
) {}
