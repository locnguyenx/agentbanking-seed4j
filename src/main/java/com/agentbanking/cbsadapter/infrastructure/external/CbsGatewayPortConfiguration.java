package com.agentbanking.cbsadapter.infrastructure.external;

import feign.Logger;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CbsGatewayPortConfiguration {

    @Bean
    public Logger.Level cbsLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options cbsRequestOptions() {
        return new Request.Options(10000, 30000);
    }
}