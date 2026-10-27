package com.soilsync.app.service;

import com.soilsync.app.model.LocationCandidate;

import java.util.List;

public interface RegionalWeatherClient {

    List<LocationCandidate> searchLocations(String query);

    OpenMeteoClient.WeatherAtLocation getWeather(String region, LocationCandidate selection);

    default OpenMeteoClient.WeatherAtLocation getWeather(String region) {
        return getWeather(region, null);
    }
}
