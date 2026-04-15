package com.agentbanking.isoadapter.infrastructure.external;

import com.agentbanking.isoadapter.infrastructure.external.IsoMessageEncoder;
import com.agentbanking.isoadapter.infrastructure.external.IsoMessageDecoder;
import feign.Logger;
import feign.Request;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwitchPortConfiguration {

    @Bean
    public Encoder isoMessageEncoder() {
        return new IsoMessageEncoder();
    }

    @Bean
    public Decoder isoMessageDecoder() {
        return new IsoMessageDecoder();
    }

    @Bean
    public Logger.Level isoLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options isoRequestOptions() {
        return new Request.Options(10000, 30000);
    }
}