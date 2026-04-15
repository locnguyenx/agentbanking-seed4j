package com.agentbanking.orchestrator.workflow.dto;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;

public record CashWithdrawalWorkflowInput(
  String sagaId,
  AgentId agentId,
  Money amount,
  String customerAccountId,
  String customerPhone,
  String idempotencyKey
) {}
