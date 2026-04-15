package com.agentbanking.isoadapter.infrastructure.config;

import com.agentbanking.isoadapter.domain.port.in.IsoAdapterService;
import com.agentbanking.isoadapter.infrastructure.external.IsoSocketClient;
import com.agentbanking.isoadapter.infrastructure.external.SwitchPort;
import com.agentbanking.isoadapter.infrastructure.secondary.IsoAdapterServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IsoAdapterDomainServiceConfig {

    @Value("${switch.host:localhost}")
    private String switchHost;

    @Value("${switch.port:9090}")
    private int switchPort;

    @Value("${switch.connection-timeout:10000}")
    private int connectionTimeout;

    @Value("${switch.socket-timeout:30000}")
    private int socketTimeout;

    @Bean
    public IsoAdapterService isoAdapterService() {
        return new IsoAdapterServiceImpl();
    }

    @Bean
    public IsoSocketClient isoSocketClient() {
        return new IsoSocketClient(switchHost, switchPort, connectionTimeout, socketTimeout);
    }
}