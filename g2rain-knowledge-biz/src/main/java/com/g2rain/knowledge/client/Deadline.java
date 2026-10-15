package com.g2rain.knowledge.client;

import com.g2rain.knowledge.enums.KnowledgeErrorCode;

import java.time.Duration;

/**
 * End-to-end wall-clock budget for retrieve/answer dependency chains.
 */
public final class Deadline {
    private final long deadlineNanos;

    private Deadline(long deadlineNanos) {
        this.deadlineNanos = deadlineNanos;
    }

    public static Deadline after(Duration budget) {
        long millis = budget == null ? 0L : Math.max(0L, budget.toMillis());
        return new Deadline(System.nanoTime() + millis * 1_000_000L);
    }

    public static Deadline afterMillis(long totalTimeoutMs) {
        return after(Duration.ofMillis(Math.max(0L, totalTimeoutMs)));
    }

    public void check() {
        if (System.nanoTime() > deadlineNanos) {
            throw new KnowledgeDependencyException("deadline", KnowledgeErrorCode.RETRIEVE_DEADLINE_EXCEEDED);
        }
    }
}
