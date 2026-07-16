package com.resume.module.resume.interview;

import com.resume.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterviewStateTest {

    @Test
    void happyPath_transitions() {
        InterviewState.PREPARING.assertCanTransitTo(InterviewState.QUESTIONING);
        InterviewState.QUESTIONING.assertCanTransitTo(InterviewState.EVALUATING);
        InterviewState.EVALUATING.assertCanTransitTo(InterviewState.QUESTIONING);
        InterviewState.EVALUATING.assertCanTransitTo(InterviewState.SUMMARIZING);
        InterviewState.SUMMARIZING.assertCanTransitTo(InterviewState.COMPLETED);
    }

    @Test
    void illegalTransition_throws409() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> InterviewState.COMPLETED.assertCanTransitTo(InterviewState.QUESTIONING));
        assertEquals(409, ex.getCode());
    }

    @Test
    void abortAllowedFromActiveStates() {
        InterviewState.QUESTIONING.assertCanTransitTo(InterviewState.ABORTED);
        InterviewState.EVALUATING.assertCanTransitTo(InterviewState.ABORTED);
        assertTrue(InterviewState.ABORTED.isTerminal());
        assertFalse(InterviewState.QUESTIONING.isTerminal());
        assertTrue(InterviewState.QUESTIONING.canAnswer());
        assertFalse(InterviewState.EVALUATING.canAnswer());
    }

    @Test
    void legacyStatusMapping() {
        assertEquals("ONGOING", InterviewState.QUESTIONING.toLegacyStatus());
        assertEquals("DONE", InterviewState.COMPLETED.toLegacyStatus());
        assertEquals(InterviewState.COMPLETED, InterviewState.from("DONE"));
        assertEquals(InterviewState.QUESTIONING, InterviewState.from("ONGOING"));
    }
}
