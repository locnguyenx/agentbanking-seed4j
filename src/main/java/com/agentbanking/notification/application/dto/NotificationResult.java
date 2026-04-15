package com.agentbanking.notification.application.dto;

import java.util.UUID;

public record NotificationResult(
  UUID notificationId,
  boolean success,
  String errorCode
) {}