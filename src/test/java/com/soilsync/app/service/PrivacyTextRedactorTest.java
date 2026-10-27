package com.soilsync.app.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrivacyTextRedactorTest {

    @Test
    void redactsCommonContactLocationAndCredentialPatterns() {
        String text = "alice@example.com +1 (555) 123-4567 192.168.1.10 "
            + "123 Main Street password: secret-value";

        String redacted = PrivacyTextRedactor.redact(text);

        assertThat(redacted).contains(
            "[REDACTED_EMAIL]",
            "[REDACTED_PHONE]",
            "[REDACTED_IP]",
            "[REDACTED_ADDRESS]",
            "[REDACTED_SECRET]"
        );
        assertThat(redacted).doesNotContain(
            "alice@example.com",
            "555",
            "192.168.1.10",
            "123 Main Street",
            "secret-value"
        );
    }

    @Test
    void leavesNullValuesAndLimitsLongTextWithAnExplicitMarker() {
        assertThat(PrivacyTextRedactor.redact(null)).isNull();
        assertThat(PrivacyTextRedactor.limit(null, 10)).isNull();
        assertThat(PrivacyTextRedactor.limit("x".repeat(50), 40))
            .hasSize(40)
            .endsWith("[truncated in privacy log]");
    }
}
