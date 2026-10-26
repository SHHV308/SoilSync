package com.soilsync.app.repository;

import com.soilsync.app.model.CareEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareEventRepository extends JpaRepository<CareEvent, Long> {
}
