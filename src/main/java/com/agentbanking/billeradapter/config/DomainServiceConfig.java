package com.agentbanking.billeradapter.config;

import com.agentbanking.billeradapter.domain.port.out.BillerGatewayPort;
import com.agentbanking.billeradapter.domain.port.out.BillerRegistryPort;
import com.agentbanking.billeradapter.domain.service.BillPaymentService;
import com.agentbanking.billeradapter.infrastructure.adapter.CompositeBillerAdapter;
import com.agentbanking.billeradapter.infrastructure.external.*;
import com.agentbanking.billeradapter.infrastructure.registry.BillerRegistry;
import com.agentbanking.billeradapter.application.service.BillerApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

  @Bean
  public BillerRegistryPort billerRegistryPort() {
    return new BillerRegistry();
  }

  @Bean
  public BillerGatewayPort billerGatewayPort(
      TnbBillerClient tnbClient,
      MaxisBillerClient maxisClient,
      JompayBillerClient jompayClient,
      AstroBillerClient astroClient,
      EpfBillerClient epfClient,
      TmBillerClient tmClient) {
    return new CompositeBillerAdapter(tnbClient, maxisClient, jompayClient, astroClient, epfClient, tmClient);
  }

  @Bean
  public BillPaymentService billPaymentService(
      BillerGatewayPort gatewayPort,
      BillerRegistryPort registryPort) {
    return new BillPaymentService(gatewayPort, registryPort);
  }

  @Bean
  public BillerApplicationService billerApplicationService(
      BillPaymentService billPaymentService) {
    return new BillerApplicationService(billPaymentService, billPaymentService);
  }
}