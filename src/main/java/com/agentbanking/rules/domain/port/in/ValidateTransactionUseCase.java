package com.agentbanking.rules.domain.port.in;

import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.onboarding.domain.AgentType;
import com.agentbanking.rules.domain.model.FeeCalculationResult;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionType;
import java.util.List;

public interface ValidateTransactionUseCase {

  ValidationResult validate(TransactionValidationRequest request);

  record TransactionValidationRequest(
    TransactionType type,
    AgentId agentId,
    AgentType agentTier,
    Money amount,
    String mykadNumber
  ) {}

  record ValidationResult(
    boolean valid,
    List<String> errors,
    FeeCalculationResult feeCalculation
  ) {}
}
