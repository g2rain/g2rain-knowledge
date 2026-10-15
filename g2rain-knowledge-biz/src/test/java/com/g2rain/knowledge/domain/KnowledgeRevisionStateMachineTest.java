package com.g2rain.knowledge.domain;

import com.g2rain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeRevisionStateMachineTest {
    private final KnowledgeRevisionStateMachine stateMachine = new KnowledgeRevisionStateMachine();

    @Test
    void onlyDraftIsEditable() {
        assertDoesNotThrow(() -> stateMachine.requireEditable("DRAFT"));
        assertThrows(BusinessException.class, () -> stateMachine.requireEditable("REVIEWING"));
        assertThrows(BusinessException.class, () -> stateMachine.requireEditable("PUBLISHED"));
    }
}
