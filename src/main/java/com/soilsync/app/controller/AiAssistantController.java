package com.soilsync.app.controller;

import com.soilsync.app.model.CropAdviceRequest;
import com.soilsync.app.model.CropAdviceResponse;
import com.soilsync.app.model.LocationCandidate;
import com.soilsync.app.model.LocationSearchRequest;
import com.soilsync.app.service.RegionalCropAdviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class AiAssistantController {

    private final RegionalCropAdviceService regionalCropAdviceService;

    public AiAssistantController(RegionalCropAdviceService regionalCropAdviceService) {
        this.regionalCropAdviceService = regionalCropAdviceService;
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
    public CropAdviceResponse advise(@Valid @RequestBody CropAdviceRequest request) {
        return regionalCropAdviceService.advise(request);
    }
}
