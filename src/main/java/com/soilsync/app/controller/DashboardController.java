package com.soilsync.app.controller;

import com.soilsync.app.repository.PlantRepository;
import com.soilsync.app.repository.ReminderRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DashboardController {

    private final PlantRepository plantRepository;
    private final ReminderRepository reminderRepository;

    public DashboardController(PlantRepository plantRepository, ReminderRepository reminderRepository) {
        this.plantRepository = plantRepository;
        this.reminderRepository = reminderRepository;
    }

    @GetMapping("/api/dashboard")
    public Map<String, Object> dashboard() {
        return Map.of(
            "plantCount", plantRepository.count(),
            "activeReminderCount", reminderRepository.count(),
            "summary", "Demo season summary for gardens and field plots"
        );
    }
}
