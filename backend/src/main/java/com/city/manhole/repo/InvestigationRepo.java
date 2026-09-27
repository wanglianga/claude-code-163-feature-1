package com.city.manhole.repo;

import com.city.manhole.model.OwnershipInvestigation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestigationRepo extends JpaRepository<OwnershipInvestigation, Long> {
    List<OwnershipInvestigation> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
