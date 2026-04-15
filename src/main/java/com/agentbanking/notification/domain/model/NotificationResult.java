package com.agentbanking.notification.domain.model;

import java.util.UUID;

public record NotificationResult(
  UUID notificationId,
  boolean success,
  String errorCode
) {}
