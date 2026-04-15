package com.agentbanking.prepaid.config;

import com.agentbanking.prepaid.domain.port.out.TopUpGatewayPort;
import com.agentbanking.prepaid.domain.port.out.TopUpProviderRegistryPort;
import com.agentbanking.prepaid.domain.service.PrepaidTopUpService;
import com.agentbanking.prepaid.domain.service.PhoneValidationService;
import com.agentbanking.prepaid.infrastructure.external.PrepaidAggregatorClient;
import com.agentbanking.prepaid.infrastructure.registry.PrepaidProviderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrepaidDomainServiceConfig {

    @Bean
    public TopUpProviderRegistryPort topUpProviderRegistryPort() {
        return new PrepaidProviderRegistry();
    }

    @Bean
    public TopUpGatewayPort topUpGatewayPort(PrepaidAggregatorClient aggregatorClient) {
        return aggregatorClient;
    }

    @Bean
    public PhoneValidationService phoneValidationService() {
        return new PhoneValidationService();
    }

    @Bean
    public PrepaidTopUpService prepaidTopUpService(
            TopUpGatewayPort gatewayPort,
            TopUpProviderRegistryPort registryPort,
            PhoneValidationService phoneValidationService) {
        return new PrepaidTopUpService(gatewayPort, registryPort, phoneValidationService);
    }
}