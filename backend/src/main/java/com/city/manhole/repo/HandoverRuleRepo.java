package com.city.manhole.repo;

import com.city.manhole.model.HandoverRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoverRuleRepo extends JpaRepository<HandoverRule, Long> {
}
