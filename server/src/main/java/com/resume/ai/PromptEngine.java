package com.resume.ai;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
        try {
            ClassPathResource resource = new ClassPathResource("ai/templates/" + templateName + ".yaml");
            String raw = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            Object loaded = yaml.load(raw);
            if (loaded instanceof Map<?, ?> map) {
                Object value = map.get(section);
                if (value != null) {
                    return String.valueOf(value).trim();
                }
            }
        } catch (Exception ignored) {
        }
        return section.equals("system") ? "You are a helpful assistant." : "${input}";
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