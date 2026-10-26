package com.soilsync.app.controller;

import com.soilsync.app.model.Plant;
import com.soilsync.app.repository.PlantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PlantController {

    private final PlantRepository plantRepository;

    public PlantController(PlantRepository plantRepository) {
        this.plantRepository = plantRepository;
    }

    @GetMapping("/api/plants")
    public List<Plant> getPlants() {
        return plantRepository.findAll();
    }

    @PostMapping("/api/plants")
    public ResponseEntity<Plant> createPlant(@RequestBody Plant plant) {
        Plant saved = plantRepository.save(plant);
        return ResponseEntity.ok(saved);
    }
}
