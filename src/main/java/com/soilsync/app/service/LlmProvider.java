package com.soilsync.app.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface LlmProvider {

    LlmReply complete(List<LlmMessage> messages, List<JsonNode> tools);

    record LlmMessage(
        String role,
        String content,
        String toolCallId,
        List<LlmToolCall> toolCalls
    ) {
        public static LlmMessage text(String role, String content) {
            return new LlmMessage(role, content, null, List.of());
        }

        public static LlmMessage toolCall(String content, List<LlmToolCall> toolCalls) {
            return new LlmMessage("assistant", content, null, toolCalls);
        }

        public static LlmMessage toolResult(String toolCallId, String content) {
            return new LlmMessage("tool", content, toolCallId, List.of());
        }
    }

    record LlmToolCall(String id, String name, String arguments) {
    }

    record LlmReply(String content, List<LlmToolCall> toolCalls) {
    }
}
