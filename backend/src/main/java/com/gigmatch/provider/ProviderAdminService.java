package com.gigmatch.provider;

import com.gigmatch.engine.MatchingEngine;
import com.gigmatch.match.MatchLogRepository;
import com.gigmatch.pacing.CapacityPacer;
import com.gigmatch.portfolio.Portfolio;
import com.gigmatch.portfolio.PortfolioRepository;
import com.gigmatch.provider.dto.CreateProviderRequest;
import com.gigmatch.provider.dto.ProviderDto;
import com.gigmatch.provider.dto.ProviderStatsDto;
import com.gigmatch.skill.SkillOffering;
import com.gigmatch.skill.SkillOfferingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ProviderAdminService {

    private final ProviderRepository       providerRepository;
    private final SkillOfferingRepository  skillOfferingRepository;
    private final PortfolioRepository      portfolioRepository;
    private final MatchLogRepository       matchLogRepository;
    private final CapacityPacer            capacityPacer;
    private final MatchingEngine           matchingEngine;

    public ProviderAdminService(ProviderRepository providerRepository,
                                SkillOfferingRepository skillOfferingRepository,
                                PortfolioRepository portfolioRepository,
                                MatchLogRepository matchLogRepository,
                                CapacityPacer capacityPacer,
                                @Lazy MatchingEngine matchingEngine) {
        this.providerRepository      = providerRepository;
        this.skillOfferingRepository = skillOfferingRepository;
        this.portfolioRepository     = portfolioRepository;
        this.matchLogRepository      = matchLogRepository;
        this.capacityPacer           = capacityPacer;
        this.matchingEngine          = matchingEngine;
    }

    @Transactional(readOnly = true)
    public List<ProviderDto> findAll() {
        return providerRepository.findAll().stream()
                .map(ProviderDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderDto findById(Long id) {
        Provider p = providerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found: " + id));
        return ProviderDto.from(p);
    }

    @Transactional
    public ProviderDto create(CreateProviderRequest req) {
        Provider provider = Provider.builder()
                .name(req.name())
                .providerKey(req.providerKey())
                .avgRating(req.avgRating())
                .completionRate(req.completionRate())
                .dailyJobCapacity(req.dailyJobCapacity())
                .totalActiveJobs(0)
                .status("ACTIVE")
                .build();
        providerRepository.save(provider);

        // Create skill offerings
        for (String category : req.skillCategories()) {
            skillOfferingRepository.save(SkillOffering.builder()
                    .provider(provider)
                    .skillCategory(category)
                    .minQuoteCents(1000)
                    .maxQuoteCents(100000)
                    .build());
        }

        // Reload with associations, then refresh engine cache
        ProviderDto result = ProviderDto.from(providerRepository.findById(provider.getId()).orElseThrow());
        matchingEngine.refreshProviderCache();
        return result;
    }

    @Transactional
    public ProviderDto update(Long id, CreateProviderRequest req) {
        Provider p = providerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found: " + id));
        p.setName(req.name());
        p.setAvgRating(req.avgRating());
        p.setCompletionRate(req.completionRate());
        p.setDailyJobCapacity(req.dailyJobCapacity());
        providerRepository.save(p);
        matchingEngine.refreshProviderCache();
        return ProviderDto.from(p);
    }

    @Transactional(readOnly = true)
    public ProviderStatsDto getStats(Long id) {
        Provider p = providerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found: " + id));

        long totalMatches  = matchLogRepository.countTotalByProviderId(id);
        long totalWins     = matchLogRepository.countWinsByProviderId(id);
        long totalEarnings = matchLogRepository.sumEarningsByProviderId(id);
        double winRate     = totalMatches > 0 ? (double) totalWins / totalMatches * 100.0 : 0.0;
        double avgClearing = totalWins > 0 ? (double) totalEarnings / totalWins : 0.0;
        double capacity    = capacityPacer.getRemainingCapacity(id, p.getDailyJobCapacity());

        return new ProviderStatsDto(
            id, p.getProviderKey(), p.getName(),
            totalMatches, totalEarnings,
            winRate, avgClearing, capacity, p.getDailyJobCapacity()
        );
    }

    @Transactional
    public com.gigmatch.portfolio.dto.PortfolioDto addPortfolio(Long providerId,
                                                                  String title,
                                                                  String description,
                                                                  String category,
                                                                  MultipartFile file) throws IOException {
        Provider p = providerRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found: " + providerId));

        // For the demo, store a placeholder URL (in production you'd upload to S3 etc.)
        String sampleUrl = "https://placehold.co/600x400?text=" +
                UUID.randomUUID().toString().substring(0, 8);

        Portfolio portfolio = Portfolio.builder()
                .provider(p)
                .title(title)
                .description(description)
                .sampleUrl(sampleUrl)
                .category(category)
                .active(true)
                .build();

        portfolioRepository.save(portfolio);
        return com.gigmatch.portfolio.dto.PortfolioDto.from(portfolio);
    }
}
