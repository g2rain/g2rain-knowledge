package com.g2rain.knowledge.client;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.ErrorCode;

/**
 * Signals that a retrieve/answer dependency is unavailable. Mapped to HTTP 503
 * by a scoped controller advice; must not leak provider bodies.
 */
public class KnowledgeDependencyException extends BusinessException {
    private final String dependency;

    public KnowledgeDependencyException(String dependency, ErrorCode errorCode) {
        super(errorCode);
        this.dependency = dependency == null ? "unknown" : dependency;
    }

    public KnowledgeDependencyException(String dependency, ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
        this.dependency = dependency == null ? "unknown" : dependency;
    }

    public String getDependency() {
        return dependency;
    }
}
