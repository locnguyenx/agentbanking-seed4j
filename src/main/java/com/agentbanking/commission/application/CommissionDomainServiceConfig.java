package com.agentbanking.commission.application;

import com.agentbanking.commission.domain.port.in.CalculateCommissionUseCase;
import com.agentbanking.commission.domain.port.in.SettleCommissionUseCase;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import com.agentbanking.commission.domain.service.CommissionCalculationService;
import com.agentbanking.commission.domain.service.CommissionSettlementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommissionDomainServiceConfig {

  @Bean
  public CommissionCalculationService commissionCalculationService(
      CommissionRepository commissionRepository) {
    return new CommissionCalculationService(commissionRepository);
  }

  @Bean
  public CommissionSettlementService commissionSettlementService(
      CommissionRepository commissionRepository) {
    return new CommissionSettlementService(commissionRepository);
  }

  @Bean
  public CalculateCommissionUseCase calculateCommissionUseCase(
      CommissionCalculationService service) {
    return service;
  }

  @Bean
  public SettleCommissionUseCase settleCommissionUseCase(
      CommissionSettlementService service) {
    return service;
  }
}