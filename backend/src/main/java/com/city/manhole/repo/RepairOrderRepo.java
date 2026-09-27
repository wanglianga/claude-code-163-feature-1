package com.city.manhole.repo;

import com.city.manhole.model.RepairOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepairOrderRepo extends JpaRepository<RepairOrder, Long> {
    Optional<RepairOrder> findByIncidentId(Long incidentId);
    List<RepairOrder> findByTeamIdOrderByAssignedAtDesc(Long teamId);
    List<RepairOrder> findByOwnerIdOrderByAssignedAtDesc(Long ownerId);
}
