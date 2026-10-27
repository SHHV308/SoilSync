package com.soilsync.app.model;

public record LocationSearchRequest(
    String query,
    Double latitude,
    Double longitude,
    String region,
    String country,
    String countryCode
) {
}
