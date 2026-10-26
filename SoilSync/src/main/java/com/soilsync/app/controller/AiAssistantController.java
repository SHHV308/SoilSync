package com.soilsync.app.controller;

import com.soilsync.app.model.CropAdviceRequest;
import com.soilsync.app.model.CropAdviceResponse;
import com.soilsync.app.service.RegionalCropAdviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiAssistantController {

    private final RegionalCropAdviceService regionalCropAdviceService;

    public AiAssistantController(RegionalCropAdviceService regionalCropAdviceService) {
        this.regionalCropAdviceService = regionalCropAdviceService;
    }

    @PostMapping("/api/ai/advice")
    public CropAdviceResponse advise(@Valid @RequestBody CropAdviceRequest request) {
        return regionalCropAdviceService.advise(request);
    }
}
