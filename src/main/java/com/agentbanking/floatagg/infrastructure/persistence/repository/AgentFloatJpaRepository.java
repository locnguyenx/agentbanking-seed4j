package com.agentbanking.floatagg.infrastructure.persistence.repository;

import com.agentbanking.floatagg.infrastructure.persistence.entity.AgentFloatEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface AgentFloatJpaRepository extends JpaRepository<AgentFloatEntity, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<AgentFloatEntity> findByAgentId(String agentId);
}
