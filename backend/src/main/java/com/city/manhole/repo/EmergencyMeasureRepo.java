package com.city.manhole.repo;

import com.city.manhole.model.EmergencyMeasure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmergencyMeasureRepo extends JpaRepository<EmergencyMeasure, Long> {
    Optional<EmergencyMeasure> findByIncidentId(Long incidentId);
}
