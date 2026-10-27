package com.soilsync.app.model;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LocationCandidate(
    String id,
    String name,
    String admin1,
    String admin2,
    String country,
    String countryCode,
    double latitude,
    double longitude,
    String timezone,
    String climateZone,
    String metadataSource
) {
    public String displayName() {
        return Stream.of(name, admin1, admin2, country)
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .collect(Collectors.joining(", "));
    }

    public LocationCandidate withTimezone(String resolvedTimezone) {
        return new LocationCandidate(
            id, name, admin1, admin2, country, countryCode, latitude, longitude,
            resolvedTimezone, climateZone, metadataSource
        );
    }
}
