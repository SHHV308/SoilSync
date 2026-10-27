package com.soilsync.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class OpenAiCompatibleLlmProvider implements LlmProvider {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String model;
    private final String apiKey;

    public OpenAiCompatibleLlmProvider(
        ObjectMapper objectMapper,
        @Value("${soilsync.llm.base-url:}") String baseUrl,
        @Value("${soilsync.llm.model:}") String model,
        @Value("${soilsync.llm.api-key:}") String apiKey
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public LlmReply complete(List<LlmMessage> messages, List<JsonNode> tools) {
        if (baseUrl.isBlank() || model.isBlank() || apiKey.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The assistant provider is not configured. Set its base URL, model, and API key."
            );
        }

        HttpRequest request = HttpRequest.newBuilder(endpoint())
            .timeout(Duration.ofSeconds(35))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(serialize(buildRequest(messages, tools))))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The assistant provider returned HTTP " + response.statusCode() + "."
                );
            }
            return parseReply(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The assistant request was interrupted. Please try again.",
                exception
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "The assistant provider is temporarily unavailable.",
                exception
            );
        }
    }

    private URI endpoint() {
        try {
            URI base = URI.create(baseUrl.trim());
            String scheme = base.getScheme() == null ? "" : base.getScheme().toLowerCase(Locale.ROOT);
            String host = base.getHost() == null ? "" : base.getHost().toLowerCase(Locale.ROOT);
            boolean localHttp = "http".equals(scheme)
                && ("localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host));
            if (!"https".equals(scheme) && !localHttp) {
                throw new IllegalArgumentException("The configured provider URL must use HTTPS.");
            }
            if (host.isBlank()
                || base.getUserInfo() != null
                || base.getQuery() != null
                || base.getFragment() != null) {
                throw new IllegalArgumentException("The configured provider URL must use HTTPS.");
            }
            String path = base.toString().replaceAll("/+$", "");
            if (!path.endsWith("/chat/completions")) {
                path += "/chat/completions";
            }
            return URI.create(path);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The configured assistant provider URL is invalid or insecure.",
                exception
            );
        }
    }

    private ObjectNode buildRequest(List<LlmMessage> messages, List<JsonNode> tools) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);
        root.put("max_tokens", 1200);
        ArrayNode messageArray = root.putArray("messages");
        for (LlmMessage message : messages) {
            ObjectNode messageNode = messageArray.addObject();
            messageNode.put("role", message.role());
            if (message.content() != null) {
                messageNode.put("content", message.content());
            }
            if (message.toolCallId() != null) {
                messageNode.put("tool_call_id", message.toolCallId());
            }
            if (!message.toolCalls().isEmpty()) {
                ArrayNode calls = messageNode.putArray("tool_calls");
                for (LlmToolCall toolCall : message.toolCalls()) {
                    ObjectNode call = calls.addObject();
                    call.put("id", toolCall.id());
                    call.put("type", "function");
                    ObjectNode function = call.putObject("function");
                    function.put("name", toolCall.name());
                    function.put("arguments", toolCall.arguments());
                }
            }
        }
        if (!tools.isEmpty()) {
            ArrayNode toolArray = root.putArray("tools");
            tools.forEach(toolArray::add);
            root.put("tool_choice", "auto");
        }
        return root;
    }

    private String serialize(ObjectNode body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode the assistant provider request.", exception);
        }
    }

    private LlmReply parseReply(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null) {
                throw invalidProviderResponse();
            }
            JsonNode message = root.path("choices").path(0).path("message");
            if (message.isMissingNode()) {
                throw invalidProviderResponse();
            }

            JsonNode contentNode = message.path("content");
            String content = contentNode.isTextual() ? contentNode.asText() : null;
            List<LlmToolCall> calls = new ArrayList<>();
            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray()) {
                for (JsonNode toolCall : toolCalls) {
                    JsonNode function = toolCall.path("function");
                    JsonNode id = toolCall.path("id");
                    JsonNode name = function.path("name");
                    JsonNode arguments = function.path("arguments");
                    if (!id.isTextual() || !name.isTextual() || !arguments.isTextual()) {
                        throw invalidProviderResponse();
                    }
                    calls.add(new LlmToolCall(id.asText(), name.asText(), arguments.asText()));
                }
            }
            if ((content == null || content.isBlank()) && calls.isEmpty()) {
                throw invalidProviderResponse();
            }
            return new LlmReply(content, List.copyOf(calls));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "The assistant provider returned invalid JSON.",
                exception
            );
        }
    }

    private static ResponseStatusException invalidProviderResponse() {
        return new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "The assistant provider returned an incomplete response."
        );
    }
}
