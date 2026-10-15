package com.g2rain.knowledge.client.model;

/** Server-owned chat completion boundary for knowledge answers. */
public interface KnowledgeChatClient {
    String complete(String systemPrompt, String userPrompt);
}
