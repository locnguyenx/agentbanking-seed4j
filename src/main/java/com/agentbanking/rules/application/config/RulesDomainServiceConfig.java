package com.agentbanking.rules.application.config;

import com.agentbanking.rules.domain.port.in.ValidateTransactionUseCase;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import com.agentbanking.rules.domain.service.FeeCalculationService;
import com.agentbanking.rules.domain.service.RulesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RulesDomainServiceConfig {

  @Bean
  public RulesService rulesService(
      FeeConfigRepository feeConfigRepository,
      VelocityRuleRepository velocityRuleRepository,
      FeeCalculationService feeCalculationService) {
    return new RulesService(feeConfigRepository, velocityRuleRepository, feeCalculationService);
  }

  @Bean
  public FeeCalculationService feeCalculationService() {
    return new FeeCalculationService();
  }

  @Bean
  public ValidateTransactionUseCase validateTransactionUseCase(RulesService rulesService) {
    return rulesService;
  }
}