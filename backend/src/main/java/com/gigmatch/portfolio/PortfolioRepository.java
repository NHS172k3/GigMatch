package com.gigmatch.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByProviderIdAndActiveTrue(Long providerId);

    List<Portfolio> findByProviderIdOrderByCreatedAtDesc(Long providerId);
}
