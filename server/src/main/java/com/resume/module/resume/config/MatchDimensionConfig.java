package com.resume.module.resume.config;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public final class MatchDimensionConfig {

    public enum Preset {
        TECHNICAL,
        MANAGEMENT,
        GENERAL
    }

    private MatchDimensionConfig() {
    }

    @Getter
    public static final class DimensionDef {
        private final String key;
        private final String name;
        private final BigDecimal weight;
        private final String description;
        private final ScoreSource scoreSource;

        public DimensionDef(String key, String name, double weight, String description, ScoreSource scoreSource) {
            this.key = key;
            this.name = name;
            this.weight = BigDecimal.valueOf(weight);
            this.description = description;
            this.scoreSource = scoreSource;
        }
    }

    public enum ScoreSource {
        SKILL,
        PROJECT,
        EXPERIENCE,
        EDUCATION,
        SUMMARY,
        COMMUNICATION,
        LEARNING,
        COMPREHENSIVE
    }

    public static List<DimensionDef> dimensions(Preset preset) {
        return switch (preset) {
            case TECHNICAL -> List.of(
                    new DimensionDef("skill", "专业技能", 0.25, "技术栈掌握程度", ScoreSource.SKILL),
                    new DimensionDef("project", "项目经验", 0.20, "项目复杂度与贡献", ScoreSource.PROJECT),
                    new DimensionDef("experience", "工作经验", 0.15, "相关工作经验年限", ScoreSource.EXPERIENCE),
                    new DimensionDef("education", "学历背景", 0.10, "教育水平与专业匹配", ScoreSource.EDUCATION),
                    new DimensionDef("communication", "沟通能力", 0.15, "沟通与协作能力", ScoreSource.COMMUNICATION),
                    new DimensionDef("learning", "学习能力", 0.15, "学习新技术的能力", ScoreSource.LEARNING)
            );
            case MANAGEMENT -> List.of(
                    new DimensionDef("leadership", "领导力", 0.25, "团队引领与决策", ScoreSource.EXPERIENCE),
                    new DimensionDef("strategy", "战略思维", 0.20, "目标规划与业务理解", ScoreSource.SUMMARY),
                    new DimensionDef("team", "团队管理", 0.20, "团队建设与协作", ScoreSource.PROJECT),
                    new DimensionDef("communication", "沟通协调", 0.15, "跨部门沟通", ScoreSource.COMMUNICATION),
                    new DimensionDef("industry", "行业经验", 0.10, "行业背景匹配", ScoreSource.EXPERIENCE),
                    new DimensionDef("decision", "决策能力", 0.10, "问题分析与决策", ScoreSource.SKILL)
            );
            case GENERAL -> List.of(
                    new DimensionDef("skill", "专业技能", 0.20, "专业技能掌握程度", ScoreSource.SKILL),
                    new DimensionDef("experience", "工作经验", 0.20, "相关工作经验", ScoreSource.EXPERIENCE),
                    new DimensionDef("education", "学历背景", 0.15, "教育水平与专业匹配", ScoreSource.EDUCATION),
                    new DimensionDef("communication", "沟通能力", 0.15, "沟通与协作能力", ScoreSource.COMMUNICATION),
                    new DimensionDef("project", "项目经验", 0.15, "项目经历与贡献", ScoreSource.PROJECT),
                    new DimensionDef("comprehensive", "综合素质", 0.15, "整体职业素养", ScoreSource.COMPREHENSIVE)
            );
        };
    }

    public static int resolveScore(ScoreSource source, int summary, int education, int experience, int skill, int project) {
        return switch (source) {
            case SKILL -> skill;
            case PROJECT -> project;
            case EXPERIENCE -> experience;
            case EDUCATION -> education;
            case SUMMARY, COMPREHENSIVE -> summary;
            case COMMUNICATION -> average(summary, experience);
            case LEARNING -> average(skill, education);
        };
    }

    private static int average(int a, int b) {
        return (a + b) / 2;
    }
}
