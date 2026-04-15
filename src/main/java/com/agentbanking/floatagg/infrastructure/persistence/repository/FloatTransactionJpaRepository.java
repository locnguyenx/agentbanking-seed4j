package com.agentbanking.floatagg.infrastructure.persistence.repository;

import com.agentbanking.floatagg.infrastructure.persistence.entity.FloatTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FloatTransactionJpaRepository extends JpaRepository<FloatTransactionEntity, UUID> {

  List<FloatTransactionEntity> findByAgentIdOrderByCreatedAtDesc(String agentId);
}
