package com.agentbanking.commission.infrastructure.persistence.repository;

import com.agentbanking.commission.infrastructure.persistence.entity.CommissionEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CommissionEntryJpaRepository extends JpaRepository<CommissionEntryEntity, UUID> {

  List<CommissionEntryEntity> findByAgentIdAndStatus(String agentId, String status);
  
  List<CommissionEntryEntity> findBySettledAtIsNull();
}
