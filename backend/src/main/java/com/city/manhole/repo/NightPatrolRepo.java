package com.city.manhole.repo;

import com.city.manhole.model.NightPatrol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NightPatrolRepo extends JpaRepository<NightPatrol, Long> {
    List<NightPatrol> findByIncidentIdOrderByArrivedAtAsc(Long incidentId);
    Optional<NightPatrol> findFirstByIncidentIdAndArrivedAtLessThanEqualOrderByArrivedAtDesc(
            Long incidentId, java.time.LocalDateTime arrivedAt);
}
