package com.city.manhole.repo;

import com.city.manhole.model.NightFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NightFailureRepo extends JpaRepository<NightFailure, Long> {
    List<NightFailure> findByIncidentIdOrderByConeMovedAtAsc(Long incidentId);
    List<NightFailure> findAllByOrderByCreatedAtDesc();
    long countByCodeStartingWith(String prefix);
}
