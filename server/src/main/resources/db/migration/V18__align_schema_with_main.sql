-- 修复本地库曾用 V12~V15 跑旧模板迁移、未执行 main 上岗位/匹配字段迁移的问题。
-- 幂等：列/索引/外键已存在则跳过。

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'target_jobs' AND COLUMN_NAME = 'source_urls') = 0,
    'ALTER TABLE target_jobs ADD COLUMN source_urls TEXT NULL COMMENT ''来源链接（JSON数组格式，存储AI搜索时获取的来源URL列表）'' AFTER original_job_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'match_records' AND COLUMN_NAME = 'job_id') = 0,
    'ALTER TABLE match_records ADD COLUMN job_id BIGINT UNSIGNED NULL COMMENT ''岗位ID（关联target_jobs表）'' AFTER resume_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'match_records' AND INDEX_NAME = 'idx_job_id') = 0,
    'ALTER TABLE match_records ADD KEY idx_job_id (job_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'match_records' AND CONSTRAINT_NAME = 'fk_match_job') = 0,
    'ALTER TABLE match_records ADD CONSTRAINT fk_match_job FOREIGN KEY (job_id) REFERENCES target_jobs(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resumes' AND COLUMN_NAME = 'photo_path') = 0,
    'ALTER TABLE resumes ADD COLUMN photo_path VARCHAR(255) NULL COMMENT ''一寸照相对路径（相对 app.storage.upload-dir）'' AFTER title',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
