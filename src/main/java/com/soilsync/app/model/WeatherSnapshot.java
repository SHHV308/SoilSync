package com.soilsync.app.model;

public record WeatherSnapshot(
    double temperatureC,
    int humidityPercent,
    double precipitationMm,
    int forecastRainProbabilityPercent,
    String forecastSummary,
    String source
) {
}
