package com.soilsync.app.service;

public interface RegionalWeatherClient {

    OpenMeteoClient.WeatherAtLocation getWeather(String region);
}
