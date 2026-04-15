package com.agentbanking.hsmadapter.infrastructure.config;

import com.agentbanking.hsmadapter.domain.port.in.HsmAdapterService;
import com.agentbanking.hsmadapter.infrastructure.external.HsmGatewayPort;
import com.agentbanking.hsmadapter.infrastructure.external.PinManagementService;
import com.agentbanking.hsmadapter.infrastructure.secondary.HsmAdapterServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HsmAdapterDomainServiceConfig {

  @Bean
  public HsmAdapterService hsmAdapterService() {
    return new HsmAdapterServiceImpl();
  }

  @Bean
  public PinManagementService pinManagementService(HsmGatewayPort hsmGateway) {
    return new PinManagementService(hsmGateway);
  }
}