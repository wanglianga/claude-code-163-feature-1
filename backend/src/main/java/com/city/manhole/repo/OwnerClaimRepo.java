package com.city.manhole.repo;

import com.city.manhole.model.OwnerClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OwnerClaimRepo extends JpaRepository<OwnerClaim, Long> {
    List<OwnerClaim> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
