package com.soilsync.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soilsync.app.model.LocationCandidate;
import com.soilsync.app.model.WeatherSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OpenMeteoClient implements RegionalWeatherClient {

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String SOURCE = "https://open-meteo.com/";
    private static final Set<String> ISO_COUNTRY_CODES = Set.of(Locale.getISOCountries());

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String geocodingUrl;
    private final String forecastUrl;

    @Autowired
    public OpenMeteoClient(ObjectMapper objectMapper) {
        this(
            objectMapper,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(),
            GEOCODING_URL,
            FORECAST_URL
        );
    }

    OpenMeteoClient(ObjectMapper objectMapper, HttpClient httpClient, String geocodingUrl, String forecastUrl) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.geocodingUrl = geocodingUrl;
        this.forecastUrl = forecastUrl;
    }

    @Override
    public WeatherAtLocation getWeather(String region) {
        return getWeather(region, null);
    }

    @Override
    public List<LocationCandidate> searchLocations(String query) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Enter at least two characters to search for a place.");
        }

        JsonNode geocoding = getJson(geocodingUrl + "?name=" + encode(query.trim())
            + "&count=10&language=en&format=json");
        JsonNode results = geocoding.path("results");
        if (!results.isArray()) {
            return List.of();
        }

        List<LocationCandidate> locations = new ArrayList<>();
        for (JsonNode result : results) {
            locations.add(new LocationCandidate(
                requiredId(result, "id"),
                requiredText(result, "name"),
                optionalText(result, "admin1"),
                optionalText(result, "admin2"),
                requiredText(result, "country"),
                requiredText(result, "country_code").toUpperCase(Locale.ROOT),
                requiredNumber(result, "latitude"),
                requiredNumber(result, "longitude"),
                optionalText(result, "timezone"),
                "UNCLASSIFIED",
                "Open-Meteo geocoding"
            ));
        }
        return List.copyOf(locations);
    }

    @Override
    public WeatherAtLocation getWeather(String region, LocationCandidate selection) {
        LocationCandidate place;
        if (selection == null) {
            place = resolveLocationSelection(searchLocations(region), null);
        } else if ("coordinates".equals(selection.id())) {
            place = validateCoordinateSelection(selection);
        } else {
            place = resolveLocationSelection(searchLocations(region), selection.id());
        }

        String forecastUri = forecastUrl
            + "?latitude=" + place.latitude()
            + "&longitude=" + place.longitude()
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
        String timezone = optionalText(forecast, "timezone");
        if (timezone.isBlank()) {
            timezone = place.timezone();
        }
        LocationCandidate resolvedPlace = place.withTimezone(timezone.isBlank() ? "Unknown" : timezone);
        String summary = String.format(
            Locale.ROOT,
            "Forecast for %s: up to %.0f%% rain chance, %.1f mm expected rain, and a high near %.1f°C.",
            forecastDate, (double) rainProbability, expectedRain, expectedHigh
        );

        return new WeatherAtLocation(
            resolvedPlace.displayName(),
            new WeatherSnapshot(
                temperature,
                humidity,
                precipitation,
                rainProbability,
                summary,
                SOURCE
            ),
            resolvedPlace
        );
    }

    static LocationCandidate resolveLocationSelection(List<LocationCandidate> results, String selectedId) {
        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "We could not find that place. Try a city, region, or country name.");
        }
        if (selectedId == null && results.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Several places match. Search for the place and select the country and region before requesting advice.");
        }
        if (selectedId == null) {
            return results.get(0);
        }
        return results.stream()
            .filter(candidate -> candidate.id().equals(selectedId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "The selected place no longer matches the search. Search again and select a result."));
    }

    private static LocationCandidate validateCoordinateSelection(LocationCandidate selection) {
        if (!Double.isFinite(selection.latitude()) || selection.latitude() < -90 || selection.latitude() > 90
            || !Double.isFinite(selection.longitude()) || selection.longitude() < -180 || selection.longitude() > 180
            || selection.country() == null || selection.country().isBlank()
            || selection.countryCode() == null
            || !(ISO_COUNTRY_CODES.contains(selection.countryCode().toUpperCase(Locale.ROOT))
                || "XK".equalsIgnoreCase(selection.countryCode()))
            || selection.admin1() == null || selection.admin1().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Coordinates require valid latitude/longitude and a country, two-letter country code, and region.");
        }
        return new LocationCandidate(
            "coordinates",
            "",
            selection.admin1().trim(),
            "",
            selection.country().trim(),
            selection.countryCode().toUpperCase(Locale.ROOT),
            selection.latitude(),
            selection.longitude(),
            selection.timezone(),
            "UNCLASSIFIED",
            "User-provided coordinates and region"
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

    private static String optionalText(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        return value.isTextual() ? value.asText().trim() : "";
    }

    private static String requiredId(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (!value.canConvertToLong()) {
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

    public record WeatherAtLocation(String location, WeatherSnapshot weather, LocationCandidate locationDetails) {
        public WeatherAtLocation(String location, WeatherSnapshot weather) {
            this(location, weather, null);
        }
    }
}
