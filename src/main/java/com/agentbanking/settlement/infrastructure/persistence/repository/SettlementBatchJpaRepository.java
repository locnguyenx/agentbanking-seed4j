package com.agentbanking.settlement.infrastructure.persistence.repository;

import com.agentbanking.settlement.infrastructure.persistence.entity.SettlementBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SettlementBatchJpaRepository extends JpaRepository<SettlementBatchEntity, UUID> {

  List<SettlementBatchEntity> findByBatchDateBetween(Instant start, Instant end);
}
