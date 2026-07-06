package com.resume.ai;

import com.resume.common.BusinessException;
import com.resume.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PromptEngine {

    private final Yaml yaml = new Yaml();

    public String renderSystem(String templateName, Map<String, String> vars) {
        return substitute(extractSection(templateName, "system"), vars);
    }

    public String renderUser(String templateName, Map<String, String> vars) {
        return substitute(extractSection(templateName, "user"), vars);
    }

    private String extractSection(String templateName, String section) {
        ClassPathResource resource = new ClassPathResource("ai/templates/" + templateName + ".yaml");
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "prompt template not found: " + templateName);
        }
        try {
            String raw = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            Object loaded = yaml.load(raw);
            if (loaded instanceof Map<?, ?> map) {
                Object value = map.get(section);
                if (value != null) {
                    return String.valueOf(value).trim();
                }
            }
            log.warn("Prompt section '{}' missing in template '{}'", section, templateName);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "prompt section missing: " + templateName + "/" + section);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to parse prompt template {} section {}", templateName, section, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "prompt template parse failed: " + templateName);
        }
    }

    private String substitute(String template, Map<String, String> vars) {
        if (vars == null || vars.isEmpty()) {
            return template;
        }
        List<Map.Entry<String, String>> entries = new ArrayList<>(vars.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());
        String rendered = template;
        for (Map.Entry<String, String> entry : entries) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace(placeholder, value);
        }
        return rendered;
    }
}