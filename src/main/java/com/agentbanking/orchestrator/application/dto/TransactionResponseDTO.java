package com.agentbanking.orchestrator.application.dto;

import com.agentbanking.orchestrator.domain.model.SagaExecutionId;
import com.agentbanking.orchestrator.domain.model.TransactionResponse;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionStatus;
import java.time.Instant;

public record TransactionResponseDTO(
  SagaExecutionId sagaId,
  TransactionStatus status,
  Money amount,
  String errorCode,
  String errorMessage,
  Instant initiatedAt,
  Instant completedAt
) {
  public static TransactionResponseDTO fromDomain(TransactionResponse domain) {
    return new TransactionResponseDTO(
      domain.sagaId(),
      domain.status(),
      domain.amount(),
      domain.errorCode(),
      domain.errorMessage(),
      domain.initiatedAt(),
      domain.completedAt()
    );
  }
}
