package com.agentbanking.notification.infrastructure.primary;

import com.agentbanking.notification.domain.model.NotificationResult;
import com.agentbanking.notification.domain.model.NotificationType;
import com.agentbanking.notification.domain.port.in.SendNotificationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
class NotificationController {

  private final SendNotificationUseCase sendNotificationUseCase;

  public NotificationController(SendNotificationUseCase sendNotificationUseCase) {
    this.sendNotificationUseCase = sendNotificationUseCase;
  }

  @PostMapping("/send")
  public ResponseEntity<NotificationResult> send(
      @RequestBody Map<String, String> request) {
    NotificationResult result = sendNotificationUseCase.send(
      request.get("recipient"),
      request.get("message"),
      NotificationType.valueOf(request.get("type"))
    );
    return ResponseEntity.ok(result);
  }
}