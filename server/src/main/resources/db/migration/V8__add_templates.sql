-- 简历模板与临时导出文件

CREATE TABLE IF NOT EXISTS templates (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    name VARCHAR(128) NOT NULL COMMENT '模板名称',
    category VARCHAR(64) NOT NULL DEFAULT '通用' COMMENT '分类：技术岗/设计岗/应届生/商务等',
    preview_url VARCHAR(512) NULL COMMENT '预览图相对路径',
    template_path VARCHAR(512) NOT NULL COMMENT 'docx模板相对路径',
    applicable_scene VARCHAR(255) NULL COMMENT '适用场景描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_category (category),
    KEY idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历模板表';

CREATE TABLE IF NOT EXISTS export_files (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '导出文件ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    resume_id BIGINT UNSIGNED NULL COMMENT '简历ID',
    template_id BIGINT UNSIGNED NULL COMMENT '模板ID',
    file_type VARCHAR(16) NOT NULL COMMENT 'WORD/PDF',
    file_path VARCHAR(512) NOT NULL COMMENT '存储相对路径',
    original_name VARCHAR(255) NOT NULL COMMENT '下载文件名',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_expire_at (expire_at),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='临时导出文件表';
