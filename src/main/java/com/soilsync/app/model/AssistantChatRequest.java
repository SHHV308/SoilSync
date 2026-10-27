package com.soilsync.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssistantChatRequest(
    @NotBlank @Size(max = 4000) String question,
    LocationCandidate location,
    @Size(max = 120) String crop,
    @Size(max = 80) String growthStage,
    @Size(max = 100) String soilType,
    @Pattern(regexp = "(?i)^(en|es|fr|pt|hi|sw)$")
    String preferredLanguage,
    @Pattern(regexp = "(?i)^(metric|imperial)$")
    String units
) {
}
