package com.agentbanking.notification.domain.port.in;

import com.agentbanking.notification.domain.model.NotificationResult;
import com.agentbanking.notification.domain.model.NotificationType;

public interface SendNotificationUseCase {

  NotificationResult send(String recipient, String message, NotificationType type);
}