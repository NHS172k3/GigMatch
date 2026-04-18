package com.gigmatch.skill;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Computes the skills match score for a provider against a job request's required skills.
 * Score = matching skill categories / total required skills (clamped to [0, 1]).
 */
@Service
@RequiredArgsConstructor
public class SkillMatchService {

    private final SkillOfferingRepository skillOfferingRepository;

    /**
     * @param requiredSkills  job's required skill categories, e.g. ["software-dev", "api"]
     * @param providerId      the provider to evaluate
     * @return match score in [0.0, 1.0]
     */
    public double computeSkillsMatch(List<String> requiredSkills, long providerId) {
        if (requiredSkills == null || requiredSkills.isEmpty()) return 1.0;
        long matching = skillOfferingRepository.countMatchingCategories(providerId, requiredSkills);
        return Math.min(1.0, (double) matching / requiredSkills.size());
    }
}
