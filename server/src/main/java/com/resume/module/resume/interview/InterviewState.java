package com.resume.module.resume.interview;

import com.resume.common.BusinessException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sprint3 he：面试对话状态机。
 * PREPARING → QUESTIONING ⇄ EVALUATING → SUMMARIZING → COMPLETED
 * 任意进行中状态可 → ABORTED
 */
public enum InterviewState {
    PREPARING,
    QUESTIONING,
    EVALUATING,
    SUMMARIZING,
    COMPLETED,
    ABORTED;

    private static final Map<InterviewState, Set<InterviewState>> EDGES = Map.of(
            PREPARING, EnumSet.of(QUESTIONING, ABORTED),
            QUESTIONING, EnumSet.of(EVALUATING, SUMMARIZING, ABORTED),
            EVALUATING, EnumSet.of(QUESTIONING, SUMMARIZING, ABORTED),
            SUMMARIZING, EnumSet.of(COMPLETED, ABORTED),
            COMPLETED, EnumSet.noneOf(InterviewState.class),
            ABORTED, EnumSet.noneOf(InterviewState.class)
    );

    public static InterviewState from(String raw) {
        if (raw == null || raw.isBlank()) {
            return QUESTIONING;
        }
        try {
            return InterviewState.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 兼容旧 status
            if ("ONGOING".equalsIgnoreCase(raw) || "DONE".equalsIgnoreCase(raw)) {
                return "DONE".equalsIgnoreCase(raw) ? COMPLETED : QUESTIONING;
            }
            throw new BusinessException(400, "未知面试状态: " + raw);
        }
    }

    public void assertCanTransitTo(InterviewState next) {
        Set<InterviewState> allowed = EDGES.getOrDefault(this, EnumSet.noneOf(InterviewState.class));
        if (!allowed.contains(next)) {
            throw new BusinessException(409, "非法状态迁移: " + this + " → " + next);
        }
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == ABORTED;
    }

    public boolean canAnswer() {
        return this == QUESTIONING;
    }

    /** 兼容旧字段 status */
    public String toLegacyStatus() {
        return isTerminal() ? "DONE" : "ONGOING";
    }
}
