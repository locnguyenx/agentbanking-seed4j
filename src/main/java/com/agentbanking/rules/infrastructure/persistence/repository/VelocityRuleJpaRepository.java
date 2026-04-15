package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.infrastructure.persistence.entity.VelocityRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VelocityRuleJpaRepository extends JpaRepository<VelocityRuleEntity, UUID> {

  List<VelocityRuleEntity> findByScopeAndIsActiveTrue(String scope);
}
