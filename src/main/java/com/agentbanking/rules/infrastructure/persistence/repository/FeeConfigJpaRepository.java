package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.infrastructure.persistence.entity.FeeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeeConfigJpaRepository extends JpaRepository<FeeConfigEntity, UUID> {

  List<FeeConfigEntity> findByTransactionTypeAndAgentTier(String transactionType, String agentTier);
}
