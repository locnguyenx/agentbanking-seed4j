package com.agentbanking.notification.domain.service;

import com.agentbanking.notification.domain.model.NotificationResult;
import com.agentbanking.notification.domain.model.NotificationStatus;
import com.agentbanking.notification.domain.model.NotificationType;
import com.agentbanking.notification.domain.port.in.SendNotificationUseCase;
import java.util.UUID;

public class NotificationService implements SendNotificationUseCase {

  @Override
  public NotificationResult send(String recipient, String message, NotificationType type) {
    UUID notificationId = UUID.randomUUID();
    
    // Simplified: assume success
    // In production, would integrate with SMS/Email providers
    boolean success = true;
    
    return new NotificationResult(
      notificationId,
      success,
      success ? null : "ERR_NOTIF_001"
    );
  }
}