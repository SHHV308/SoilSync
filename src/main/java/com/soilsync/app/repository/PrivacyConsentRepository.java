package com.soilsync.app.repository;

import com.soilsync.app.model.PrivacyConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrivacyConsentRepository extends JpaRepository<PrivacyConsent, UUID> {
}
