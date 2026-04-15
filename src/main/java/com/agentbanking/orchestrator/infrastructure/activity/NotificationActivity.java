package com.agentbanking.orchestrator.infrastructure.activity;

import com.agentbanking.notification.domain.model.NotificationResult;
import com.agentbanking.notification.domain.model.NotificationType;
import com.agentbanking.notification.domain.port.in.SendNotificationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationActivity {

    private static final Logger log = LoggerFactory.getLogger(NotificationActivity.class);

    private final SendNotificationUseCase sendNotificationUseCase;

    public NotificationActivity(SendNotificationUseCase sendNotificationUseCase) {
        this.sendNotificationUseCase = sendNotificationUseCase;
    }

    public void sendWithdrawalSuccessNotification(String customerPhone, String amount, String stan) {
        log.info("Sending withdrawal success SMS to customer: phone={}, amount={}, stan={}", 
            customerPhone, amount, stan);
        String message = String.format(
            "Your cash withdrawal of RM%s has been completed. STAN: %s. Thank you for using Agent Banking.",
            amount, stan
        );
        sendNotificationUseCase.send(customerPhone, message, NotificationType.SMS);
        log.info("SMS notification sent successfully");
    }

    public void sendDepositSuccessNotification(String customerPhone, String amount, String stan) {
        log.info("Sending deposit success SMS to customer: phone={}, amount={}, stan={}", 
            customerPhone, amount, stan);
        String message = String.format(
            "Your cash deposit of RM%s has been received. STAN: %s. Thank you for using Agent Banking.",
            amount, stan
        );
        sendNotificationUseCase.send(customerPhone, message, NotificationType.SMS);
        log.info("SMS notification sent successfully");
    }

    public void sendReversalNotification(String customerPhone, String amount, String reason) {
        log.info("Sending reversal notification to customer: phone={}, amount={}, reason={}", 
            customerPhone, amount, reason);
        String message = String.format(
            "Your transaction of RM%s has been reversed. Reason: %s. Please contact your agent for details.",
            amount, reason
        );
        sendNotificationUseCase.send(customerPhone, message, NotificationType.SMS);
        log.info("Reversal notification sent successfully");
    }
}