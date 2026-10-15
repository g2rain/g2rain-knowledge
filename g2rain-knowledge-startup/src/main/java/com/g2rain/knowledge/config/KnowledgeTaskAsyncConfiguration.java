package com.g2rain.knowledge.config;

import com.g2rain.common.web.PrincipalContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Isolates long-running knowledge processing from HTTP request threads.
 * Uses virtual threads and propagates Principal via {@link PrincipalContextHolder#wrap}.
 */
@Configuration
@EnableAsync
public class KnowledgeTaskAsyncConfiguration {
    @Bean("knowledgeTaskExecutor")
    AsyncTaskExecutor knowledgeTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("knowledge-task-");
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(PrincipalContextHolder::wrap);
        return executor;
    }
}
