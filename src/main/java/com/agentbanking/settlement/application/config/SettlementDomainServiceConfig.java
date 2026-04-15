package com.agentbanking.settlement.application.config;

import com.agentbanking.settlement.domain.port.in.ProcessSettlementUseCase;
import com.agentbanking.settlement.domain.port.out.SettlementFileRepository;
import com.agentbanking.settlement.domain.service.SettlementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SettlementDomainServiceConfig {

  @Bean
  public SettlementService settlementService(SettlementFileRepository fileRepository) {
    return new SettlementService(fileRepository);
  }

  @Bean
  public ProcessSettlementUseCase processSettlementUseCase(SettlementService service) {
    return service;
  }
}
