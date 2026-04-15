package com.agentbanking.notification.application.config;

import com.agentbanking.notification.domain.port.in.SendNotificationUseCase;
import com.agentbanking.notification.domain.service.NotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationDomainServiceConfig {

  @Bean
  public NotificationService notificationService() {
    return new NotificationService();
  }

  @Bean
  public SendNotificationUseCase sendNotificationUseCase(NotificationService service) {
    return service;
  }
}