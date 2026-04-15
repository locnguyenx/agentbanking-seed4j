package com.agentbanking.orchestrator.domain.model;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionType;

public record TransactionRequest(
  TransactionType type,
  AgentId agentId,
  Money amount,
  String customerAccountId,
  String idempotencyKey
) {}
