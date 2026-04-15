package com.agentbanking.switchadapter.config;

import com.agentbanking.switchadapter.application.service.SwitchAdapterApplicationService;
import com.agentbanking.switchadapter.domain.port.out.SwitchPort;
import com.agentbanking.switchadapter.domain.service.IsoTranslationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwitchAdapterDomainServiceConfig {

    @Bean
    public IsoTranslationService isoTranslationService() {
        return new IsoTranslationService();
    }

    @Bean
    public SwitchPort switchPort() {
        return new com.agentbanking.switchadapter.infrastructure.adapter.SwitchPortAdapter();
    }

    @Bean
    public SwitchAdapterApplicationService switchAdapterApplicationService(
            IsoTranslationService translationService,
            SwitchPort switchPort) {
        return new SwitchAdapterApplicationService(translationService, switchPort);
    }
}