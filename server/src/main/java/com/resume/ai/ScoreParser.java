package com.resume.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScoreParser {

    private static final Pattern SCORE_LINE = Pattern.compile("(?im)^SCORE:\\s*(\\d{1,3})\\s*$");

    private ScoreParser() {}

    public static Integer parse(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = SCORE_LINE.matcher(raw);
        if (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            if (value >= 0 && value <= 100) {
                return value;
            }
        }
        return null;
    }
}