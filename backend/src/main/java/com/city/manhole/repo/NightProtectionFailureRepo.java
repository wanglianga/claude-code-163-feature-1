package com.city.manhole.repo;

import com.city.manhole.model.NightProtectionFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NightProtectionFailureRepo extends JpaRepository<NightProtectionFailure, Long> {
    Optional<NightProtectionFailure> findByIncidentId(Long incidentId);
    List<NightProtectionFailure> findAllByOrderByCreatedAtDesc();
}
