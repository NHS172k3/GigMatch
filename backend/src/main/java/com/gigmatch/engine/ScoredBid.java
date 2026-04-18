package com.gigmatch.engine;

import com.gigmatch.match.dto.ProviderBid;

/**
 * Pairs a raw provider bid with its quality-adjusted effective score.
 *
 * effectiveScore = predictedSuccess × competitiveness
 * where:
 *   predictedSuccess = QualityScoreModel.predictSuccess(completionRate, ratingNorm, skillsMatch)
 *   competitiveness  = clamp((budgetCents - quoteCents) / budgetCents, 0, 1)
 */
public record ScoredBid(
    ProviderBid original,
    double predictedSuccessRate,
    double competitiveness,
    double effectiveScore
) {}
