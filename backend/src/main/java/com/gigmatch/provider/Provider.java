package com.gigmatch.provider;

import com.gigmatch.portfolio.Portfolio;
import com.gigmatch.skill.SkillOffering;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "providers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "provider_key", nullable = false, unique = true)
    private String providerKey;

    @Column(name = "avg_rating", nullable = false)
    private double avgRating;

    @Column(name = "completion_rate", nullable = false)
    private double completionRate;

    @Column(name = "daily_job_capacity", nullable = false)
    private int dailyJobCapacity;

    @Column(name = "total_active_jobs", nullable = false)
    private int totalActiveJobs;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "provider", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SkillOffering> skillOfferings = new ArrayList<>();

    @OneToMany(mappedBy = "provider", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Portfolio> portfolios = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Provider p)) return false;
        return id != null && id.equals(p.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
