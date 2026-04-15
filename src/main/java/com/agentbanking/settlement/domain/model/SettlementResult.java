package com.agentbanking.settlement.domain.model;

import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;
import java.util.UUID;

public record SettlementResult(
  UUID settlementId,
  SettlementStatus status,
  Money totalAmount,
  Money totalFee,
  Money totalCommission,
  int transactionCount,
  Instant processedAt
) {}
