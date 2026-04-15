package com.agentbanking.settlement.domain.model;

import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;
import java.util.UUID;

public record SettlementEntry(
  UUID id,
  String transactionId,
  String agentId,
  Money amount,
  Money fee,
  Money commission,
  String type,
  Instant transactionDate
) {}