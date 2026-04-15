package com.agentbanking.floatagg.application;

import com.agentbanking.floatagg.domain.port.in.BalanceInquiryUseCase;
import com.agentbanking.floatagg.domain.port.in.CreditFloatUseCase;
import com.agentbanking.floatagg.domain.port.in.DebitFloatUseCase;
import com.agentbanking.floatagg.domain.port.out.AgentFloatRepository;
import com.agentbanking.floatagg.domain.service.FloatService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FloatDomainServiceConfig {

  @Bean
  public FloatService floatService(AgentFloatRepository agentFloatRepository) {
    return new FloatService(agentFloatRepository);
  }

  @Bean
  public BalanceInquiryUseCase balanceInquiryUseCase(FloatService service) {
    return service;
  }

  @Bean
  public CreditFloatUseCase creditFloatUseCase(FloatService service) {
    return service;
  }

  @Bean
  public DebitFloatUseCase debitFloatUseCase(FloatService service) {
    return service;
  }
}