package com.g2rain.knowledge.controller;

import com.g2rain.common.exception.ExceptionProcessor;
import com.g2rain.common.model.Result;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.knowledge.client.KnowledgeDependencyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps retrieve/answer dependency failures to HTTP 503 while keeping the common Result body.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {
    KnowledgeRetrieveController.class,
    KnowledgeAnswerController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class KnowledgeRetrieveExceptionAdvice {

    private final ExceptionProcessor exceptionProcessor;

    public KnowledgeRetrieveExceptionAdvice(ExceptionProcessor exceptionProcessor) {
        this.exceptionProcessor = exceptionProcessor;
    }

    @ExceptionHandler(KnowledgeDependencyException.class)
    public ResponseEntity<Result<Void>> handleDependencyUnavailable(KnowledgeDependencyException ex) {
        log.warn("知识检索依赖不可用 dependency={} code={}", ex.getDependency(), ex.getErrorCode(), ex);
        Result<Void> result = exceptionProcessor.process(ex, PrincipalContextHolder.getAcceptLanguage());
        return new ResponseEntity<>(result, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
