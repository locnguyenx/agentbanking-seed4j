package com.agentbanking.cbsadapter.infrastructure.config;

import com.agentbanking.cbsadapter.domain.port.in.CbsAdapterService;
import com.agentbanking.cbsadapter.infrastructure.external.CbsGatewayPort;
import com.agentbanking.cbsadapter.infrastructure.external.CbsIntegrationService;
import com.agentbanking.cbsadapter.infrastructure.secondary.CbsAdapterServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CbsAdapterDomainServiceConfig {

  @Bean
  public CbsAdapterService cbsAdapterService() {
    return new CbsAdapterServiceImpl();
  }

  @Bean
  public CbsIntegrationService cbsIntegrationService(CbsGatewayPort cbsGateway) {
    return new CbsIntegrationService(cbsGateway);
  }
}