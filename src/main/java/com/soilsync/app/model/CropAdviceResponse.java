package com.soilsync.app.model;

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
    LocationCandidate locationDetails
) {
}
