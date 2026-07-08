CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `email` VARCHAR(255) NOT NULL COMMENT '邮箱（登录账号，唯一）',
    `password_hash` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '密码哈希（bcrypt加密）',
    `nickname` VARCHAR(100) NULL DEFAULT NULL COMMENT '昵称',

    -- 基本信息（用于简历自动填充）
    `name` VARCHAR(50) NULL DEFAULT NULL COMMENT '姓名',
    `phone` VARCHAR(20) NULL DEFAULT NULL COMMENT '手机号',
    `birth_date` DATE NULL DEFAULT NULL COMMENT '出生日期',
    `education` VARCHAR(50) NULL DEFAULT NULL COMMENT '最高学历（如：本科、硕士、博士）',
    `work_years` TINYINT UNSIGNED NULL DEFAULT NULL COMMENT '工作年限',
    `city` VARCHAR(50) NULL DEFAULT NULL COMMENT '所在城市',
    
    -- 账号状态
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '账号状态：0-禁用，1-正常',

    -- 时间戳
    `last_login_at` DATETIME NULL DEFAULT NULL COMMENT '最后登录时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';