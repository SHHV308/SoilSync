package com.soilsync.app.repository;

import com.soilsync.app.model.AdvisoryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdvisoryLogRepository extends JpaRepository<AdvisoryLog, Long> {

    Optional<AdvisoryLog> findByIdAndConsentId(Long id, UUID consentId);

    List<AdvisoryLog> findAllByConsentId(UUID consentId);

    long deleteAllByConsentId(UUID consentId);
}
