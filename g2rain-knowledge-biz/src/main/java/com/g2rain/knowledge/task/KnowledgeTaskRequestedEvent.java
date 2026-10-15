package com.g2rain.knowledge.task;

/** Published after a PROCESS task commits so execution never precedes its row. */
public record KnowledgeTaskRequestedEvent(Long taskId) {
}
