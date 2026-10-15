package com.g2rain.knowledge.domain;

import java.time.Duration;

/** Backoff and lease constants are centralized so Workers do not improvise retries. */
public final class KnowledgeTaskPolicy {
    private static final Duration BASE_DELAY = Duration.ofSeconds(15);
    private static final Duration MAX_DELAY = Duration.ofMinutes(15);

    private KnowledgeTaskPolicy() {
    }

    public static Duration nextDelay(int attemptNo) {
        long multiplier = 1L << Math.min(Math.max(attemptNo, 0), 6);
        Duration delay = BASE_DELAY.multipliedBy(multiplier);
        return delay.compareTo(MAX_DELAY) > 0 ? MAX_DELAY : delay;
    }
}
