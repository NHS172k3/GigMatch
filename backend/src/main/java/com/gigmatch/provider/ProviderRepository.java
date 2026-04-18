package com.gigmatch.provider;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByProviderKey(String providerKey);

    List<Provider> findByStatus(String status);

    @Query("""
        SELECT DISTINCT p FROM Provider p
        JOIN p.skillOfferings s
        WHERE p.status = 'ACTIVE'
        AND s.skillCategory IN :categories
        """)
    List<Provider> findActiveBySkillCategories(@Param("categories") List<String> categories);

    @Modifying
    @Transactional
    @Query("UPDATE Provider p SET p.totalActiveJobs = p.totalActiveJobs + 1 WHERE p.id = :id")
    void incrementActiveJobs(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Provider p SET p.totalActiveJobs = GREATEST(0, p.totalActiveJobs - 1) WHERE p.id = :id")
    void decrementActiveJobs(@Param("id") Long id);
}
