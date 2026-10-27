package com.soilsync.app.service;

import com.soilsync.app.model.PrivacyConsent;
import com.soilsync.app.repository.AdvisoryLogRepository;
import com.soilsync.app.repository.PrivacyConsentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PrivacyConsentService {

    public static final String COOKIE_NAME = "soilsync_privacy_id";
    public static final String NOTICE = "If you opt in, SoilSync stores this browser's advisory and chat records, "
        + "including question and answer text after common identifier redaction, selected country and region, "
        + "crop, growth stage, soil type, weather summary, risk flag, sources, feedback, and application outcome. "
        + "Exact coordinates are not stored. Automated redaction may miss personal details; do not include "
        + "personal or sensitive information. Delete this browser's records at any time using "
        + "DELETE /api/privacy/data.";

    private final PrivacyConsentRepository consentRepository;
    private final AdvisoryLogRepository advisoryLogRepository;
    private final String consentVersion;

    public PrivacyConsentService(
        PrivacyConsentRepository consentRepository,
        AdvisoryLogRepository advisoryLogRepository,
        @Value("${soilsync.privacy.consent-version:v1}") String consentVersion
    ) {
        this.consentRepository = consentRepository;
        this.advisoryLogRepository = advisoryLogRepository;
        this.consentVersion = consentVersion;
    }

    @Transactional
    public ConsentGrant grantConsent(String token) {
        Optional<PrivacyConsent> existing = findConsent(token);
        PrivacyConsent consent = existing.orElseGet(() -> consentRepository.save(
            new PrivacyConsent(UUID.randomUUID(), Instant.now(), consentVersion)
        ));
        return new ConsentGrant(statusFor(consent), consent.getId().toString());
    }

    @Transactional(readOnly = true)
    public ConsentStatus status(String token) {
        return findConsent(token).map(this::statusFor)
            .orElseGet(() -> new ConsentStatus(false, null, null, NOTICE));
    }

    @Transactional
    public void deleteData(String token) {
        Optional<UUID> consentId = parseToken(token);
        if (consentId.isEmpty() || !consentRepository.existsById(consentId.get())) {
            return;
        }
        advisoryLogRepository.deleteAllByConsentId(consentId.get());
        consentRepository.deleteById(consentId.get());
    }

    static Optional<UUID> parseToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(token));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<PrivacyConsent> findConsent(String token) {
        return parseToken(token).flatMap(consentRepository::findById);
    }

    private ConsentStatus statusFor(PrivacyConsent consent) {
        return new ConsentStatus(true, consent.getConsentVersion(), consent.getConsentedAt(), NOTICE);
    }

    public record ConsentStatus(
        boolean consented,
        String consentVersion,
        Instant consentedAt,
        String notice
    ) {
    }

    public record ConsentGrant(ConsentStatus status, String token) {
    }
}
