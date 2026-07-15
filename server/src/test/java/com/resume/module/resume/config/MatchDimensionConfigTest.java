package com.resume.module.resume.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchDimensionConfigTest {

    @Test
    void technicalPreset_weightsSumToOne() {
        List<MatchDimensionConfig.DimensionDef> dims = MatchDimensionConfig.dimensions(
                MatchDimensionConfig.Preset.TECHNICAL);
        BigDecimal sum = dims.stream()
                .map(MatchDimensionConfig.DimensionDef::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(BigDecimal.ONE));
        assertEquals(6, dims.size());
    }

    @Test
    void resolveScore_mapsSourcesAndAverages() {
        assertEquals(80, MatchDimensionConfig.resolveScore(
                MatchDimensionConfig.ScoreSource.SKILL, 10, 20, 30, 80, 90));
        assertEquals(90, MatchDimensionConfig.resolveScore(
                MatchDimensionConfig.ScoreSource.PROJECT, 10, 20, 30, 80, 90));
        assertEquals(10, MatchDimensionConfig.resolveScore(
                MatchDimensionConfig.ScoreSource.SUMMARY, 10, 20, 30, 80, 90));
        assertEquals(20, MatchDimensionConfig.resolveScore(
                MatchDimensionConfig.ScoreSource.COMMUNICATION, 10, 20, 30, 80, 90));
        assertEquals(50, MatchDimensionConfig.resolveScore(
                MatchDimensionConfig.ScoreSource.LEARNING, 10, 20, 30, 80, 90));
    }

    @Test
    void weightedRadarScore_example() {
        List<MatchDimensionConfig.DimensionDef> dims = MatchDimensionConfig.dimensions(
                MatchDimensionConfig.Preset.GENERAL);
        int summary = 70, education = 80, experience = 60, skill = 90, project = 50;
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (MatchDimensionConfig.DimensionDef def : dims) {
            int score = MatchDimensionConfig.resolveScore(
                    def.getScoreSource(), summary, education, experience, skill, project);
            weighted = weighted.add(BigDecimal.valueOf(score).multiply(def.getWeight()));
            total = total.add(def.getWeight());
        }
        int radar = weighted.divide(total, 0, RoundingMode.HALF_UP).intValue();
        assertTrue(radar >= 0 && radar <= 100);
        assertEquals(70, radar);
    }
}
