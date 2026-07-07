package com.resume.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreParserTest {

    @Test
    void parsesScoreLine() {
        assertEquals(85, ScoreParser.parse("SCORE: 85\nGood match"));
    }

    @Test
    void ignoresUnrelatedNumbers() {
        assertNull(ScoreParser.parse("Year 2024 score about 50 percent"));
    }

    @Test
    void rejectsOutOfRange() {
        assertNull(ScoreParser.parse("SCORE: 150"));
    }

    @Test
    void nullInputReturnsNull() {
        assertNull(ScoreParser.parse(null));
    }
}