package com.soilsync.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "privacy_consents")
public class PrivacyConsent {

    @Id
    private UUID id;

    @Column(name = "consented_at", nullable = false)
    private Instant consentedAt;

    @Column(name = "consent_version", nullable = false, length = 40)
    private String consentVersion;

    protected PrivacyConsent() {
    }

    public PrivacyConsent(UUID id, Instant consentedAt, String consentVersion) {
        this.id = id;
        this.consentedAt = consentedAt;
        this.consentVersion = consentVersion;
    }

    public UUID getId() {
        return id;
    }

    public Instant getConsentedAt() {
        return consentedAt;
    }

    public String getConsentVersion() {
        return consentVersion;
    }
}
