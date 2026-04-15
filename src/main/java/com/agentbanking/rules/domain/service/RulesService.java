package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.FeeCalculationResult;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.VelocityRule;
import com.agentbanking.rules.domain.port.in.ValidateTransactionUseCase;
import com.agentbanking.rules.domain.port.in.ValidateTransactionUseCase.ValidationResult;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import com.agentbanking.shared.money.domain.Money;
import java.util.ArrayList;
import java.util.List;

public class RulesService implements ValidateTransactionUseCase {

  private final FeeConfigRepository feeConfigRepository;
  private final VelocityRuleRepository velocityRuleRepository;
  private final FeeCalculationService feeCalculationService;

  public RulesService(FeeConfigRepository feeConfigRepository, 
                      VelocityRuleRepository velocityRuleRepository,
                      FeeCalculationService feeCalculationService) {
    this.feeConfigRepository = feeConfigRepository;
    this.velocityRuleRepository = velocityRuleRepository;
    this.feeCalculationService = feeCalculationService;
  }

  public ValidationResult validate(ValidateTransactionUseCase.TransactionValidationRequest request) {
    List<String> errors = new ArrayList<>();

    if (!request.amount().isPositive()) {
      errors.add("Amount must be positive");
    }

    var feeConfigOpt = feeConfigRepository.findByTransactionTypeAndAgentTier(
        request.type(), request.agentTier());
    
    FeeCalculationResult feeResult = null;
    
    if (feeConfigOpt.isEmpty()) {
      errors.add("Fee configuration not found for transaction type and agent tier");
    } else if (!feeConfigOpt.get().isActive()) {
      errors.add("Fee configuration is not active");
    } else {
      feeResult = feeCalculationService.calculate(feeConfigOpt.get(), request.amount());
    }

    var velocityRuleOpt = velocityRuleRepository.findActiveByScope(
        com.agentbanking.rules.domain.model.VelocityScope.PER_AGENT);
    
    if (velocityRuleOpt.isPresent()) {
      VelocityRule rule = velocityRuleOpt.get();
      if (rule.isExceeded(1, request.amount())) {
        errors.add("Velocity limit exceeded for agent");
      }
    }

    return new ValidationResult(errors.isEmpty(), errors, feeResult);
  }
}