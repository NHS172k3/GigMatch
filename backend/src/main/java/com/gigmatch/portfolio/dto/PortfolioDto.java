package com.gigmatch.portfolio.dto;

import com.gigmatch.portfolio.Portfolio;

import java.time.LocalDateTime;

public record PortfolioDto(
    Long id,
    Long providerId,
    String title,
    String description,
    String sampleUrl,
    String category,
    boolean active,
    LocalDateTime createdAt
) {
    public static PortfolioDto from(Portfolio p) {
        return new PortfolioDto(
            p.getId(),
            p.getProvider().getId(),
            p.getTitle(),
            p.getDescription(),
            p.getSampleUrl(),
            p.getCategory(),
            p.isActive(),
            p.getCreatedAt()
        );
    }
}
