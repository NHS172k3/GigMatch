package com.gigmatch.score;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Logistic regression model that predicts P(job_success) for a given provider bid.
 *
 * Model features:
 *   - completionRate:   provider's historical job completion rate (0-1)
 *   - avgRatingNorm:    provider's average rating normalised to [0,1] = rating / 5.0
 *   - skillsMatchScore: fraction of required skills covered by provider (0-1)
 *
 * Coefficients loaded at startup from classpath:quality_score_coefficients.json.
 * Run ml/train_model.py to regenerate coefficients from synthetic data.
 *
 * Effective score formula (used by {@link com.gigmatch.engine.MatchingAuction}):
 *   competitiveness = clamp((budgetCents - quoteCents) / budgetCents, 0, 1)
 *   effectiveScore  = predictSuccess(...) × competitiveness
 */
@Component
@Slf4j
public class QualityScoreModel {

    private double coefCompletionRate;
    private double coefAvgRatingNorm;
    private double coefSkillsMatch;
    private double intercept;

    @PostConstruct
    void loadCoefficients() {
        try (InputStream is = new ClassPathResource("quality_score_coefficients.json").getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            JsonNode coefs = root.get("coefficients");
            coefCompletionRate = coefs.get("completion_rate").asDouble();
            coefAvgRatingNorm  = coefs.get("avg_rating_norm").asDouble();
            coefSkillsMatch    = coefs.get("skills_match_score").asDouble();
            intercept          = root.get("intercept").asDouble();
            log.info("QualityScoreModel loaded: completion_rate={}, avg_rating_norm={}, skills_match={}, intercept={}",
                    coefCompletionRate, coefAvgRatingNorm, coefSkillsMatch, intercept);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load quality_score_coefficients.json", e);
        }
    }

    /**
     * Returns predicted probability of job success in (0, 1).
     *
     * @param completionRate   provider's historical completion rate (0-1)
     * @param avgRatingNorm    provider's average rating / 5.0
     * @param skillsMatchScore fraction of required skills this provider covers (0-1)
     */
    public double predictSuccess(double completionRate, double avgRatingNorm, double skillsMatchScore) {
        double logit = intercept
                + coefCompletionRate * completionRate
                + coefAvgRatingNorm  * avgRatingNorm
                + coefSkillsMatch    * skillsMatchScore;
        return sigmoid(logit);
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
}
