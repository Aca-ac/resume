-- Sprint3 he：面试对话状态机字段扩展（在 V7 基础上演进）

ALTER TABLE interview_sessions
    ADD COLUMN job_id BIGINT UNSIGNED NULL COMMENT '目标岗位ID（可选）' AFTER resume_id,
    ADD COLUMN state VARCHAR(32) NOT NULL DEFAULT 'QUESTIONING' COMMENT '状态机：PREPARING/QUESTIONING/EVALUATING/SUMMARIZING/COMPLETED/ABORTED' AFTER status,
    ADD COLUMN question_index INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前已提出的问题序号（从1开始）' AFTER state,
    ADD COLUMN max_questions INT UNSIGNED NOT NULL DEFAULT 5 COMMENT '本场最大问题数' AFTER question_index,
    ADD COLUMN last_seq INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'WebSocket 消息序号（断线恢复用）' AFTER max_questions,
    ADD COLUMN jd_snapshot MEDIUMTEXT NULL COMMENT '开始面试时的 JD 快照' AFTER last_seq,
    ADD COLUMN ended_at DATETIME NULL COMMENT '结束时间' AFTER report;

ALTER TABLE interview_sessions
    ADD KEY idx_job_id (job_id),
    ADD KEY idx_state (state),
    ADD KEY idx_user_created (user_id, created_at);

-- 兼容：已有 ONGOING 会话标记为 QUESTIONING；DONE 标记为 COMPLETED
UPDATE interview_sessions SET state = 'QUESTIONING' WHERE status = 'ONGOING' AND (state IS NULL OR state = 'QUESTIONING');
UPDATE interview_sessions SET state = 'COMPLETED' WHERE status = 'DONE';

ALTER TABLE interview_messages
    ADD COLUMN message_type VARCHAR(32) NOT NULL DEFAULT 'CHAT' COMMENT 'QUESTION/ANSWER/FEEDBACK/SYSTEM/CHAT' AFTER role,
    ADD COLUMN question_index INT UNSIGNED NULL COMMENT '对应第几题' AFTER message_type,
    ADD COLUMN evaluation MEDIUMTEXT NULL COMMENT '对回答的评估反馈（JSON或文本）' AFTER content,
    ADD COLUMN seq INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '会话内单调递增序号，用于 WS 恢复' AFTER evaluation,
    ADD COLUMN client_msg_id VARCHAR(64) NULL COMMENT '客户端幂等ID' AFTER seq;

ALTER TABLE interview_messages
    ADD KEY idx_session_seq (session_id, seq),
    ADD KEY idx_session_client_msg (session_id, client_msg_id);
