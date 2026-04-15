package com.agentbanking.orchestrator.domain.model;

import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionStatus;
import java.time.Instant;

public record TransactionResponse(
  SagaExecutionId sagaId,
  TransactionStatus status,
  Money amount,
  String errorCode,
  String errorMessage,
  Instant initiatedAt,
  Instant completedAt
) {}
