package com.soilsync.app.model;

import jakarta.validation.constraints.NotNull;

public record ApplicationOutcomeRequest(@NotNull ApplicationOutcome outcome) {
}
