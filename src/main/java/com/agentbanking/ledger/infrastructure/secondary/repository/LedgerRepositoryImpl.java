package com.agentbanking.ledger.infrastructure.secondary.repository;

import com.agentbanking.ledger.domain.model.AccountType;
import com.agentbanking.ledger.domain.model.EntryType;
import com.agentbanking.ledger.domain.model.LedgerEntry;
import com.agentbanking.ledger.domain.port.out.LedgerRepository;
import com.agentbanking.ledger.infrastructure.secondary.entity.LedgerEntryEntity;
import com.agentbanking.shared.money.domain.Money;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class LedgerRepositoryImpl implements LedgerRepository {

  private final LedgerEntryJpaRepository jpaRepository;

  public LedgerRepositoryImpl(LedgerEntryJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public UUID createEntry(LedgerEntry entry) {
    LedgerEntryEntity entity = toEntity(entry);
    LedgerEntryEntity saved = jpaRepository.save(entity);
    return saved.getId();
  }

  @Override
  public List<LedgerEntry> findByTransactionId(String transactionId) {
    return jpaRepository.findByTransactionId(transactionId)
      .stream()
      .map(this::toDomain)
      .toList();
  }

  private LedgerEntry toDomain(LedgerEntryEntity entity) {
    return new LedgerEntry(
      entity.getId(),
      entity.getTransactionId(),
      entity.getAccountCode(),
      AccountType.valueOf(entity.getAccountType()),
      EntryType.valueOf(entity.getEntryType()),
      Money.of(entity.getAmount()),
      entity.getDescription(),
      entity.getCreatedAt()
    );
  }

  private LedgerEntryEntity toEntity(LedgerEntry entry) {
    LedgerEntryEntity entity = new LedgerEntryEntity();
    entity.setId(entry.id());
    entity.setTransactionId(entry.transactionId());
    entity.setAccountCode(entry.accountCode());
    entity.setAccountType(entry.accountType().name());
    entity.setEntryType(entry.entryType().name());
    entity.setAmount(entry.amount().amount());
    entity.setDescription(entry.description());
    entity.setCreatedAt(entry.createdAt());
    return entity;
  }
}
