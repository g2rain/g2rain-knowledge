package com.g2rain.knowledge.domain;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeVisibility;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KnowledgePublicWriteGuardTest {

    @Mock private KnowledgeAccessService knowledgeAccessService;
    @InjectMocks private KnowledgePublicWriteGuard guard;

    @Test
    void privateOnlyDoesNotRequireAdminCompany() {
        guard.requireIfPublic("PRIVATE", KnowledgeVisibility.PRIVATE.name());
        verify(knowledgeAccessService, never()).requireAdminCompany();
    }

    @Test
    void publicTargetRequiresAdminCompany() {
        doThrow(new BusinessException(KnowledgeErrorCode.PUBLIC_WRITE_FORBIDDEN))
            .when(knowledgeAccessService).requireAdminCompany();
        assertThrows(BusinessException.class,
            () -> guard.requireIfPublic("PRIVATE", KnowledgeVisibility.PUBLIC.name()));
        verify(knowledgeAccessService).requireAdminCompany();
    }

    @Test
    void requireValidParsesVisibility() {
        assertEquals(KnowledgeVisibility.PUBLIC, guard.requireValid("PUBLIC"));
        assertEquals(KnowledgeVisibility.PRIVATE, guard.requireValid("private"));
    }
}
