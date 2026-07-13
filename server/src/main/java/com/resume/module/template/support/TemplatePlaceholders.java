package com.resume.module.template.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * docx 模板占位符规范（poi-tl 使用 {{key}} 语法，dataMap 的 key 不含花括号）。
 *
 * <pre>
 * 个人信息：name phone email school major city
 * 分段正文：education experience project skills summary
 * </pre>
 *
 * 后端：从 resume_details.section_type 聚合填充；前端可传 JSON 覆盖同名字段。
 * 模板维护：Word 中直接写 {{name}} 等标签，勿改 key。
 */
public final class TemplatePlaceholders {

    public static final String NAME = "name";
    public static final String PHONE = "phone";
    public static final String EMAIL = "email";
    public static final String SCHOOL = "school";
    public static final String MAJOR = "major";
    public static final String CITY = "city";
    public static final String EDUCATION = "education";
    public static final String EXPERIENCE = "experience";
    public static final String PROJECT = "project";
    public static final String SKILLS = "skills";
    public static final String SUMMARY = "summary";

    public static final List<String> ALL = List.of(
            NAME, PHONE, EMAIL, SCHOOL, MAJOR, CITY,
            EDUCATION, EXPERIENCE, PROJECT, SKILLS, SUMMARY
    );

    private TemplatePlaceholders() {
    }

    public static Map<String, Object> emptyDataMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : ALL) {
            map.put(key, "");
        }
        return map;
    }

    /** section_type → placeholder key */
    public static String mapSectionType(String sectionType) {
        if (sectionType == null) {
            return SUMMARY;
        }
        return switch (sectionType.trim().toUpperCase()) {
            case "EDUCATION", "EDU" -> EDUCATION;
            case "WORK_EXPERIENCE", "WORK", "EXPERIENCE" -> EXPERIENCE;
            case "PROJECT", "PROJECTS" -> PROJECT;
            case "SKILL", "SKILLS" -> SKILLS;
            case "SUMMARY", "BASIC", "PROFILE", "SELF" -> SUMMARY;
            default -> SUMMARY;
        };
    }
}
