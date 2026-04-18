package com.gigmatch.match.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record JobRequest(
    @NotBlank String requestId,
    @NotBlank String clientId,
    @NotBlank String jobTitle,
    @NotBlank String jobCategory,
    List<String> requiredSkills,
    @NotNull @Min(100) Integer budgetCents,
    String urgencyLevel,
    Instant postedAt
) {
    public JobRequest {
        if (requiredSkills == null) requiredSkills = List.of();
        if (urgencyLevel == null)   urgencyLevel   = "MEDIUM";
        if (postedAt == null)       postedAt        = Instant.now();
    }
}
