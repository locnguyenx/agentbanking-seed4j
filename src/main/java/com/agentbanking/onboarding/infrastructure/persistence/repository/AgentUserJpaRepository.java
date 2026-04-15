package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.infrastructure.persistence.entity.AgentUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AgentUserJpaRepository extends JpaRepository<AgentUserEntity, String> {

  Optional<AgentUserEntity> findByUsername(String username);
  
  List<AgentUserEntity> findByAgentId(String agentId);
}
