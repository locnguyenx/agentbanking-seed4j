package com.agentbanking.commission.domain.model;

import com.agentbanking.shared.money.domain.Money;
import java.time.Instant;
import java.util.UUID;

public record CommissionEntry(
  UUID id,
  String transactionId,
  String agentId,
  CommissionType type,
  Money transactionAmount,
  Money commissionAmount,
  CommissionStatus status,
  Instant createdAt,
  Instant settledAt
) {}
