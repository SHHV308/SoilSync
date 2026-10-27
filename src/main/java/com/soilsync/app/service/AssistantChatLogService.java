package com.soilsync.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soilsync.app.model.AdvisoryFeedback;
import com.soilsync.app.model.ApplicationOutcome;
import com.soilsync.app.model.AssistantChatLog;
import com.soilsync.app.model.AssistantChatRequest;
import com.soilsync.app.model.AssistantChatResponse;
import com.soilsync.app.model.LocationCandidate;
import com.soilsync.app.model.WeatherSnapshot;
import com.soilsync.app.repository.AssistantChatLogRepository;
import com.soilsync.app.repository.PrivacyConsentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssistantChatLogService {

    private final PrivacyConsentRepository consentRepository;
    private final AssistantChatLogRepository chatLogRepository;
    private final ObjectMapper objectMapper;

    public AssistantChatLogService(
        PrivacyConsentRepository consentRepository,
        AssistantChatLogRepository chatLogRepository,
        ObjectMapper objectMapper
    ) {
        this.consentRepository = consentRepository;
        this.chatLogRepository = chatLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Optional<Long> recordIfConsented(
        String token,
        AssistantChatRequest request,
        AssistantChatResponse response,
        WeatherSnapshot weather
    ) {
        UUID consentId = PrivacyConsentService.parseToken(token)
            .filter(consentRepository::existsById)
            .orElse(null);
        if (consentId == null) {
            return Optional.empty();
        }

        LocationCandidate location = request.location();
        String countryCode = location == null ? null : location.countryCode();
        String region = location == null ? null : selectedRegion(location);
        boolean riskFlag = weather != null
            && (weather.forecastRainProbabilityPercent() >= 70 || weather.precipitationMm() >= 5);
        AssistantChatLog saved = chatLogRepository.save(new AssistantChatLog(
            consentId,
            Instant.now(),
            countryCode,
            PrivacyTextRedactor.limit(PrivacyTextRedactor.redact(region), 120),
            PrivacyTextRedactor.limit(PrivacyTextRedactor.redact(request.crop()), 120),
            PrivacyTextRedactor.limit(PrivacyTextRedactor.redact(request.growthStage()), 80),
            PrivacyTextRedactor.limit(PrivacyTextRedactor.redact(request.soilType()), 100),
            serializeWeather(weather),
            riskFlag,
            PrivacyTextRedactor.limit(PrivacyTextRedactor.redact(request.question()), 2000),
            PrivacyTextRedactor.limit(PrivacyTextRedactor.redact(response.answer()), 8000),
            PrivacyTextRedactor.limit(
                PrivacyTextRedactor.redact(String.join("\n", response.sources())),
                6000
            )
        ));
        return Optional.of(saved.getId());
    }

    @Transactional
    public void updateFeedback(String token, Long logId, AdvisoryFeedback feedback) {
        AssistantChatLog log = ownedLog(token, logId);
        log.setFeedback(feedback);
        chatLogRepository.save(log);
    }

    @Transactional
    public void updateOutcome(String token, Long logId, ApplicationOutcome outcome) {
        AssistantChatLog log = ownedLog(token, logId);
        log.setApplicationOutcome(outcome);
        chatLogRepository.save(log);
    }

    private AssistantChatLog ownedLog(String token, Long logId) {
        UUID consentId = PrivacyConsentService.parseToken(token)
            .filter(consentRepository::existsById)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat record not found."));
        return chatLogRepository.findByIdAndConsentId(logId, consentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat record not found."));
    }

    private String serializeWeather(WeatherSnapshot weather) {
        if (weather == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(weather);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode the chat weather snapshot.", exception);
        }
    }

    private static String selectedRegion(LocationCandidate location) {
        if (location.admin1() != null && !location.admin1().isBlank()) {
            return location.admin1();
        }
        return location.admin2();
    }
}
