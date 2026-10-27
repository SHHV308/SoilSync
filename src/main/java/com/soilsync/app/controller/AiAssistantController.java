package com.soilsync.app.controller;

import com.soilsync.app.model.CropAdviceRequest;
import com.soilsync.app.model.CropAdviceResponse;
import com.soilsync.app.model.LocationCandidate;
import com.soilsync.app.model.LocationSearchRequest;
import com.soilsync.app.service.AdvisoryLogService;
import com.soilsync.app.service.PrivacyConsentService;
import com.soilsync.app.service.RegionalCropAdviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
public class AiAssistantController {

    private final RegionalCropAdviceService regionalCropAdviceService;
    private final AdvisoryLogService advisoryLogService;

    public AiAssistantController(
        RegionalCropAdviceService regionalCropAdviceService,
        AdvisoryLogService advisoryLogService
    ) {
        this.regionalCropAdviceService = regionalCropAdviceService;
        this.advisoryLogService = advisoryLogService;
    }

    @GetMapping("/api/ai/locations")
    public List<LocationCandidate> searchLocations(@RequestParam String query) {
        return regionalCropAdviceService.searchLocations(query);
    }

    @PostMapping("/api/ai/locations")
    public List<LocationCandidate> provideCoordinates(@RequestBody LocationSearchRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A location search request is required.");
        }
        if (request.query() != null && !request.query().isBlank()) {
            if (request.latitude() != null || request.longitude() != null
                || request.region() != null || request.country() != null || request.countryCode() != null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide either a place-name query or coordinates with country and region details."
                );
            }
            return regionalCropAdviceService.searchLocations(request.query());
        }

        return List.of(regionalCropAdviceService.coordinateLocation(
            request.latitude(),
            request.longitude(),
            request.region(),
            request.country(),
            request.countryCode()
        ));
    }

    @PostMapping("/api/ai/advice")
    public CropAdviceResponse advise(
        @Valid @RequestBody CropAdviceRequest request,
        @CookieValue(name = PrivacyConsentService.COOKIE_NAME, required = false) String consentToken
    ) {
        CropAdviceResponse response = regionalCropAdviceService.advise(request);
        Optional<Long> advisoryLogId = advisoryLogService.recordIfConsented(consentToken, request, response);
        return advisoryLogId.map(response::withAdvisoryLogId).orElse(response);
    }
}
