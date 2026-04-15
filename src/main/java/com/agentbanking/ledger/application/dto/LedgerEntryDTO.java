package com.agentbanking.ledger.application.dto;

import com.agentbanking.ledger.domain.model.AccountType;
import com.agentbanking.ledger.domain.model.EntryType;
import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryDTO(
  UUID id,
  String transactionId,
  String accountCode,
  AccountType accountType,
  EntryType entryType,
  Money amount,
  String description,
  Instant createdAt
) {
  public static LedgerEntryDTO fromDomain(com.agentbanking.ledger.domain.model.LedgerEntry domain) {
    return new LedgerEntryDTO(
      domain.id(),
      domain.transactionId(),
      domain.accountCode(),
      domain.accountType(),
      domain.entryType(),
      domain.amount(),
      domain.description(),
      domain.createdAt()
    );
  }
}