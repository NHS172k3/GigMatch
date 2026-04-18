package com.gigmatch.match;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "job_category")
    private String jobCategory;

    /** Comma-separated list of required skills */
    @Column(name = "required_skills", length = 1000)
    private String requiredSkills;

    @Column(name = "budget_cents")
    private Integer budgetCents;

    @Column(name = "urgency_level")
    private String urgencyLevel;

    @Column(name = "winner_provider_key")
    private String winnerProviderKey;

    @Column(name = "winner_provider_id")
    private Long winnerProviderId;

    @Column(name = "clearing_price_cents")
    private Integer clearingPriceCents;

    @Column(name = "predicted_success_rate")
    private Double predictedSuccessRate;

    @Column(name = "effective_score")
    private Double effectiveScore;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    /** MATCHED | NO_MATCH | TIMEOUT */
    @Column(nullable = false)
    private String outcome;

    /** Comma-separated list of provider keys that participated */
    @Column(name = "participating_providers", length = 500)
    private String participatingProviders;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchLog m)) return false;
        return id != null && id.equals(m.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
