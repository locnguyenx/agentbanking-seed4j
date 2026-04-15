package com.agentbanking.notification.domain.model;

import java.time.Instant;

public record NotificationEvent(
  String eventId,
  NotificationType type,
  String recipient,
  String message,
  String transactionId,
  Instant createdAt
) {}
