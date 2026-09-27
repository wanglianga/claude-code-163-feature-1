package com.city.manhole.repo;

import com.city.manhole.model.InspectionRoute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionRouteRepo extends JpaRepository<InspectionRoute, Long> {
}
