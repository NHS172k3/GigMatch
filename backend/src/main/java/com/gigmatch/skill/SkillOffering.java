package com.gigmatch.skill;

import com.gigmatch.provider.Provider;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skill_offerings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Column(name = "skill_category", nullable = false)
    private String skillCategory;

    @Column(name = "min_quote_cents", nullable = false)
    private int minQuoteCents;

    @Column(name = "max_quote_cents", nullable = false)
    private int maxQuoteCents;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkillOffering s)) return false;
        return id != null && id.equals(s.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
