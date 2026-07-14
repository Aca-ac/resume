-- he：简历模板表（tian 渲染侧只读 template_path）
CREATE TABLE IF NOT EXISTS resume_templates (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '模板ID',
    name VARCHAR(64) NOT NULL COMMENT '模板名称',
    category VARCHAR(32) NOT NULL COMMENT '分类：简约/专业/创意/学术',
    preview_url VARCHAR(512) NULL COMMENT '预览图路径',
    template_path VARCHAR(255) NOT NULL COMMENT '模板文件路径（classpath 相对）',
    applicable_scene VARCHAR(128) NULL COMMENT '适用场景',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历模板表';

-- 与 tian 内置 4 套模板及 FALLBACK templateId=1~4 对齐
INSERT INTO resume_templates (name, category, preview_url, template_path, applicable_scene) VALUES
('简约风格', '简约', '/templates/previews/simple.png', '/templates/resumes/simple/template.docx', '应届生、初级岗位'),
('专业风格', '专业', '/templates/previews/professional.png', '/templates/resumes/professional/template.docx', '中级、高级岗位、金融、咨询'),
('创意风格', '创意', '/templates/previews/creative.png', '/templates/resumes/creative/template.docx', '设计、创意类岗位'),
('学术风格', '学术', '/templates/previews/academic.png', '/templates/resumes/academic/template.docx', '学术研究、教育类岗位');
