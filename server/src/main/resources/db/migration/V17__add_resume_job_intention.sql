ALTER TABLE resumes
    ADD COLUMN job_intention VARCHAR(256) NULL COMMENT '求职意向（模板占位符 {{jobIntention}}）' AFTER photo_path;
