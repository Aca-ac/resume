-- 缝合迁移：兼容「只有 title 无 content」与「已有 content 无 version」等遗留库
ALTER TABLE resumes
    ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL=在线新建 IMPORT=文件导入' AFTER title;

ALTER TABLE resumes
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '编辑版本号：每次保存+1' AFTER source_type;

ALTER TABLE resumes
    MODIFY COLUMN content MEDIUMTEXT NULL COMMENT '简历正文（列表/编辑/导出均读此字段）';

ALTER TABLE resume_details
    MODIFY COLUMN content MEDIUMTEXT NOT NULL COMMENT '分段内容，禁止存原始大文件';

ALTER TABLE resume_files
    ADD COLUMN file_md5 VARCHAR(32) NULL COMMENT 'MD5，秒传去重' AFTER file_size,
    ADD COLUMN parse_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/FAILED/OCR_FALLBACK' AFTER ocr_text,
    ADD KEY idx_user_md5 (user_id, file_md5);
