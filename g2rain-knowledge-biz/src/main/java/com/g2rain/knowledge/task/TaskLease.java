package com.g2rain.knowledge.task;

import java.time.LocalDateTime;

/** Immutable fencing token returned only after a task is atomically claimed. */
public record TaskLease(Long taskId, Long organId, Long revisionId, String taskType,
                        String workerId, long generation, LocalDateTime expiresAt) {
}
