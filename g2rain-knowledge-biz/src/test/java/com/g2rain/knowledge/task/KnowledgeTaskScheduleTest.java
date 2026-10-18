package com.g2rain.knowledge.task;

import com.g2rain.common.utils.Moments;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class KnowledgeTaskScheduleTest {

    @Test
    void immediateIsAlreadyDue() {
        assertFalse(KnowledgeTaskSchedule.immediate().isAfter(Moments.now()));
    }
}
