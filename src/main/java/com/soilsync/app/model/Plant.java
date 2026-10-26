package com.soilsync.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "plants")
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String variety;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String soilType;

    @Column(length = 1200)
    private String notes;

    @Column(nullable = false)
    private String healthStatus;

    @Column(nullable = false)
    private LocalDateTime lastWateredAt;

    @Column(nullable = false)
    private LocalDateTime nextWateringAt;

    public Plant() {
    }

    public Plant(String name, String variety, String location, String soilType, String notes, String healthStatus,
                 LocalDateTime lastWateredAt, LocalDateTime nextWateringAt) {
        this.name = name;
        this.variety = variety;
        this.location = location;
        this.soilType = soilType;
        this.notes = notes;
        this.healthStatus = healthStatus;
        this.lastWateredAt = lastWateredAt;
        this.nextWateringAt = nextWateringAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSoilType() {
        return soilType;
    }

    public void setSoilType(String soilType) {
        this.soilType = soilType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public LocalDateTime getLastWateredAt() {
        return lastWateredAt;
    }

    public void setLastWateredAt(LocalDateTime lastWateredAt) {
        this.lastWateredAt = lastWateredAt;
    }

    public LocalDateTime getNextWateringAt() {
        return nextWateringAt;
    }

    public void setNextWateringAt(LocalDateTime nextWateringAt) {
        this.nextWateringAt = nextWateringAt;
    }
}
