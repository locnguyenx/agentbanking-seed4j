package com.agentbanking.orchestrator.application.dto;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionType;

public record TransactionRequestDTO(
  TransactionType type,
  AgentId agentId,
  Money amount,
  String customerAccountId,
  String idempotencyKey
) {
  public com.agentbanking.orchestrator.domain.model.TransactionRequest toDomain() {
    return new com.agentbanking.orchestrator.domain.model.TransactionRequest(type, agentId, amount, customerAccountId, idempotencyKey);
  }
}
