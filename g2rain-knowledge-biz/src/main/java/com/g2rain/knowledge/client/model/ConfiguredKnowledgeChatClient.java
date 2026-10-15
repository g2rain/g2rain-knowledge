package com.g2rain.knowledge.client.model;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** OpenAI-compatible chat client. The request URL comes from configuration. */
@Component
public class ConfiguredKnowledgeChatClient implements KnowledgeChatClient {
    private final KnowledgeAnswerProperties properties;

    public ConfiguredKnowledgeChatClient(KnowledgeAnswerProperties properties) {
        this.properties = properties;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (blank(properties.getUrl()) || blank(properties.getModelName())) {
            throw new BusinessException(KnowledgeErrorCode.ANSWER_NOT_CONFIGURED);
        }
        try {
            ChatResponse response = ModelNexusRestClients
                .create(properties.getConnectTimeout(), properties.getReadTimeout())
                .post()
                .uri(properties.getUrl())
                .body(new ChatRequest(properties.getModelName(), List.of(
                    new Message("system", systemPrompt),
                    new Message("user", userPrompt))))
                .retrieve()
                .body(ChatResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null
                || blank(response.choices().getFirst().message().content())) {
                throw new BusinessException(KnowledgeErrorCode.ANSWER_INVALID_RESPONSE);
            }
            return response.choices().getFirst().message().content().trim();
        } catch (RestClientException exception) {
            throw new BusinessException(KnowledgeErrorCode.ANSWER_OPERATION_FAILED);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    record ChatRequest(String model, List<Message> messages) {
    }

    record Message(String role, String content) {
    }

    record ChatResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }
}
