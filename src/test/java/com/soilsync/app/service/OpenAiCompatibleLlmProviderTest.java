package com.soilsync.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleLlmProviderTest {

    private HttpServer server;
    private OpenAiCompatibleLlmProvider provider;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeEach
    void startProviderServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", this::respondWithToolCall);
        server.start();
        provider = new OpenAiCompatibleLlmProvider(
            new ObjectMapper(),
            "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
            "test-model",
            "test-api-key"
        );
    }

    @AfterEach
    void stopProviderServer() {
        server.stop(0);
    }

    @Test
    void sendsCompatibleToolCallingRequestAndParsesToolCalls() {
        ObjectNode tool = new ObjectMapper().createObjectNode();
        tool.put("type", "function");
        tool.putObject("function").put("name", "get_live_weather_forecast");

        LlmProvider.LlmReply reply = provider.complete(
            List.of(LlmProvider.LlmMessage.text("user", "Check my local forecast.")),
            List.of(tool)
        );

        assertThat(reply.content()).isNull();
        assertThat(reply.toolCalls()).containsExactly(
            new LlmProvider.LlmToolCall("call-1", "get_live_weather_forecast", "{}")
        );
        assertThat(requestBody.get()).contains("\"model\":\"test-model\"", "\"tool_choice\":\"auto\"");
        assertThat(authorization.get()).isEqualTo("Bearer test-api-key");
    }

    @Test
    void missingCredentialsFailClearlyWithoutCallingTheProvider() {
        OpenAiCompatibleLlmProvider unconfigured = new OpenAiCompatibleLlmProvider(
            new ObjectMapper(),
            "https://llm.example/v1",
            "test-model",
            ""
        );

        assertThatThrownBy(() -> unconfigured.complete(
            List.of(LlmProvider.LlmMessage.text("user", "Hello")),
            List.of()
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void rejectsRemoteHttpEndpointsToProtectTheApiKey() {
        OpenAiCompatibleLlmProvider insecure = new OpenAiCompatibleLlmProvider(
            new ObjectMapper(),
            "http://llm.example/v1",
            "test-model",
            "test-api-key"
        );

        assertThatThrownBy(() -> insecure.complete(
            List.of(LlmProvider.LlmMessage.text("user", "Hello")),
            List.of()
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    private void respondWithToolCall(HttpExchange exchange) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        byte[] response = """
            {"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[
              {"id":"call-1","type":"function","function":{
                "name":"get_live_weather_forecast","arguments":"{}"
              }}
            ]}}]}
            """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
