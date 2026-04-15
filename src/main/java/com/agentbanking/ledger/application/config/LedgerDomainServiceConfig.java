package com.agentbanking.ledger.application.config;

import com.agentbanking.ledger.domain.port.out.LedgerRepository;
import com.agentbanking.ledger.domain.service.DoubleEntryService;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerDomainServiceConfig {

  @Bean
  public LedgerService ledgerService(LedgerRepository ledgerRepository) {
    return new LedgerService(ledgerRepository);
  }

  @Bean
  public DoubleEntryService doubleEntryService() {
    return new DoubleEntryService();
  }
}