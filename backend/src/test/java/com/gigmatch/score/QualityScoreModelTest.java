package com.gigmatch.score;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class QualityScoreModelTest {

    private QualityScoreModel model;

    @BeforeEach
    void setUp() {
        model = new QualityScoreModel();
        model.loadCoefficients();
    }

    @Test
    void sigmoid_outputAlwaysInZeroOne() {
        for (int i = 0; i < 100; i++) {
            double cr = Math.random();
            double rn = Math.random();
            double sm = Math.random();
            double result = model.predictSuccess(cr, rn, sm);
            assertThat(result).isBetween(0.0, 1.0);
        }
    }

    @Test
    void perfectProvider_highSuccessRate() {
        double result = model.predictSuccess(1.0, 1.0, 1.0);
        assertThat(result).isGreaterThan(0.7);
    }

    @Test
    void poorProvider_lowSuccessRate() {
        double result = model.predictSuccess(0.5, 0.5, 0.0);
        assertThat(result).isLessThan(0.6);
    }

    @Test
    void higherCompletionRate_higherSuccess() {
        double low  = model.predictSuccess(0.6, 0.8, 0.5);
        double high = model.predictSuccess(0.99, 0.8, 0.5);
        assertThat(high).isGreaterThan(low);
    }

    @Test
    void higherRating_higherSuccess() {
        double low  = model.predictSuccess(0.85, 0.5, 0.5);
        double high = model.predictSuccess(0.85, 1.0, 0.5);
        assertThat(high).isGreaterThan(low);
    }

    @Test
    void higherSkillsMatch_higherSuccess() {
        double low  = model.predictSuccess(0.85, 0.85, 0.0);
        double high = model.predictSuccess(0.85, 0.85, 1.0);
        assertThat(high).isGreaterThan(low);
    }
}
