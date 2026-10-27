package com.soilsync.app;

import com.soilsync.app.model.AdvisoryFeedback;
import com.soilsync.app.model.AdvisoryLog;
import com.soilsync.app.model.ApplicationOutcome;
import com.soilsync.app.model.CropAdviceRequest;
import com.soilsync.app.model.CropAdviceResponse;
import com.soilsync.app.model.LocationCandidate;
import com.soilsync.app.model.WeatherSnapshot;
import com.soilsync.app.repository.AdvisoryLogRepository;
import com.soilsync.app.repository.PrivacyConsentRepository;
import com.soilsync.app.service.AdvisoryLogService;
import com.soilsync.app.service.PrivacyConsentService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PrivacyLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrivacyConsentService privacyConsentService;

    @Autowired
    private AdvisoryLogService advisoryLogService;

    @Autowired
    private AdvisoryLogRepository advisoryLogRepository;

    @Autowired
    private PrivacyConsentRepository privacyConsentRepository;

    @Test
    void consentIsRejectedWithoutExplicitAcceptance() throws Exception {
        mockMvc.perform(post("/api/privacy/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepted\":false}"))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("false"))));

        assertThat(privacyConsentRepository.count()).isZero();
    }

    @Test
    void consentCookieEnablesOptInAndCanDeleteThisBrowsersData() throws Exception {
        String cookieHeader = mockMvc.perform(post("/api/privacy/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepted\":true}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Set-Cookie",
                org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsString("HttpOnly"),
                    org.hamcrest.Matchers.containsString("SameSite=Lax")
                )))
            .andReturn()
            .getResponse()
            .getHeader("Set-Cookie");
        String cookieValue = cookieHeader.split(";", 2)[0].split("=", 2)[1];
        Cookie browserCookie = new Cookie(PrivacyConsentService.COOKIE_NAME, cookieValue);

        mockMvc.perform(get("/api/privacy/consent").cookie(browserCookie))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                "$.consented"
            ).value(true));

        mockMvc.perform(delete("/api/privacy/data").cookie(browserCookie))
            .andExpect(status().isNoContent())
            .andExpect(header().string("Set-Cookie",
                org.hamcrest.Matchers.containsString("Max-Age=0")));

        assertThat(privacyConsentRepository.count()).isZero();
    }

    @Test
    void logsOnlyWithConsentRedactsTextAndRestrictsFeedbackAndDeletionToThatBrowser() {
        PrivacyConsentService.ConsentGrant firstBrowser = privacyConsentService.grantConsent(null);
        PrivacyConsentService.ConsentGrant secondBrowser = privacyConsentService.grantConsent(null);
        LocationCandidate selectedLocation = new LocationCandidate(
            "selected-place", "Springfield", "Illinois", "", "United States", "US",
            39.8, -89.6, "America/Chicago", "UNCLASSIFIED", "Open-Meteo geocoding"
        );
        CropAdviceRequest request = new CropAdviceRequest(
            "Springfield, Illinois",
            "Maize",
            "Flowering",
            "Loam",
            "Contact me at farmer@example.com or +1 (555) 123-4567. password: hidden-value",
            selectedLocation
        );
        CropAdviceResponse response = new CropAdviceResponse(
            "Springfield, Illinois, United States",
            "Maize",
            "Flowering",
            "Use a recent soil test; contact farmer@example.com for more details.",
            List.of("Check the local forecast."),
            new WeatherSnapshot(21.0, 60, 0, 80, "Forecast has rain risk.", "Open-Meteo"),
            List.of("Open-Meteo"),
            Instant.now(),
            selectedLocation
        );

        advisoryLogService.recordIfConsented(null, request, response);
        assertThat(advisoryLogRepository.findAllByConsentId(
            java.util.UUID.fromString(firstBrowser.token())
        )).isEmpty();

        advisoryLogService.recordIfConsented(firstBrowser.token(), request, response);
        AdvisoryLog log = advisoryLogRepository.findAllByConsentId(
            java.util.UUID.fromString(firstBrowser.token())
        ).get(0);

        assertThat(log.getCountryCode()).isEqualTo("US");
        assertThat(log.getRegion()).isEqualTo("Illinois");
        assertThat(log.isRiskFlag()).isTrue();
        assertThat(log.getQuestion()).contains("[REDACTED_EMAIL]", "[REDACTED_PHONE]", "[REDACTED_SECRET]")
            .doesNotContain("farmer@example.com", "555", "hidden-value");
        assertThat(log.getAnswer()).contains("[REDACTED_EMAIL]").doesNotContain("farmer@example.com");
        assertThat(log.getWeatherSnapshot()).doesNotContain("39.8", "-89.6");

        assertThatThrownBy(() -> advisoryLogService.updateFeedback(
            secondBrowser.token(), log.getId(), AdvisoryFeedback.HELPFUL
        )).hasMessageContaining("Advisory record not found");

        advisoryLogService.updateFeedback(
            firstBrowser.token(), log.getId(), AdvisoryFeedback.HELPFUL
        );
        advisoryLogService.updateOutcome(
            firstBrowser.token(), log.getId(), ApplicationOutcome.DEFERRED_APPLICATION
        );
        AdvisoryLog updated = advisoryLogRepository.findById(log.getId()).orElseThrow();
        assertThat(updated.getFeedback()).isEqualTo(AdvisoryFeedback.HELPFUL);
        assertThat(updated.getApplicationOutcome()).isEqualTo(ApplicationOutcome.DEFERRED_APPLICATION);

        privacyConsentService.deleteData(firstBrowser.token());
        assertThat(advisoryLogRepository.findAllByConsentId(
            java.util.UUID.fromString(firstBrowser.token())
        )).isEmpty();
        assertThat(privacyConsentRepository.existsById(java.util.UUID.fromString(secondBrowser.token()))).isTrue();
        privacyConsentService.deleteData(secondBrowser.token());
    }
}
