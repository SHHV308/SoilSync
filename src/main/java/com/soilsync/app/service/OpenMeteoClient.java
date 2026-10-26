package com.soilsync.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soilsync.app.model.WeatherSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class OpenMeteoClient implements RegionalWeatherClient {

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String SOURCE = "https://open-meteo.com/";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenMeteoClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Override
    public WeatherAtLocation getWeather(String region) {
        JsonNode geocoding = getJson(GEOCODING_URL + "?name=" + encode(region + ", India")
            + "&count=1&language=en&format=json");
        JsonNode results = geocoding.path("results");
        if (!results.isArray() || results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "We could not find that place. Try a nearby town, district, or city in India.");
        }

        JsonNode place = results.get(0);
        double latitude = requiredNumber(place, "latitude");
        double longitude = requiredNumber(place, "longitude");
        String placeName = requiredText(place, "name");
        String state = place.path("admin1").asText("");
        String location = state.isBlank() ? placeName : placeName + ", " + state;

        String forecastUri = FORECAST_URL
            + "?latitude=" + latitude
            + "&longitude=" + longitude
            + "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code"
            + "&daily=precipitation_probability_max,precipitation_sum,temperature_2m_max"
            + "&forecast_days=3&timezone=auto";
        JsonNode forecast = getJson(forecastUri);
        JsonNode current = forecast.required("current");
        JsonNode daily = forecast.required("daily");

        double temperature = requiredNumber(current, "temperature_2m");
        int humidity = (int) requiredNumber(current, "relative_humidity_2m");
        double precipitation = requiredNumber(current, "precipitation");
        int rainProbability = (int) firstArrayNumber(daily, "precipitation_probability_max");
        double expectedRain = firstArrayNumber(daily, "precipitation_sum");
        double expectedHigh = firstArrayNumber(daily, "temperature_2m_max");
        String forecastDate = firstArrayText(daily, "time");
        String summary = String.format(
            "Forecast for %s: up to %.0f%% rain chance, %.1f mm expected rain, and a high near %.1f°C.",
            forecastDate, (double) rainProbability, expectedRain, expectedHigh
        );

        return new WeatherAtLocation(
            location,
            new WeatherSnapshot(
                temperature,
                humidity,
                precipitation,
                rainProbability,
                summary,
                SOURCE
            )
        );
    }

    private JsonNode getJson(String address) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(address))
            .timeout(Duration.ofSeconds(12))
            .header("Accept", "application/json")
            .GET()
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The live weather service returned HTTP " + response.statusCode() + ".");
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "The live weather request was interrupted. Please try again.", exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Live regional weather is temporarily unavailable. Please try again shortly.", exception);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static double requiredNumber(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (!value.isNumber()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "The live weather service returned incomplete " + field + " data.");
        }
        return value.asDouble();
    }

    private static String requiredText(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "The live location service returned incomplete place data.");
        }
        return value.asText();
    }

    private static double firstArrayNumber(JsonNode parent, String field) {
        JsonNode values = parent.path(field);
        if (!values.isArray() || values.isEmpty() || !values.get(0).isNumber()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "The live weather service returned incomplete forecast data.");
        }
        return values.get(0).asDouble();
    }

    private static String firstArrayText(JsonNode parent, String field) {
        JsonNode values = parent.path(field);
        if (!values.isArray() || values.isEmpty() || !values.get(0).isTextual()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "The live weather service returned incomplete forecast dates.");
        }
        return values.get(0).asText();
    }

    public record WeatherAtLocation(String location, WeatherSnapshot weather) {
    }
}
