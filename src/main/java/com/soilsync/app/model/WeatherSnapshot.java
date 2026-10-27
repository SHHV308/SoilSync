package com.soilsync.app.model;

public record WeatherSnapshot(
    double temperatureC,
    int humidityPercent,
    double precipitationMm,
    int forecastRainProbabilityPercent,
    String forecastSummary,
    String source,
    String forecastDate,
    double forecastPrecipitationMm,
    double forecastHighC
) {
    public WeatherSnapshot(
        double temperatureC,
        int humidityPercent,
        double precipitationMm,
        int forecastRainProbabilityPercent,
        String forecastSummary,
        String source
    ) {
        this(
            temperatureC,
            humidityPercent,
            precipitationMm,
            forecastRainProbabilityPercent,
            forecastSummary,
            source,
            "",
            precipitationMm,
            temperatureC
        );
    }
}
