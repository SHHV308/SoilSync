package com.soilsync.app.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public record CropAdviceResponse(
    String location,
    String crop,
    String growthStage,
    String answer,
    List<String> recommendations,
    WeatherSnapshot weather,
    List<String> dataSources,
    Instant fetchedAt,
    LocationCandidate locationDetails,
    @JsonInclude(JsonInclude.Include.NON_NULL) Long advisoryLogId
) {
    public CropAdviceResponse(
        String location,
        String crop,
        String growthStage,
        String answer,
        List<String> recommendations,
        WeatherSnapshot weather,
        List<String> dataSources,
        Instant fetchedAt,
        LocationCandidate locationDetails
    ) {
        this(
            location,
            crop,
            growthStage,
            answer,
            recommendations,
            weather,
            dataSources,
            fetchedAt,
            locationDetails,
            null
        );
    }

    public CropAdviceResponse withAdvisoryLogId(Long logId) {
        return new CropAdviceResponse(
            location,
            crop,
            growthStage,
            answer,
            recommendations,
            weather,
            dataSources,
            fetchedAt,
            locationDetails,
            logId
        );
    }
}
