package com.soilsync.app.service;

import com.soilsync.app.model.CropAdviceRequest;
import com.soilsync.app.model.CropAdviceResponse;
import com.soilsync.app.model.LocationCandidate;
import com.soilsync.app.model.WeatherSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RegionalCropAdviceService {

    private static final Set<String> ISO_COUNTRY_CODES = Set.of(Locale.getISOCountries());

    private final RegionalWeatherClient weatherClient;
    private final RegionalKnowledgeBase knowledgeBase;

    public RegionalCropAdviceService(RegionalWeatherClient weatherClient, RegionalKnowledgeBase knowledgeBase) {
        this.weatherClient = weatherClient;
        this.knowledgeBase = knowledgeBase;
    }

    public List<LocationCandidate> searchLocations(String query) {
        if (query == null || query.trim().length() < 2 || query.length() > 120) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Enter a place name between 2 and 120 characters long."
            );
        }
        return weatherClient.searchLocations(query);
    }

    public LocationCandidate coordinateLocation(
        Double latitude,
        Double longitude,
        String region,
        String country,
        String countryCode
    ) {
        if (latitude == null || longitude == null
            || !Double.isFinite(latitude) || latitude < -90 || latitude > 90
            || !Double.isFinite(longitude) || longitude < -180 || longitude > 180
            || region == null || region.isBlank()
            || region.length() > 120
            || country == null || country.isBlank()
            || country.length() > 100
            || countryCode == null
            || !(ISO_COUNTRY_CODES.contains(countryCode.toUpperCase(Locale.ROOT))
                || "XK".equalsIgnoreCase(countryCode))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Coordinates require valid latitude/longitude and a country, two-letter country code, and region."
            );
        }

        return new LocationCandidate(
            "coordinates",
            "",
            region.trim(),
            "",
            country.trim(),
            countryCode.toUpperCase(Locale.ROOT),
            latitude,
            longitude,
            "",
            "UNCLASSIFIED",
            "User-provided coordinates and region"
        );
    }

    public CropAdviceResponse advise(CropAdviceRequest request) {
        OpenMeteoClient.WeatherAtLocation live = weatherClient.getWeather(
            request.region().trim(),
            request.location()
        );
        WeatherSnapshot weather = live.weather();
        LocationCandidate location = live.locationDetails();
        String displayLocation = location == null ? live.location() : location.displayName();
        List<String> recommendations = new ArrayList<>();
        List<String> dataSources = new ArrayList<>();
        dataSources.add("Live location and weather: Open-Meteo geocoding and forecast APIs (" + weather.source() + ")");

        String countryCode = location == null ? "" : location.countryCode();
        String country = location == null ? "the selected region" : location.country();
        var vettedDocument = knowledgeBase.findVettedDocument(
            countryCode,
            country,
            location == null ? null : location.admin1(),
            location == null ? null : location.admin2(),
            request.crop()
        );
        String coverageMessage;
        if (vettedDocument.isPresent()) {
            RegionalKnowledgeBase.KnowledgeDocument document = vettedDocument.get();
            recommendations.add(document.content());
            dataSources.add(document.citation());
            coverageMessage = "Vetted guidance is available for " + document.regionCovered()
                + " from the cited source. Follow that guidance only within its stated scope.";
        } else {
            coverageMessage = "No vetted crop-specific guidance document is available for "
                + request.crop() + " in " + country
                + ". The following are general safety principles only, not a local fertilizer prescription.";
            recommendations.add("Base fertilizer choices and rates on a recent soil test and a current recommendation from your local agricultural extension service.");
            recommendations.add("Consider organic matter and integrated nutrient management where appropriate for the crop and local conditions.");
            recommendations.add("Check local fertilizer and pesticide rules; this advisor does not verify local legal requirements.");
            dataSources.add("No vetted regional crop source is currently configured for this country and crop.");
        }

        if (weather.forecastRainProbabilityPercent() >= 70 || weather.precipitationMm() >= 5) {
            recommendations.add("The live forecast indicates rain risk. Check field conditions and local guidance before applying nutrients; this system has no locally validated timing threshold.");
        } else {
            recommendations.add("Use the live forecast alongside field conditions and local extension guidance; this system has no locally validated nutrient-application timing threshold.");
        }

        String answer = coverageMessage + " " + weather.forecastSummary()
            + " Exact fertilizer or pesticide products and rates are not provided without a cited, applicable local source.";
        return new CropAdviceResponse(
            displayLocation,
            request.crop(),
            request.growthStage(),
            answer,
            recommendations,
            weather,
            dataSources,
            Instant.now(),
            location
        );
    }
}
