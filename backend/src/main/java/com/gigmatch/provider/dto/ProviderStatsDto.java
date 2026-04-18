package com.gigmatch.provider.dto;

public record ProviderStatsDto(
    long providerId,
    String providerKey,
    String name,
    long totalMatches,
    long totalEarningsCents,
    double winRatePct,
    double avgClearingPriceCents,
    double capacityRemaining,
    int dailyJobCapacity
) {}
