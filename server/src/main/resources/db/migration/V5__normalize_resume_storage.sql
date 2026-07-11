-- 统一存储模型：resumes 只存元数据，正文在 resume_details，原文件在 resume_files

-- source_type（若 V4 未成功执行）
SET @col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resumes' AND COLUMN_NAME = 'source_type');
SET @sql := IF(@col = 0,
    'ALTER TABLE resumes ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT ''MANUAL'' COMMENT ''MANUAL=新建 IMPORT=导入'' AFTER title',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- resume_files 扩展字段
SET @col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resume_files' AND COLUMN_NAME = 'file_md5');
SET @sql := IF(@col = 0,
    'ALTER TABLE resume_files ADD COLUMN file_md5 VARCHAR(32) NULL COMMENT ''MD5秒传'' AFTER file_size',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resume_files' AND COLUMN_NAME = 'parse_status');
SET @sql := IF(@col = 0,
    'ALTER TABLE resume_files ADD COLUMN parse_status VARCHAR(16) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING/DONE/FAILED/OCR_FALLBACK'' AFTER ocr_text',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 大文本字段
ALTER TABLE resume_details
    MODIFY COLUMN content MEDIUMTEXT NOT NULL COMMENT '分段正文（SUMMARY=可编辑全文）';

ALTER TABLE resume_files
    MODIFY COLUMN ocr_text MEDIUMTEXT NULL COMMENT '解析/OCR 文本';

-- 将 resumes.content 迁移到 resume_details.SUMMARY 后删除 content 列
SET @has_content := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resumes' AND COLUMN_NAME = 'content');

SET @sql := IF(@has_content > 0,
    'INSERT INTO resume_details (resume_id, section_type, section_name, content, sort_order, created_at, updated_at)
     SELECT r.id, ''SUMMARY'', ''正文'', r.content, 0, NOW(), NOW()
     FROM resumes r
     WHERE r.content IS NOT NULL AND TRIM(r.content) <> ''''
       AND NOT EXISTS (
           SELECT 1 FROM resume_details d
           WHERE d.resume_id = r.id AND d.section_type = ''SUMMARY''
       )',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_content > 0,
    'ALTER TABLE resumes DROP COLUMN content',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 重复 version 列（V4 误加）清理：若存在两个 version 则跳过；仅确保 version 有默认值
ALTER TABLE resumes
    MODIFY COLUMN version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '编辑版本号，每次保存+1';
