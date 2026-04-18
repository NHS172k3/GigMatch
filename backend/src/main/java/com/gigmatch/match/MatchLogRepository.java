package com.gigmatch.match;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchLogRepository extends JpaRepository<MatchLog, Long> {

    Page<MatchLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<MatchLog> findByWinnerProviderIdOrderByCreatedAtDesc(Long providerId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM MatchLog m WHERE m.winnerProviderId = :id AND m.outcome = 'MATCHED'")
    long countWinsByProviderId(@Param("id") Long id);

    @Query("SELECT COUNT(m) FROM MatchLog m WHERE m.winnerProviderId = :id")
    long countTotalByProviderId(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(m.clearingPriceCents), 0) FROM MatchLog m WHERE m.winnerProviderId = :id AND m.outcome = 'MATCHED'")
    long sumEarningsByProviderId(@Param("id") Long id);

    @Query("SELECT COUNT(m) FROM MatchLog m WHERE m.outcome = 'MATCHED'")
    long countTotalMatches();

    @Query("SELECT COALESCE(SUM(m.clearingPriceCents), 0) FROM MatchLog m WHERE m.outcome = 'MATCHED'")
    long sumTotalEarnings();
}
