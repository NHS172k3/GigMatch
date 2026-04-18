package com.gigmatch.skill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkillOfferingRepository extends JpaRepository<SkillOffering, Long> {

    List<SkillOffering> findByProviderId(Long providerId);

    @Query("SELECT DISTINCT s.skillCategory FROM SkillOffering s WHERE s.provider.id = :providerId")
    List<String> findCategoriesByProviderId(@Param("providerId") Long providerId);

    @Query("SELECT COUNT(s) FROM SkillOffering s WHERE s.provider.id = :providerId AND s.skillCategory IN :categories")
    long countMatchingCategories(@Param("providerId") Long providerId, @Param("categories") List<String> categories);
}
