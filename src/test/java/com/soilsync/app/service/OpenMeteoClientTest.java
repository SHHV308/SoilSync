package com.soilsync.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soilsync.app.model.LocationCandidate;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenMeteoClientTest {

    private static final String AMBIGUOUS_LOCATIONS = """
        {"results":[
          {"id":101,"name":"Springfield","admin1":"Illinois","country":"United States","country_code":"US","latitude":39.8,"longitude":-89.6,"timezone":"America/Chicago"},
          {"id":202,"name":"Springfield","admin1":"Queensland","country":"Australia","country_code":"AU","latitude":-24.9,"longitude":152.3,"timezone":"Australia/Brisbane"}
        ]}
        """;

    private HttpServer server;
    private OpenMeteoClient client;

    @BeforeEach
    void startGeocodingServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/search", this::respondWithAmbiguousLocations);
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new OpenMeteoClient(
            new ObjectMapper(),
            HttpClient.newHttpClient(),
            baseUrl + "/search",
            baseUrl + "/forecast"
        );
    }

    @AfterEach
    void stopGeocodingServer() {
        server.stop(0);
    }

    @Test
    void globalSearchReturnsAllCandidatesWithTheirCountries() {
        var candidates = client.searchLocations("Springfield");

        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(LocationCandidate::countryCode).containsExactly("US", "AU");
        assertThat(candidates).extracting(LocationCandidate::timezone)
            .containsExactly("America/Chicago", "Australia/Brisbane");
    }

    @Test
    void ambiguousPlaceCannotBeUsedUntilTheUserSelectsACandidate() {
        assertThatThrownBy(() -> client.getWeather("Springfield"))
            .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    private void respondWithAmbiguousLocations(HttpExchange exchange) throws IOException {
        byte[] response = AMBIGUOUS_LOCATIONS.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
