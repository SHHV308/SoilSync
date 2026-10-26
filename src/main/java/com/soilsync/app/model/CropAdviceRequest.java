package com.soilsync.app.model;

import jakarta.validation.constraints.NotBlank;

public record CropAdviceRequest(
    @NotBlank String region,
    @NotBlank String crop,
    @NotBlank String growthStage,
    @NotBlank String soilType,
    String question
) {
}
