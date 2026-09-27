package com.city.manhole.repo;

import com.city.manhole.model.CostAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CostAllocationRepo extends JpaRepository<CostAllocation, Long> {
    Optional<CostAllocation> findByIncidentId(Long incidentId);
    List<CostAllocation> findAllByOrderByCreatedAtDesc();
}
