package com.g2rain.knowledge.task;

import com.g2rain.common.utils.Moments;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Schedules when a PENDING task may be claimed.
 * <p>
 * Delayed retries push {@code available_time} into the future. Immediate paths
 * ({@code processRevision} / {@code retry} / {@code dispatch}) must use
 * {@link #immediate()} so AFTER_COMMIT claim never races TIMESTAMP second rounding.
 */
public final class KnowledgeTaskSchedule {
    private KnowledgeTaskSchedule() {
    }

    /**
     * 立即就绪：已过期的可领取时间（秒截断后再减 1 秒），避免写入进位导致立刻 claim 失败。
     */
    public static LocalDateTime immediate() {
        return Moments.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(1);
    }
}
