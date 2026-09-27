package com.city.manhole.repo;

import com.city.manhole.model.CompensationCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompensationRepo extends JpaRepository<CompensationCase, Long> {
    Optional<CompensationCase> findByIncidentId(Long incidentId);
    List<CompensationCase> findAllByOrderByCreatedAtDesc();
}
