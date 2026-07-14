ALTER TABLE resumes
    ADD COLUMN photo_path VARCHAR(255) NULL COMMENT '一寸照相对路径（相对 app.storage.upload-dir）' AFTER title;
