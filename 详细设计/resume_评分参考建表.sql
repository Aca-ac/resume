-- ============================================================
-- 简历导入导出模块 · 数据库表结构（只读参考）
-- 用于简历多维度权重评分算法设计
-- 不修改任何现有代码
-- ============================================================

-- -----------------------------------------------------------
-- 1. resumes —— 简历主表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS resumes (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '简历ID',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    title       VARCHAR(255)    NOT NULL DEFAULT '未命名简历' COMMENT '简历标题',
    version     INT UNSIGNED    NOT NULL DEFAULT 1     COMMENT '版本号',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历主表';

-- -----------------------------------------------------------
-- 2. resume_details —— 简历明细表
-- 评分算法需处理字段：section_type(维度区分)、content(JSON数据)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS resume_details (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    resume_id     BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
    section_type  VARCHAR(32)     NOT NULL COMMENT '分段类型：EDUCATION/WORK_EXPERIENCE/PROJECT/SKILL/SUMMARY',
    section_name  VARCHAR(64)     NOT NULL COMMENT '分段标题',
    content       TEXT            NOT NULL COMMENT '分段内容（JSON格式）',
    sort_order    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序序号',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_resume_id (resume_id),
    CONSTRAINT fk_details_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历明细表';

-- -----------------------------------------------------------
-- 3. resume_files —— 简历文件表
-- 评分算法需处理字段：ocr_text(提取文本)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS resume_files (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    resume_id      BIGINT UNSIGNED NULL COMMENT '关联简历ID',
    file_type      VARCHAR(32)     NOT NULL COMMENT '文件类型：JPG/PNG/PDF/DOCX',
    file_path      VARCHAR(512)    NOT NULL COMMENT '文件存储路径',
    file_size      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    original_name  VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    ocr_text       TEXT            NULL COMMENT 'OCR/文本提取结果',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_resume_id (resume_id),
    CONSTRAINT fk_files_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_files_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历文件表';


-- ============================================================
-- content JSON 结构示例（评分算法参考）
-- ============================================================

-- EDUCATION:
-- {"school":"武汉大学","major":"计算机科学与技术","degree":"本科","startDate":"2018-09","endDate":"2022-06","gpa":"3.8/4.0","description":"..."}

-- WORK_EXPERIENCE:
-- {"company":"字节跳动","position":"后端开发","startDate":"2022-07","endDate":"2024-03","description":"..."}

-- PROJECT:
-- {"name":"智通车","role":"后端负责人","technologies":"Spring Boot,MySQL,Redis","description":"..."}

-- SKILL:
-- {"category":"后端","skills":["Java","Spring Boot","MySQL","Redis","Docker"]}

-- SUMMARY:
-- {"content":"3年后端开发经验，熟悉微服务架构..."}


-- ============================================================
-- 评分维度建议（5维 + 默认权重）
-- ============================================================
/*
education  0.25  学校层次、学历、GPA、专业对口
experience 0.35  公司层次、岗位匹配度、工作年限
skill      0.20  技能数量、与岗位匹配度
project    0.15  项目复杂度、技术栈匹配度
summary    0.05  表达能力、关键词匹配

总分 = Σ(维度分 × 权重)  范围: 0-100
*/
