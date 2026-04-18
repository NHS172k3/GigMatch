package com.gigmatch.match.dto;

import com.gigmatch.match.MatchLog;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record MatchLogDto(
    Long id,
    String requestId,
    String clientId,
    String jobCategory,
    List<String> requiredSkills,
    Integer budgetCents,
    String urgencyLevel,
    String winnerProviderKey,
    Long winnerProviderId,
    Integer clearingPriceCents,
    Double predictedSuccessRate,
    Double effectiveScore,
    long durationMs,
    String outcome,
    List<String> participatingProviders,
    LocalDateTime createdAt
) {
    public static MatchLogDto from(MatchLog log) {
        return new MatchLogDto(
            log.getId(),
            log.getRequestId(),
            log.getClientId(),
            log.getJobCategory(),
            log.getRequiredSkills() != null
                ? Arrays.asList(log.getRequiredSkills().split(","))
                : List.of(),
            log.getBudgetCents(),
            log.getUrgencyLevel(),
            log.getWinnerProviderKey(),
            log.getWinnerProviderId(),
            log.getClearingPriceCents(),
            log.getPredictedSuccessRate(),
            log.getEffectiveScore(),
            log.getDurationMs(),
            log.getOutcome(),
            log.getParticipatingProviders() != null
                ? Arrays.asList(log.getParticipatingProviders().split(","))
                : List.of(),
            log.getCreatedAt()
        );
    }
}
