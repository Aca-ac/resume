ALTER TABLE match_records
    ADD COLUMN job_id BIGINT UNSIGNED NULL COMMENT '岗位ID（关联target_jobs表）' AFTER resume_id;

ALTER TABLE match_records
    ADD KEY idx_job_id (job_id);

ALTER TABLE match_records
    ADD CONSTRAINT fk_match_job FOREIGN KEY (job_id) REFERENCES target_jobs(id) ON DELETE SET NULL;