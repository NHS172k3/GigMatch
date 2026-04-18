package com.gigmatch.provider.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record CreateProviderRequest(
    @NotBlank String name,
    @NotBlank String providerKey,
    @Min(1) @Max(5) double avgRating,
    @Min(0) @Max(1) double completionRate,
    @Min(1) int dailyJobCapacity,
    @NotEmpty List<String> skillCategories
) {}
