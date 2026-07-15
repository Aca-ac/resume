CREATE TABLE IF NOT EXISTS job_semantic_vector (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    job_id BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
    vector MEDIUMTEXT NOT NULL COMMENT '语义向量JSON数组',
    modified_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '修改标记：0-未修改（可用缓存），1-已修改（需重新生成）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_id (job_id),
    KEY idx_modified_flag (modified_flag),
    CONSTRAINT fk_job_vector_job FOREIGN KEY (job_id) REFERENCES target_jobs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位语义向量表';

CREATE TABLE IF NOT EXISTS resume_semantic_vector (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    resume_id BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
    vector MEDIUMTEXT NOT NULL COMMENT '语义向量JSON数组',
    modified_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '修改标记：0-未修改（可用缓存），1-已修改（需重新生成）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_id (resume_id),
    KEY idx_modified_flag (modified_flag),
    CONSTRAINT fk_resume_vector_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历语义向量表';