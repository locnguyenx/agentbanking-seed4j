package com.agentbanking.orchestrator.workflow.dto;

import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionStatus;

public record CashWithdrawalWorkflowResult(
  String sagaId,
  TransactionStatus status,
  Money amount,
  String errorCode,
  String errorMessage,
  String stan
) {}
