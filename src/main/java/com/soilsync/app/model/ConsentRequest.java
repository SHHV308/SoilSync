package com.soilsync.app.model;

import jakarta.validation.constraints.AssertTrue;

public record ConsentRequest(
    @AssertTrue(message = "Explicit consent is required before advisory logging is enabled.")
    boolean accepted
) {
}
