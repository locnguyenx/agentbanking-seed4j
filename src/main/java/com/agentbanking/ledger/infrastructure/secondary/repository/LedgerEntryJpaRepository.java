package com.agentbanking.ledger.infrastructure.secondary.repository;

import com.agentbanking.ledger.infrastructure.secondary.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, UUID> {

  List<LedgerEntryEntity> findByTransactionId(String transactionId);
}
