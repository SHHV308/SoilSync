package com.soilsync.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CropAdviceRequest(
    @Size(max = 120)
    @NotBlank String region,
    @Size(max = 120)
    @NotBlank String crop,
    @Size(max = 80)
    @NotBlank String growthStage,
    @Size(max = 100)
    @NotBlank String soilType,
    @Size(max = 500)
    String question,
    LocationCandidate location
) {
    public CropAdviceRequest(String region, String crop, String growthStage, String soilType, String question) {
        this(region, crop, growthStage, soilType, question, null);
    }
}
