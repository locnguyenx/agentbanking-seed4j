package com.agentbanking.notification.infrastructure.messaging;

import com.agentbanking.notification.domain.model.NotificationEvent;
import com.agentbanking.notification.domain.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class NotificationProducer {

  private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);
  private static final String BINDING = "notificationEvents-out-0";

  private final StreamBridge streamBridge;

  public NotificationProducer(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  public void sendNotification(String recipient, String message, NotificationType type, String transactionId) {
    NotificationEvent event = new NotificationEvent(
      UUID.randomUUID().toString(),
      type,
      recipient,
      message,
      transactionId,
      Instant.now()
    );

    boolean sent = streamBridge.send(BINDING, event);
    if (sent) {
      log.info("Notification event sent: {} to {}", type, recipient);
    } else {
      log.error("Failed to send notification event: {} to {}", type, recipient);
    }
  }
}
