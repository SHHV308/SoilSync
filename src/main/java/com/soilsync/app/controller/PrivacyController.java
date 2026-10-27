package com.soilsync.app.controller;

import com.soilsync.app.model.AdvisoryFeedbackRequest;
import com.soilsync.app.model.ApplicationOutcomeRequest;
import com.soilsync.app.model.ConsentRequest;
import com.soilsync.app.service.AdvisoryLogService;
import com.soilsync.app.service.PrivacyConsentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/privacy")
public class PrivacyController {

    private final PrivacyConsentService privacyConsentService;
    private final AdvisoryLogService advisoryLogService;

    public PrivacyController(
        PrivacyConsentService privacyConsentService,
        AdvisoryLogService advisoryLogService
    ) {
        this.privacyConsentService = privacyConsentService;
        this.advisoryLogService = advisoryLogService;
    }

    @GetMapping("/consent")
    public ResponseEntity<PrivacyConsentService.ConsentStatus> consentStatus(
        @CookieValue(name = PrivacyConsentService.COOKIE_NAME, required = false) String token
    ) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(privacyConsentService.status(token));
    }

    @PostMapping("/consent")
    public ResponseEntity<PrivacyConsentService.ConsentStatus> grantConsent(
        @Valid @RequestBody ConsentRequest request,
        @CookieValue(name = PrivacyConsentService.COOKIE_NAME, required = false) String token,
        HttpServletRequest servletRequest
    ) {
        PrivacyConsentService.ConsentGrant grant = privacyConsentService.grantConsent(token);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header("Set-Cookie", consentCookie(
                grant.token(),
                servletRequest.isSecure(),
                Duration.ofDays(365)
            ))
            .body(grant.status());
    }

    @DeleteMapping("/data")
    public ResponseEntity<Void> deleteData(
        @CookieValue(name = PrivacyConsentService.COOKIE_NAME, required = false) String token,
        HttpServletRequest servletRequest
    ) {
        privacyConsentService.deleteData(token);
        return ResponseEntity.noContent()
            .cacheControl(CacheControl.noStore())
            .header("Set-Cookie", ResponseCookie.from(PrivacyConsentService.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(servletRequest.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build()
                .toString())
            .build();
    }

    @PutMapping("/advisories/{id}/feedback")
    public ResponseEntity<Void> updateFeedback(
        @PathVariable Long id,
        @Valid @RequestBody AdvisoryFeedbackRequest request,
        @CookieValue(name = PrivacyConsentService.COOKIE_NAME, required = false) String token
    ) {
        advisoryLogService.updateFeedback(token, id, request.feedback());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PutMapping("/advisories/{id}/outcome")
    public ResponseEntity<Void> updateOutcome(
        @PathVariable Long id,
        @Valid @RequestBody ApplicationOutcomeRequest request,
        @CookieValue(name = PrivacyConsentService.COOKIE_NAME, required = false) String token
    ) {
        advisoryLogService.updateOutcome(token, id, request.outcome());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private static String consentCookie(
        String token,
        boolean secure,
        Duration maxAge
    ) {
        return ResponseCookie.from(PrivacyConsentService.COOKIE_NAME, token)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(maxAge)
            .build()
            .toString();
    }
}
