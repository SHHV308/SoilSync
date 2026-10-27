package com.soilsync.app.model;

import jakarta.validation.constraints.NotNull;

public record AdvisoryFeedbackRequest(@NotNull AdvisoryFeedback feedback) {
}
