package com.agentbanking.commission.infrastructure.persistence.repository;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.model.CommissionType;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import com.agentbanking.commission.infrastructure.persistence.entity.CommissionEntryEntity;
import com.agentbanking.shared.money.domain.Money;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class CommissionRepositoryImpl implements CommissionRepository {

  private final CommissionEntryJpaRepository jpaRepository;

  public CommissionRepositoryImpl(CommissionEntryJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public CommissionEntry save(CommissionEntry entry) {
    CommissionEntryEntity entity = toEntity(entry);
    CommissionEntryEntity saved = jpaRepository.save(entity);
    return toDomain(saved);
  }

  @Override
  public List<CommissionEntry> findByAgentIdAndStatus(String agentId, CommissionStatus status) {
    return jpaRepository.findByAgentIdAndStatus(agentId, status.name())
      .stream()
      .map(this::toDomain)
      .toList();
  }

  @Override
  public List<CommissionEntry> findBySettledAtIsNull() {
    return jpaRepository.findBySettledAtIsNull()
      .stream()
      .map(this::toDomain)
      .toList();
  }

  private CommissionEntry toDomain(CommissionEntryEntity entity) {
    return new CommissionEntry(
      entity.getId(),
      entity.getTransactionId(),
      entity.getAgentId(),
      CommissionType.valueOf(entity.getType()),
      Money.of(entity.getTransactionAmount()),
      Money.of(entity.getCommissionAmount()),
      CommissionStatus.valueOf(entity.getStatus()),
      entity.getCreatedAt(),
      entity.getSettledAt()
    );
  }

  private CommissionEntryEntity toEntity(CommissionEntry entry) {
    CommissionEntryEntity entity = new CommissionEntryEntity();
    entity.setId(entry.id());
    entity.setTransactionId(entry.transactionId());
    entity.setAgentId(entry.agentId());
    entity.setType(entry.type().name());
    entity.setTransactionAmount(entry.transactionAmount().amount());
    entity.setCommissionAmount(entry.commissionAmount().amount());
    entity.setStatus(entry.status().name());
    entity.setCreatedAt(entry.createdAt());
    entity.setSettledAt(entry.settledAt());
    return entity;
  }
}
