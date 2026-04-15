package com.agentbanking.orchestrator.application.config;

import com.agentbanking.orchestrator.domain.service.TransactionProcessingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrchestratorDomainServiceConfig {

  @Bean
  public TransactionProcessingService transactionProcessingService() {
    return new TransactionProcessingService();
  }
}