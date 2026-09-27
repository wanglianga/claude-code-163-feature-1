package com.city.manhole.repo;

import com.city.manhole.model.Acceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcceptanceRepo extends JpaRepository<Acceptance, Long> {
    List<Acceptance> findByIncidentIdOrderByInspectedAtDesc(Long incidentId);
}
