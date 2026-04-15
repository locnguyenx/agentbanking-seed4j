package com.agentbanking.commission.domain.model;

import com.agentbanking.shared.money.domain.Money;

import java.time.Instant;

public record CommissionEvent(
  String eventId,
  String transactionId,
  String agentId,
  CommissionType type,
  Money transactionAmount,
  Money commissionAmount,
  Instant createdAt
) {}
