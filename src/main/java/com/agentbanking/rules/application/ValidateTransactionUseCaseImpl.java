package com.agentbanking.rules.application;

import com.agentbanking.rules.domain.port.in.ValidateTransactionUseCase;
import com.agentbanking.rules.domain.port.in.ValidateTransactionUseCase.ValidationResult;
import com.agentbanking.rules.domain.service.RulesService;

public class ValidateTransactionUseCaseImpl implements ValidateTransactionUseCase {

  private final RulesService rulesService;

  public ValidateTransactionUseCaseImpl(RulesService rulesService) {
    this.rulesService = rulesService;
  }

  @Override
  public ValidationResult validate(TransactionValidationRequest request) {
    return rulesService.validate(request);
  }
}
