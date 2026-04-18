package com.gigmatch.provider.dto;

import com.gigmatch.portfolio.Portfolio;
import com.gigmatch.provider.Provider;
import com.gigmatch.skill.SkillOffering;

import java.util.List;

public record ProviderDto(
    Long id,
    String name,
    String providerKey,
    double avgRating,
    double completionRate,
    int dailyJobCapacity,
    int totalActiveJobs,
    String status,
    List<String> skillCategories,
    List<PortfolioSummary> portfolios
) {
    public record PortfolioSummary(Long id, String title, String sampleUrl, String category) {}

    public static ProviderDto from(Provider p) {
        List<String> categories = p.getSkillOfferings().stream()
                .map(SkillOffering::getSkillCategory)
                .distinct()
                .toList();

        List<PortfolioSummary> portfolioSummaries = p.getPortfolios().stream()
                .filter(Portfolio::isActive)
                .map(port -> new PortfolioSummary(port.getId(), port.getTitle(),
                                                  port.getSampleUrl(), port.getCategory()))
                .toList();

        return new ProviderDto(
            p.getId(), p.getName(), p.getProviderKey(),
            p.getAvgRating(), p.getCompletionRate(),
            p.getDailyJobCapacity(), p.getTotalActiveJobs(), p.getStatus(),
            categories, portfolioSummaries
        );
    }
}
