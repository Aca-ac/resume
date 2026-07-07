package com.resume.ai;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PromptEngineTest {

    private final PromptEngine engine = new PromptEngine();

    @Test
    void substitutesLongerPlaceholderFirst() {
        String template = "foo=${foobar} bar=${foo}";
        String rendered = invokeSubstitute(template, Map.of("foo", "A", "foobar", "B"));
        assertEquals("foo=B bar=A", rendered);
    }

    @Test
    void rendersJobMatchTemplate() {
        String user = engine.renderUser("job-match", Map.of("resume", "R", "jd", "J"));
        assertTrue(user.contains("R"));
        assertTrue(user.contains("J"));
        assertTrue(user.contains("SCORE:"));
    }

    private String invokeSubstitute(String template, Map<String, String> vars) {
        try {
            var method = PromptEngine.class.getDeclaredMethod("substitute", String.class, Map.class);
            method.setAccessible(true);
            return (String) method.invoke(engine, template, vars);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}