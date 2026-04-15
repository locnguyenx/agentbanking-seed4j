package com.agentbanking.transaction.infrastructure.persistence.repository;

import com.agentbanking.transaction.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, String> {

  List<TransactionEntity> findByAgentIdOrderByInitiatedAtDesc(String agentId);
  
  Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
  
  List<TransactionEntity> findBySagaExecutionId(String sagaExecutionId);
}
