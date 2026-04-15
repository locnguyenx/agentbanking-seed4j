package com.agentbanking.orchestrator.infrastructure.external.dto;

import com.agentbanking.shared.money.domain.Money;

public record CommissionEntryDTO(
  String id,
  String transactionId,
  String agentId,
  String type,
  Money transactionAmount,
  Money commissionAmount,
  String status,
  String createdAt,
  String settledAt
) {}
