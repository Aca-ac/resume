-- 岗位管理模块：目标岗位表和岗位评论表

CREATE TABLE IF NOT EXISTS target_jobs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '岗位ID，主键自增',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID，关联用户表',
    job_name VARCHAR(255) NOT NULL COMMENT '岗位名称',
    jd_content MEDIUMTEXT NULL COMMENT '岗位JD内容（包含公司名称、薪资范围、工作地点、职位描述、任职要求等全部信息）',
    source TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '来源：0-手动添加，1-AI联网搜索，2-fork他人岗位',
    original_job_id BIGINT UNSIGNED NULL COMMENT 'fork来源岗位ID（source=2时必填）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_job_name (job_name),
    KEY idx_created_at (created_at),
    CONSTRAINT fk_target_job_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_target_job_original FOREIGN KEY (original_job_id) REFERENCES target_jobs(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目标岗位表';

CREATE TABLE IF NOT EXISTS job_comments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID，主键自增',
    job_id BIGINT UNSIGNED NOT NULL COMMENT '岗位ID，关联目标岗位表',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID，关联用户表',
    content TEXT NOT NULL COMMENT '评论内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_job_id (job_id),
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at),
    CONSTRAINT fk_comment_job FOREIGN KEY (job_id) REFERENCES target_jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位评论表';