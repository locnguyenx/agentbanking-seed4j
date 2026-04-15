package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.infrastructure.persistence.entity.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, String> {

  Optional<AgentEntity> findByBusinessRegistrationNumber(String businessRegistrationNumber);
  
  List<AgentEntity> findByStatus(String status);

  Optional<AgentEntity> findByMykadNumber(String mykadNumber);

  boolean existsByMykadNumber(String mykadNumber);
}
