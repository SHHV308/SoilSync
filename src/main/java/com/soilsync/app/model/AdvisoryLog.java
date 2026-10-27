package com.soilsync.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "advisory_logs")
public class AdvisoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consent_id", nullable = false)
    private UUID consentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(length = 120)
    private String region;

    @Column(nullable = false, length = 120)
    private String crop;

    @Column(name = "growth_stage", nullable = false, length = 80)
    private String growthStage;

    @Column(name = "soil_type", nullable = false, length = 100)
    private String soilType;

    @Column(name = "weather_snapshot", nullable = false, length = 2000)
    private String weatherSnapshot;

    @Column(name = "risk_flag", nullable = false)
    private boolean riskFlag;

    @Column(length = 2000)
    private String question;

    @Column(nullable = false, length = 8000)
    private String answer;

    @Column(nullable = false, length = 6000)
    private String sources;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AdvisoryFeedback feedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_outcome", nullable = false, length = 30)
    private ApplicationOutcome applicationOutcome = ApplicationOutcome.UNKNOWN;

    protected AdvisoryLog() {
    }

    public AdvisoryLog(
        UUID consentId,
        Instant createdAt,
        String countryCode,
        String region,
        String crop,
        String growthStage,
        String soilType,
        String weatherSnapshot,
        boolean riskFlag,
        String question,
        String answer,
        String sources
    ) {
        this.consentId = consentId;
        this.createdAt = createdAt;
        this.countryCode = countryCode;
        this.region = region;
        this.crop = crop;
        this.growthStage = growthStage;
        this.soilType = soilType;
        this.weatherSnapshot = weatherSnapshot;
        this.riskFlag = riskFlag;
        this.question = question;
        this.answer = answer;
        this.sources = sources;
    }

    public Long getId() {
        return id;
    }

    public UUID getConsentId() {
        return consentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getRegion() {
        return region;
    }

    public String getCrop() {
        return crop;
    }

    public String getGrowthStage() {
        return growthStage;
    }

    public String getSoilType() {
        return soilType;
    }

    public String getWeatherSnapshot() {
        return weatherSnapshot;
    }

    public boolean isRiskFlag() {
        return riskFlag;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getSources() {
        return sources;
    }

    public AdvisoryFeedback getFeedback() {
        return feedback;
    }

    public void setFeedback(AdvisoryFeedback feedback) {
        this.feedback = feedback;
    }

    public ApplicationOutcome getApplicationOutcome() {
        return applicationOutcome;
    }

    public void setApplicationOutcome(ApplicationOutcome applicationOutcome) {
        this.applicationOutcome = applicationOutcome;
    }
}
