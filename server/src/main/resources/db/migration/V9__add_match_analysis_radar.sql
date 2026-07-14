-- 雷达图：匹配分析主表 + 维度表 + 子维度表

CREATE TABLE IF NOT EXISTS match_analysis (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分析记录ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    resume_id BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
    job_description_id BIGINT UNSIGNED NULL COMMENT '职位描述ID（预留）',
    jd_text MEDIUMTEXT NULL COMMENT '职位描述原文',
    match_score INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总分 0-100',
    match_level VARCHAR(20) NOT NULL DEFAULT 'average' COMMENT 'excellent/good/average/poor',
    analysis TEXT NOT NULL COMMENT '详细分析',
    summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '简短总结',
    highlights JSON NULL COMMENT '亮点数组',
    weaknesses JSON NULL COMMENT '待提升点数组',
    suggestions JSON NULL COMMENT '建议数组',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_resume_id (resume_id),
    KEY idx_created_at (created_at),
    KEY idx_match_score (match_score),
    CONSTRAINT fk_match_analysis_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_analysis_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匹配分析主表（雷达图）';

CREATE TABLE IF NOT EXISTS match_dimension (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '维度ID',
    analysis_id BIGINT UNSIGNED NOT NULL COMMENT '分析记录ID',
    dimension_key VARCHAR(50) NOT NULL COMMENT '维度标识',
    dimension_name VARCHAR(100) NOT NULL COMMENT '维度名称',
    score INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0-100',
    weight DECIMAL(5, 2) NOT NULL DEFAULT 1.00 COMMENT '权重',
    description VARCHAR(500) NULL COMMENT '维度说明',
    details TEXT NULL COMMENT '详细说明',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_analysis_id (analysis_id),
    CONSTRAINT fk_match_dimension_analysis FOREIGN KEY (analysis_id) REFERENCES match_analysis(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匹配分析维度评分';

CREATE TABLE IF NOT EXISTS match_sub_dimension (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '子维度ID',
    dimension_id BIGINT UNSIGNED NOT NULL COMMENT '维度ID',
    name VARCHAR(100) NOT NULL COMMENT '子维度名称',
    score INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0-100',
    description VARCHAR(500) NULL COMMENT '说明',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (id),
    KEY idx_dimension_id (dimension_id),
    CONSTRAINT fk_match_sub_dimension_dimension FOREIGN KEY (dimension_id) REFERENCES match_dimension(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匹配分析子维度';

SET @col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'match_records' AND COLUMN_NAME = 'analysis_id');
SET @sql := IF(@col = 0,
    'ALTER TABLE match_records ADD COLUMN analysis_id BIGINT UNSIGNED NULL COMMENT ''关联 match_analysis.id'' AFTER project_score, ADD KEY idx_analysis_id (analysis_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
