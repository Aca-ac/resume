-- 兼容旧库增量升级（新库由 V1 + V5 覆盖；此处仅做幂等补丁）
SET @col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resumes' AND COLUMN_NAME = 'source_type');
SET @sql := IF(@col = 0,
    'ALTER TABLE resumes ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT ''MANUAL'' COMMENT ''来源'' AFTER title',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resume_files' AND COLUMN_NAME = 'file_md5');
SET @sql := IF(@col = 0,
    'ALTER TABLE resume_files ADD COLUMN file_md5 VARCHAR(32) NULL AFTER file_size, ADD KEY idx_user_md5 (user_id, file_md5)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
