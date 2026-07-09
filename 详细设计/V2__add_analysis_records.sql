-- ============================================================
-- 简历分析记录表（analysis_records）
--
-- 设计说明：
-- 1. 基于多维度简历评分设计.md，5个维度分别评分后加权计算总分
-- 2. 各维度评分范围0-100（整数），权重参考评分设计文档
-- 3. 每个维度独立字段存储，便于SQL查询统计和前端按维度展示
-- 4. 权重也存入数据库（weight_xxx字段），便于后续调整权重而不需要改代码
-- 5. 通过resume_id关联原简历，不重复存储简历内容
-- 6. status字段支持异步分析流程
--
-- 评分维度及权重（来自多维度简历评分设计.md）：
--   summary（个人总结）：权重10%  - 求职定位明确性、个人优势、独特性
--   education（教育背景）：权重15% - 学历层次、学校层次、GPA、专业对口
--   experience（工作经历）：权重25% - 公司层次、岗位匹配度、工作年限
--   skill（技能）：权重25% - 技能数量、与岗位匹配度、技术深度
--   project（项目经验）：权重25% - 项目参与度、含金量、成果可量化性
--
-- 总分计算公式：总分 = summary×0.10 + education×0.15 + experience×0.25 + skill×0.25 + project×0.25
-- ============================================================

CREATE TABLE IF NOT EXISTS `analysis_records` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分析记录ID',

    -- 用户与简历关联
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID（谁发起的分析）',
    `resume_id` BIGINT UNSIGNED NULL COMMENT '被分析的简历ID（关联resumes表）',

    -- 各维度评分（0-100整数，由AI模型评分得出）
    -- 参考多维度简历评分设计.md中的评分区间：
    --   90-100：优秀，80-89：良好，60-79：合格，30-59：不合格，0-29：严重不合格
    `summary_score` INT UNSIGNED NULL COMMENT '个人总结评分（0-100），权重10%',
    `education_score` INT UNSIGNED NULL COMMENT '教育背景评分（0-100），权重15%',
    `experience_score` INT UNSIGNED NULL COMMENT '工作经历评分（0-100），权重25%',
    `skill_score` INT UNSIGNED NULL COMMENT '技能评分（0-100），权重25%',
    `project_score` INT UNSIGNED NULL COMMENT '项目经验评分（0-100），权重25%',

    -- 综合加权总分（由后端计算得出，范围0-100）
    `total_score` INT UNSIGNED NULL COMMENT '综合加权总分 = sum(各维度分 x 对应权重)',

    -- 各维度权重（与多维度简历评分设计.md保持一致）
    -- 权重存数据库便于前端展示评分计算方式，也方便后续调整
    `weight_summary` DECIMAL(3,2) NOT NULL DEFAULT 0.10 COMMENT '个人总结权重（默认10%）',
    `weight_education` DECIMAL(3,2) NOT NULL DEFAULT 0.15 COMMENT '教育背景权重（默认15%）',
    `weight_experience` DECIMAL(3,2) NOT NULL DEFAULT 0.25 COMMENT '工作经历权重（默认25%）',
    `weight_skill` DECIMAL(3,2) NOT NULL DEFAULT 0.25 COMMENT '技能权重（默认25%）',
    `weight_project` DECIMAL(3,2) NOT NULL DEFAULT 0.25 COMMENT '项目经验权重（默认25%）',

    -- AI分析结果
    `suggestions` TEXT NULL COMMENT 'AI给出的优化建议（中文文本，分段说明每个维度的优缺点和改进方向）',

    -- 状态与追踪
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '分析状态：0-进行中，1-完成，2-失败',
    `error_message` VARCHAR(500) NULL COMMENT '失败原因（仅在status=2时有值）',
    `request_id` VARCHAR(64) NULL COMMENT '请求追踪ID（用于日志排查和链路追踪）',

    -- 时间戳
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（分析发起时间）',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) COMMENT '按用户查询分析历史',
    KEY `idx_resume_id` (`resume_id`) COMMENT '按简历查询所有分析记录',
    KEY `idx_status` (`status`) COMMENT '按状态筛选',
    KEY `idx_created_at` (`created_at`) COMMENT '按时间排序',

    -- 外键约束
    CONSTRAINT `fk_analysis_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE ,
    CONSTRAINT `fk_analysis_resume` FOREIGN KEY (`resume_id`) REFERENCES `resumes`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历分析记录表：每次AI分析对应一条记录，存储5维度评分、权重、总分和建议';