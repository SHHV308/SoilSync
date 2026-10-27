package com.soilsync.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soilsync.app.model.AdvisoryFeedback;
import com.soilsync.app.model.AdvisoryLog;
import com.soilsync.app.model.ApplicationOutcome;
import com.soilsync.app.model.CropAdviceRequest;
import com.soilsync.app.model.CropAdviceResponse;
import com.soilsync.app.model.LocationCandidate;
import com.soilsync.app.repository.AdvisoryLogRepository;
import com.soilsync.app.repository.PrivacyConsentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdvisoryLogService {

    private static final int MAX_QUESTION_LENGTH = 2000;
    private static final int MAX_ANSWER_LENGTH = 8000;
    private static final int MAX_SOURCES_LENGTH = 6000;

    private final PrivacyConsentRepository consentRepository;
    private final AdvisoryLogRepository advisoryLogRepository;
    private final ObjectMapper objectMapper;

    public AdvisoryLogService(
        PrivacyConsentRepository consentRepository,
        AdvisoryLogRepository advisoryLogRepository,
        ObjectMapper objectMapper
    ) {
        this.consentRepository = consentRepository;
        this.advisoryLogRepository = advisoryLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Optional<Long> recordIfConsented(
        String token,
        CropAdviceRequest request,
        CropAdviceResponse response
    ) {
        UUID consentId = PrivacyConsentService.parseToken(token)
            .filter(consentRepository::existsById)
            .orElse(null);
        if (consentId == null) {
            return Optional.empty();
        }

        LocationCandidate location = response.locationDetails();
        String countryCode = location == null ? null : location.countryCode();
        String region = location == null ? request.region() : selectedRegion(location, request.region());
        boolean riskFlag = response.weather().forecastRainProbabilityPercent() >= 70
            || response.weather().precipitationMm() >= 5;

        AdvisoryLog saved = advisoryLogRepository.save(new AdvisoryLog(
            consentId,
            Instant.now(),
            countryCode == null || countryCode.isBlank() ? null : countryCode,
            PrivacyTextRedactor.limit(PrivacyTextRedactor.redact(region), 120),
            PrivacyTextRedactor.limit(PrivacyTextRedactor.redact(request.crop()), 120),
            PrivacyTextRedactor.limit(PrivacyTextRedactor.redact(request.growthStage()), 80),
            PrivacyTextRedactor.limit(PrivacyTextRedactor.redact(request.soilType()), 100),
            weatherSnapshot(response),
            riskFlag,
            PrivacyTextRedactor.limit(
                PrivacyTextRedactor.redact(request.question()),
                MAX_QUESTION_LENGTH
            ),
            PrivacyTextRedactor.limit(
                PrivacyTextRedactor.redact(response.answer()),
                MAX_ANSWER_LENGTH
            ),
            PrivacyTextRedactor.limit(
                PrivacyTextRedactor.redact(String.join("\n", response.dataSources())),
                MAX_SOURCES_LENGTH
            )
        ));
        return Optional.of(saved.getId());
    }

    @Transactional
    public void updateFeedback(String token, Long logId, AdvisoryFeedback feedback) {
        AdvisoryLog log = ownedLog(token, logId);
        log.setFeedback(feedback);
        advisoryLogRepository.save(log);
    }

    @Transactional
    public void updateOutcome(String token, Long logId, ApplicationOutcome outcome) {
        AdvisoryLog log = ownedLog(token, logId);
        log.setApplicationOutcome(outcome);
        advisoryLogRepository.save(log);
    }

    private AdvisoryLog ownedLog(String token, Long logId) {
        UUID consentId = PrivacyConsentService.parseToken(token)
            .filter(consentRepository::existsById)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Advisory record not found."));
        return advisoryLogRepository.findByIdAndConsentId(logId, consentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Advisory record not found."));
    }

    private String weatherSnapshot(CropAdviceResponse response) {
        try {
            return objectMapper.writeValueAsString(response.weather());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode the weather snapshot for the advisory log.", exception);
        }
    }

    private static String selectedRegion(LocationCandidate location, String fallback) {
        if (location.admin1() != null && !location.admin1().isBlank()) {
            return location.admin1();
        }
        if (location.admin2() != null && !location.admin2().isBlank()) {
            return location.admin2();
        }
        return fallback;
    }
}
