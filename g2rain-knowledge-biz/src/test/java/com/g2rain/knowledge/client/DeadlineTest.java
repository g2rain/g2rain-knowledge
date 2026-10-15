package com.g2rain.knowledge.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeadlineTest {

    @Test
    void allowsWorkWithinBudget() {
        Deadline deadline = Deadline.afterMillis(5_000L);
        assertDoesNotThrow(deadline::check);
    }

    @Test
    void rejectsWhenBudgetExhausted() throws InterruptedException {
        Deadline deadline = Deadline.afterMillis(1L);
        Thread.sleep(5L);
        assertThrows(KnowledgeDependencyException.class, deadline::check);
    }
}
