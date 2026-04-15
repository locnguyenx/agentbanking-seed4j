package com.agentbanking.notification.domain.service;

import static org.assertj.core.api.Assertions.*;

import com.agentbanking.UnitTest;
import com.agentbanking.notification.domain.model.NotificationResult;
import com.agentbanking.notification.domain.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
@DisplayName("NotificationService")
class NotificationServiceTest {

  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    notificationService = new NotificationService();
  }

  @Nested
  class SendNotification {

    @Test
    void shouldSendSMSNotificationSuccessfully() {
      String recipient = "+60123456789";
      String message = "Your OTP is 123456";
      NotificationType type = NotificationType.SMS;

      NotificationResult result = notificationService.send(recipient, message, type);

      assertThat(result.notificationId()).isNotNull();
      assertThat(result.success()).isTrue();
      assertThat(result.errorCode()).isNull();
    }

    @Test
    void shouldSendEmailNotificationSuccessfully() {
      String recipient = "user@example.com";
      String message = "Your transaction was successful";
      NotificationType type = NotificationType.EMAIL;

      NotificationResult result = notificationService.send(recipient, message, type);

      assertThat(result.notificationId()).isNotNull();
      assertThat(result.success()).isTrue();
      assertThat(result.errorCode()).isNull();
    }

    @Test
    void shouldSendPushNotificationSuccessfully() {
      String recipient = "device-token-abc123";
      String message = "New commission credited";
      NotificationType type = NotificationType.PUSH;

      NotificationResult result = notificationService.send(recipient, message, type);

      assertThat(result.notificationId()).isNotNull();
      assertThat(result.success()).isTrue();
      assertThat(result.errorCode()).isNull();
    }

    @Test
    void shouldGenerateUniqueNotificationIdForEachSend() {
      NotificationResult result1 = notificationService.send("+60123456789", "Message 1", NotificationType.SMS);
      NotificationResult result2 = notificationService.send("+60123456789", "Message 2", NotificationType.SMS);

      assertThat(result1.notificationId()).isNotEqualTo(result2.notificationId());
    }

    @Test
    void shouldSendNotificationWithEmptyMessage() {
      NotificationResult result = notificationService.send("+60123456789", "", NotificationType.SMS);

      assertThat(result.success()).isTrue();
      assertThat(result.errorCode()).isNull();
    }

    @Test
    void shouldSendNotificationWithNullMessage() {
      NotificationResult result = notificationService.send("+60123456789", null, NotificationType.SMS);

      assertThat(result.success()).isTrue();
      assertThat(result.errorCode()).isNull();
    }

    @Test
    void shouldSendNotificationWithEmptyRecipient() {
      NotificationResult result = notificationService.send("", "Test message", NotificationType.EMAIL);

      assertThat(result.success()).isTrue();
      assertThat(result.errorCode()).isNull();
    }

    @Test
    void shouldSendSMSWithLongMessage() {
      String longMessage = "A".repeat(1000);

      NotificationResult result = notificationService.send("+60123456789", longMessage, NotificationType.SMS);

      assertThat(result.success()).isTrue();
      assertThat(result.errorCode()).isNull();
    }

    @Test
    void shouldSendEmailWithSpecialCharactersInMessage() {
      String message = "Your balance is RM 1,234.56 & commission is RM 12.34 <tax>";

      NotificationResult result = notificationService.send("user@example.com", message, NotificationType.EMAIL);

      assertThat(result.success()).isTrue();
      assertThat(result.errorCode()).isNull();
    }

    @Test
    void shouldSendPushWithDeviceTokenAsRecipient() {
      String deviceToken = "APA91bHun4MxPqegoKMx0W0l5Yd8vTJ3g";

      NotificationResult result = notificationService.send(deviceToken, "Push notification", NotificationType.PUSH);

      assertThat(result.success()).isTrue();
      assertThat(result.errorCode()).isNull();
    }
  }
}
