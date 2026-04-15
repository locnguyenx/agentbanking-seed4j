package com.agentbanking.orchestrator.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class Resilience4jConfig {

  @Bean
  public Customizer<Resilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
    return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
      .circuitBreakerConfig(CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .slidingWindowSize(10)
        .minimumNumberOfCalls(5)
        .build())
      .timeLimiterConfig(TimeLimiterConfig.custom()
        .timeoutDuration(Duration.ofSeconds(10))
        .build())
      .build());
  }
}
