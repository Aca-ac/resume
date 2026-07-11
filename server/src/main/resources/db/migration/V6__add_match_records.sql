-- 职位匹配分析记录表
CREATE TABLE IF NOT EXISTS match_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '匹配记录ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    resume_id BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
    jd_text MEDIUMTEXT NOT NULL COMMENT '职位描述原文',
    match_score INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '匹配分 0-100',
    analysis MEDIUMTEXT NULL COMMENT '匹配分析说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_resume_id (resume_id),
    KEY idx_created_at (created_at),
    CONSTRAINT fk_match_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='JD职位匹配记录';
