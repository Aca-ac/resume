-- 模板 docx 与预览图同目录；每类一套 template.docx + preview.png
UPDATE resume_templates
SET name = '简约风格',
    category = '简约',
    template_path = '/templates/resumes/simple/template.docx',
    preview_url = '/templates/resumes/simple/preview.png',
    applicable_scene = '应届生、初级岗位',
    updated_at = NOW()
WHERE id = 1;

UPDATE resume_templates
SET name = '专业风格',
    category = '专业',
    template_path = '/templates/resumes/professional/template.docx',
    preview_url = '/templates/resumes/professional/preview.png',
    applicable_scene = '中级、高级岗位、金融、咨询',
    updated_at = NOW()
WHERE id = 2;

UPDATE resume_templates
SET name = '创意风格',
    category = '创意',
    template_path = '/templates/resumes/creative/template.docx',
    preview_url = '/templates/resumes/creative/preview.png',
    updated_at = NOW()
WHERE id = 3;

UPDATE resume_templates
SET name = '学术风格',
    category = '学术',
    template_path = '/templates/resumes/academic/template.docx',
    preview_url = '/templates/resumes/academic/preview.png',
    updated_at = NOW()
WHERE id = 4;

DELETE FROM resume_templates WHERE id = 5;
