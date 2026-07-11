-- 模拟面试会话与消息

CREATE TABLE IF NOT EXISTS interview_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    resume_id BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
    job_title VARCHAR(255) NOT NULL COMMENT '目标职位',
    status VARCHAR(16) NOT NULL DEFAULT 'ONGOING' COMMENT 'ONGOING/DONE',
    report MEDIUMTEXT NULL COMMENT '面试报告',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_resume_id (resume_id),
    CONSTRAINT fk_interview_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_interview_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟面试会话';

CREATE TABLE IF NOT EXISTS interview_messages (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    session_id BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
    role VARCHAR(16) NOT NULL COMMENT 'user/assistant',
    content MEDIUMTEXT NOT NULL COMMENT '消息内容',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '顺序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    CONSTRAINT fk_interview_msg_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟面试消息';
