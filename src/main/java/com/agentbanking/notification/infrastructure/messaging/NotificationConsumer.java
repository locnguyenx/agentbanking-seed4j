package com.agentbanking.notification.infrastructure.messaging;

import com.agentbanking.notification.domain.model.NotificationEvent;
import com.agentbanking.notification.domain.model.NotificationType;
import com.agentbanking.notification.domain.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class NotificationConsumer {

  private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

  private final NotificationService notificationService;

  public NotificationConsumer(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Bean
  public Consumer<NotificationEvent> processNotifications() {
    return event -> {
      log.info("Processing notification: {} to {}", event.type(), event.recipient());
      notificationService.send(event.recipient(), event.message(), event.type());
    };
  }

  @Bean
  public Consumer<NotificationEvent> processSmsNotifications() {
    return event -> {
      if (event.type() == NotificationType.SMS) {
        log.info("Processing SMS notification to {}", event.recipient());
        notificationService.send(event.recipient(), event.message(), NotificationType.SMS);
      }
    };
  }

  @Bean
  public Consumer<NotificationEvent> processEmailNotifications() {
    return event -> {
      if (event.type() == NotificationType.EMAIL) {
        log.info("Processing email notification to {}", event.recipient());
        notificationService.send(event.recipient(), event.message(), NotificationType.EMAIL);
      }
    };
  }
}
