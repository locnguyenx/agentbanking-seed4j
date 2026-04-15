package com.agentbanking.settlement.infrastructure.persistence.repository;

import com.agentbanking.settlement.infrastructure.persistence.entity.ReconciliationRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReconciliationRecordJpaRepository extends JpaRepository<ReconciliationRecordEntity, UUID> {

  List<ReconciliationRecordEntity> findByStatus(String status);
}
