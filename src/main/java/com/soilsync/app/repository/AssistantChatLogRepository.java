package com.soilsync.app.repository;

import com.soilsync.app.model.AssistantChatLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssistantChatLogRepository extends JpaRepository<AssistantChatLog, Long> {

    Optional<AssistantChatLog> findByIdAndConsentId(Long id, UUID consentId);

    long deleteAllByConsentId(UUID consentId);
}
