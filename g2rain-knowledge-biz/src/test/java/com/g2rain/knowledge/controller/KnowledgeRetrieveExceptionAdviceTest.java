package com.g2rain.knowledge.controller;

import com.g2rain.common.exception.ExceptionProcessor;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.client.KnowledgeDependencyException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KnowledgeRetrieveExceptionAdviceTest {

    @Test
    void mapsDependencyFailureToHttp503WithResultBody() {
        ExceptionProcessor processor = (ex, locale) -> Result.error(ex.getErrorCode(), ex.getErrorMessage());
        KnowledgeRetrieveExceptionAdvice advice = new KnowledgeRetrieveExceptionAdvice(processor);

        ResponseEntity<Result<Void>> response = advice.handleDependencyUnavailable(
            new KnowledgeDependencyException("embedding", KnowledgeErrorCode.EMBEDDING_OPERATION_FAILED));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(KnowledgeErrorCode.EMBEDDING_OPERATION_FAILED.code(), response.getBody().getErrorCode());
    }
}
