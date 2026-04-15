package com.agentbanking.ledger.domain.model;

import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(
  UUID id,
  String transactionId,
  String accountCode,
  AccountType accountType,
  EntryType entryType,
  Money amount,
  String description,
  Instant createdAt
) {}