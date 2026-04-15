package com.agentbanking.hsmadapter.infrastructure.external;

import feign.Logger;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HsmGatewayPortConfiguration {

    @Bean
    public Logger.Level hsmLoggerLevel() {
        return Logger.Level.NONE;
    }

    @Bean
    public Request.Options hsmRequestOptions() {
        return new Request.Options(5000, 10000);
    }
}