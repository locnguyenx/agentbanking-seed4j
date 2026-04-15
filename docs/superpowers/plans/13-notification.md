# Notification Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Notification Service bounded context for Agent Banking Platform — consumes Kafka events and dispatches SMS/Email notifications to customers and agents.

**Architecture:** Wave 4 — Normal Service (NO Temporal). Hexagonal architecture with domain layer having ZERO framework imports. Kafka consumer for async event processing via Spring Cloud Stream. Template-based message formatting.

**Tech Stack:** Java 21, Spring Boot 4, Spring Cloud Stream (Kafka), JUnit 5, Mockito, ArchUnit, Gradle

---

## Task 1: Seed4J Scaffolding

Run Seed4J CLI to scaffold the notification service with Kafka infrastructure:

```bash
# Apply spring-boot-kafka module for Kafka infrastructure
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply spring-boot-kafka \
  --context notification \
  --package com.agentbanking.notification \
  --no-commit
```

This creates:
- Hexagonal package structure: `domain/`, `application/`, `infrastructure/`, `config/`
- `@BusinessContext` annotation on package-info
- `DomainServiceConfig.java` stub with `@Bean` registration pattern
- Flyway migration stub: `V1__notification_init.sql`
- ArchUnit test: `HexagonalArchitectureTest.java`
- Test base classes and `@UnitTest` annotation
- Gradle build configuration with Kafka dependencies

- [ ] **Step 1: Run Seed4J scaffolding**
- [ ] **Step 2: Verify generated structure matches hexagonal layout**
- [ ] **Step 3: Commit scaffolding**

```bash
git add . && git commit -m "feat(notification): scaffold with Seed4J spring-boot-kafka module"
```

---

## Task 2: Domain Models (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/notification/domain/model/NotificationType.java`
- Create: `src/main/java/com/agentbanking/notification/domain/model/NotificationStatus.java`
- Create: `src/main/java/com/agentbanking/notification/domain/model/Notification.java`
- Create: `src/main/java/com/agentbanking/notification/domain/model/NotificationTemplate.java`

**CRITICAL: ZERO framework imports in domain/ — no Logger, no Spring, no JPA.**

- [ ] **Step 1: Write tests**

```java
package com.agentbanking.notification.domain.model;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
class NotificationTest {

  @Test
  @DisplayName("should create notification with PENDING status")
  void shouldCreateWithPendingStatus() {
    Notification notification = Notification.send(
      "AGT-001", "+60121234567",
      NotificationType.TRANSACTION_SUCCESS,
      "Your transaction of RM 500.00 was successful."
    );
    assertThat(notification.status()).isEqualTo(NotificationStatus.PENDING);
    assertThat(notification.type()).isEqualTo(NotificationType.TRANSACTION_SUCCESS);
  }

  @Test
  @DisplayName("should mark as sent successfully")
  void shouldMarkAsSent() {
    Notification notification = Notification.send(
      "AGT-001", "+60121234567", NotificationType.TRANSACTION_SUCCESS, "Test message"
    );
    Notification sent = notification.markSent();
    assertThat(sent.status()).isEqualTo(NotificationStatus.SENT);
    assertThat(sent.sentAt()).isNotNull();
  }

  @Test
  @DisplayName("should mark as failed")
  void shouldMarkAsFailed() {
    Notification notification = Notification.send(
      "AGT-001", "+60121234567", NotificationType.TRANSACTION_SUCCESS, "Test message"
    );
    Notification failed = notification.markFailed("SMS provider error");
    assertThat(failed.status()).isEqualTo(NotificationStatus.FAILED);
    assertThat(failed.errorMessage()).isEqualTo("SMS provider error");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "NotificationTest"
```
Expected: FAIL — classes not found

- [ ] **Step 3: Write implementation**

**NotificationType.java:**
```java
package com.agentbanking.notification.domain.model;

public enum NotificationType {
  TRANSACTION_SUCCESS,
  TRANSACTION_FAILED,
  TRANSACTION_REVERSAL,
  KYC_APPROVED,
  KYC_REJECTED,
  AGENT_APPROVED,
  AGENT_REJECTED,
  AGENT_SUSPENDED,
  SETTLEMENT_COMPLETE,
  FLOAT_LOW_BALANCE,
  COMMISSION_CREDITED,
  GENERAL
}
```

**NotificationStatus.java:**
```java
package com.agentbanking.notification.domain.model;

public enum NotificationStatus {
  PENDING, SENT, FAILED, RETRY_EXHAUSTED
}
```

**Notification.java:**
```java
package com.agentbanking.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Notification(
  UUID id, String agentId, String recipient, NotificationType type,
  NotificationStatus status, String message, String channel,
  Instant createdAt, Instant sentAt, String errorMessage, int retryCount
) {
  public static Notification send(String agentId, String recipient, NotificationType type, String message) {
    return new Notification(UUID.randomUUID(), agentId, recipient, type,
      NotificationStatus.PENDING, message, "SMS", Instant.now(), null, null, 0);
  }

  public Notification markSent() {
    return new Notification(id, agentId, recipient, type, NotificationStatus.SENT,
      message, channel, createdAt, Instant.now(), errorMessage, retryCount);
  }

  public Notification markFailed(String errorMessage) {
    return new Notification(id, agentId, recipient, type, NotificationStatus.FAILED,
      message, channel, createdAt, sentAt, errorMessage, retryCount);
  }

  public Notification incrementRetry() {
    return new Notification(id, agentId, recipient, type, status,
      message, channel, createdAt, sentAt, errorMessage, retryCount + 1);
  }
}
```

**NotificationTemplate.java:**
```java
package com.agentbanking.notification.domain.model;

import java.util.Map;

public record NotificationTemplate(NotificationType type, String template) {
  public String format(Map<String, String> variables) {
    String result = template;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return result;
  }

  public static NotificationTemplate transactionSuccess() {
    return new NotificationTemplate(NotificationType.TRANSACTION_SUCCESS,
      "Your {{transactionType}} of RM {{amount}} was successful. Ref: {{reference}}");
  }

  public static NotificationTemplate agentApproved() {
    return new NotificationTemplate(NotificationType.AGENT_APPROVED,
      "Congratulations! Your agent application has been approved. Agent ID: {{agentId}}");
  }

  public static NotificationTemplate settlementComplete() {
    return new NotificationTemplate(NotificationType.SETTLEMENT_COMPLETE,
      "Settlement completed. Net amount: RM {{amount}}. Reference: {{reference}}");
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew test --tests "NotificationTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/agentbanking/notification/domain/model/
git commit -m "feat(notification): add domain models"
```

---

## Task 3: Domain Ports and Services (Write Manually)

**CRITICAL: NO Logger in domain layer.** Logging belongs in infrastructure or application layer.

**Files:**
- Create: `src/main/java/com/agentbanking/notification/domain/port/out/NotificationRepository.java`
- Create: `src/main/java/com/agentbanking/notification/domain/port/out/SmsGatewayPort.java`
- Create: `src/main/java/com/agentbanking/notification/domain/port/in/SendNotificationUseCase.java`
- Create: `src/main/java/com/agentbanking/notification/domain/service/NotificationDispatchService.java`

- [ ] **Step 1: Write implementation**

**NotificationRepository.java:**
```java
package com.agentbanking.notification.domain.port.out;

import com.agentbanking.notification.domain.model.Notification;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
  Optional<Notification> findById(UUID id);
  Notification save(Notification notification);
}
```

**SmsGatewayPort.java:**
```java
package com.agentbanking.notification.domain.port.out;

public interface SmsGatewayPort {
  SendResult send(String mobileNumber, String message);

  record SendResult(boolean success, String messageId, String errorMessage) {
    public static SendResult success(String messageId) {
      return new SendResult(true, messageId, null);
    }
    public static SendResult failure(String errorMessage) {
      return new SendResult(false, null, errorMessage);
    }
  }
}
```

**SendNotificationUseCase.java:**
```java
package com.agentbanking.notification.domain.port.in;

import com.agentbanking.notification.domain.model.NotificationType;

public interface SendNotificationUseCase {
  void send(String recipient, NotificationType type, String message);
  void sendToAgent(String agentId, String mobileNumber, NotificationType type, String message);
}
```

**NotificationDispatchService.java (NO Logger):**
```java
package com.agentbanking.notification.domain.service;

import com.agentbanking.notification.domain.model.Notification;
import com.agentbanking.notification.domain.model.NotificationType;
import com.agentbanking.notification.domain.port.out.NotificationRepository;
import com.agentbanking.notification.domain.port.out.SmsGatewayPort;

import java.util.Map;

public class NotificationDispatchService {

  private static final int MAX_RETRIES = 3;

  private final NotificationRepository repository;
  private final SmsGatewayPort smsGateway;

  public NotificationDispatchService(NotificationRepository repository, SmsGatewayPort smsGateway) {
    this.repository = repository;
    this.smsGateway = smsGateway;
  }

  public void dispatch(String agentId, String mobileNumber, NotificationType type, String message) {
    Notification notification = Notification.send(agentId, mobileNumber, type, message);
    repository.save(notification);

    SmsGatewayPort.SendResult result = smsGateway.send(mobileNumber, message);

    if (result.success()) {
      Notification sent = notification.markSent();
      repository.save(sent);
    } else {
      handleFailure(notification, result.errorMessage());
    }
  }

  private void handleFailure(Notification notification, String errorMessage) {
    if (notification.retryCount() < MAX_RETRIES) {
      Notification retry = notification.incrementRetry();
      repository.save(retry);
    } else {
      Notification failed = notification.markFailed(errorMessage);
      repository.save(failed);
    }
  }

  public void dispatchFromEvent(String agentId, String mobileNumber, NotificationType type,
                                String template, Map<String, String> variables) {
    String message = applyTemplate(template, variables);
    dispatch(agentId, mobileNumber, type, message);
  }

  private String applyTemplate(String template, Map<String, String> variables) {
    String result = template;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return result;
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/notification/domain/port/
git add src/main/java/com/agentbanking/notification/domain/service/
git commit -m "feat(notification): add domain ports and dispatch service"
```

---

## Task 4: Application Layer — DTOs and Service (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/notification/application/dto/TransactionNotificationEvent.java`
- Create: `src/main/java/com/agentbanking/notification/application/dto/AgentNotificationEvent.java`
- Create: `src/main/java/com/agentbanking/notification/application/service/NotificationApplicationService.java`

- [ ] **Step 1: Write implementation**

**TransactionNotificationEvent.java:**
```java
package com.agentbanking.notification.application.dto;

import com.agentbanking.notification.domain.model.NotificationType;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionNotificationEvent(
  String eventId, String agentId, String mobileNumber,
  NotificationType notificationType, String transactionType,
  BigDecimal amount, String reference, Instant timestamp
) {}
```

**AgentNotificationEvent.java:**
```java
package com.agentbanking.notification.application.dto;

import com.agentbanking.notification.domain.model.NotificationType;
import java.time.Instant;

public record AgentNotificationEvent(
  String eventId, String agentId, String mobileNumber,
  NotificationType notificationType, String agentName,
  String message, Instant timestamp
) {}
```

**NotificationApplicationService.java:**
```java
package com.agentbanking.notification.application.service;

import com.agentbanking.notification.application.dto.*;
import com.agentbanking.notification.domain.model.NotificationType;
import com.agentbanking.notification.domain.service.NotificationDispatchService;

import java.util.Map;

public class NotificationApplicationService {

  private final NotificationDispatchService dispatchService;

  public NotificationApplicationService(NotificationDispatchService dispatchService) {
    this.dispatchService = dispatchService;
  }

  public void handleTransactionEvent(TransactionNotificationEvent event) {
    String template = getTemplateForType(event.notificationType());
    Map<String, String> variables = Map.of(
      "transactionType", event.transactionType(),
      "amount", event.amount().toPlainString(),
      "reference", event.reference()
    );
    dispatchService.dispatchFromEvent(event.agentId(), event.mobileNumber(),
      event.notificationType(), template, variables);
  }

  public void handleAgentEvent(AgentNotificationEvent event) {
    String template = getTemplateForType(event.notificationType());
    Map<String, String> variables = Map.of(
      "agentName", event.agentName(),
      "message", event.message() != null ? event.message() : ""
    );
    dispatchService.dispatchFromEvent(event.agentId(), event.mobileNumber(),
      event.notificationType(), template, variables);
  }

  private String getTemplateForType(NotificationType type) {
    return switch (type) {
      case TRANSACTION_SUCCESS -> "Your {{transactionType}} of RM {{amount}} was successful. Ref: {{reference}}";
      case TRANSACTION_FAILED -> "Your {{transactionType}} of RM {{amount}} failed. Ref: {{reference}}. Reason: {{reason}}";
      case KYC_APPROVED -> "Your KYC verification has been approved.";
      case KYC_REJECTED -> "Your KYC verification was rejected. Reason: {{reason}}";
      case AGENT_APPROVED -> "Congratulations {{agentName}}! Your agent application has been approved.";
      case AGENT_REJECTED -> "Your agent application was rejected. Reason: {{reason}}";
      case SETTLEMENT_COMPLETE -> "Settlement completed. Amount: RM {{amount}}. Ref: {{reference}}";
      case FLOAT_LOW_BALANCE -> "Alert: Your float balance is low (RM {{balance}}). Please top up.";
      case COMMISSION_CREDITED -> "Commission of RM {{amount}} has been credited to your account.";
      default -> "{{message}}";
    };
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/notification/application/
git commit -m "feat(notification): add application DTOs and service"
```

---

## Task 5: Infrastructure — Kafka Consumer, SMS Gateway, Repository (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/notification/infrastructure/messaging/NotificationEventConsumer.java`
- Create: `src/main/java/com/agentbanking/notification/infrastructure/external/SmsGatewayClient.java`
- Create: `src/main/java/com/agentbanking/notification/infrastructure/persistence/repository/NotificationRepositoryImpl.java`

- [ ] **Step 1: Write implementation**

**NotificationEventConsumer.java:**
```java
package com.agentbanking.notification.infrastructure.messaging;

import com.agentbanking.notification.application.dto.AgentNotificationEvent;
import com.agentbanking.notification.application.dto.TransactionNotificationEvent;
import com.agentbanking.notification.application.service.NotificationApplicationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

@Configuration
public class NotificationEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

  private final NotificationApplicationService service;

  public NotificationEventConsumer(NotificationApplicationService service) {
    this.service = service;
  }

  @KafkaListener(topics = "${agentbanking.kafka.topic.transaction-events}", groupId = "notification-group")
  public void consumeTransactionEvent(@Payload TransactionNotificationEvent event) {
    log.info("Consumed transaction event: {}", event.eventId());
    try {
      service.handleTransactionEvent(event);
    } catch (Exception e) {
      log.error("Failed to process transaction event: {}", event.eventId(), e);
    }
  }

  @KafkaListener(topics = "${agentbanking.kafka.topic.agent-events}", groupId = "notification-group")
  public void consumeAgentEvent(@Payload AgentNotificationEvent event) {
    log.info("Consumed agent event: {}", event.eventId());
    try {
      service.handleAgentEvent(event);
    } catch (Exception e) {
      log.error("Failed to process agent event: {}", event.eventId(), e);
    }
  }
}
```

**SmsGatewayClient.java:**
```java
package com.agentbanking.notification.infrastructure.external;

import com.agentbanking.notification.domain.port.out.SmsGatewayPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsGatewayClient implements SmsGatewayPort {

  private static final Logger log = LoggerFactory.getLogger(SmsGatewayClient.class);

  @Override
  public SendResult send(String mobileNumber, String message) {
    log.info("Sending SMS to: {}, message length: {}", mobileNumber, message.length());
    try {
      // TODO: Integrate with actual SMS provider (e.g., Twilio, Nexmo)
      String messageId = "MSG-" + System.currentTimeMillis();
      return SendResult.success(messageId);
    } catch (Exception e) {
      log.error("Failed to send SMS to {}: {}", mobileNumber, e.getMessage());
      return SendResult.failure(e.getMessage());
    }
  }
}
```

**NotificationRepositoryImpl.java:**
```java
package com.agentbanking.notification.infrastructure.persistence.repository;

import com.agentbanking.notification.domain.model.Notification;
import com.agentbanking.notification.domain.port.out.NotificationRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

  @Override
  public Optional<Notification> findById(UUID id) {
    // TODO: Implement JPA repository
    return Optional.empty();
  }

  @Override
  public Notification save(Notification notification) {
    // TODO: Implement JPA repository
    return notification;
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/notification/infrastructure/
git commit -m "feat(notification): add Kafka consumer, SMS gateway, and repository"
```

---

## Task 6: Config — Domain Service Registration (Write Manually)

**Law V: Domain services via @Bean in config, NOT @Service annotation.**

**Files:**
- Update: `src/main/java/com/agentbanking/notification/config/DomainServiceConfig.java` (created by Seed4J)

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.notification.config;

import com.agentbanking.notification.domain.port.out.NotificationRepository;
import com.agentbanking.notification.domain.port.out.SmsGatewayPort;
import com.agentbanking.notification.domain.service.NotificationDispatchService;
import com.agentbanking.notification.application.service.NotificationApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

  @Bean
  public NotificationDispatchService notificationDispatchService(
    NotificationRepository repository, SmsGatewayPort smsGateway
  ) {
    return new NotificationDispatchService(repository, smsGateway);
  }

  @Bean
  public NotificationApplicationService notificationApplicationService(
    NotificationDispatchService dispatchService
  ) {
    return new NotificationApplicationService(dispatchService);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/notification/config/DomainServiceConfig.java
git commit -m "feat(notification): register domain services via @Bean"
```

---

## Task 7: Application Configuration and Flyway

**Files:**
- Update: `src/main/resources/application.yaml`
- Update: `src/main/resources/db/migration/V1__notification_init.sql` (created by Seed4J)

- [ ] **Step 1: Update application.yaml**

```yaml
agentbanking:
  kafka:
    topic:
      transaction-events: agentbanking.transaction-events
      agent-events: agentbanking.agent-events
      settlement-events: agentbanking.settlement-events
  notification:
    sms:
      provider: mock
      timeout-seconds: 30
    retry:
      max-attempts: 3
      initial-interval-ms: 1000
      multiplier: 2.0
```

- [ ] **Step 2: Update Flyway migration**

```sql
CREATE TABLE IF NOT EXISTS notification (
    id UUID PRIMARY KEY,
    agent_id VARCHAR(50),
    recipient VARCHAR(20) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_notification_status ON notification(status);
CREATE INDEX idx_notification_agent ON notification(agent_id);
CREATE INDEX idx_notification_recipient ON notification(recipient);
CREATE INDEX idx_notification_created ON notification(created_at);
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/
git commit -m "feat(notification): add Kafka topic config and Flyway migration"
```

---

## Verification

```bash
./gradlew test
```

Expected: All tests PASS, ArchUnit compliance verified.

---

## Summary

| Task | Component | Approach | Tests |
|------|-----------|----------|-------|
| 1 | Scaffolding | **Seed4J CLI** (spring-boot-kafka) | ArchUnit |
| 2 | Domain Models | Manual | 1 |
| 3 | Domain Ports & Services | Manual (NO Logger in domain) | 0 |
| 4 | Application DTOs & Service | Manual | 0 |
| 5 | Infrastructure (Kafka, SMS, Repo) | Manual | 0 |
| 6 | Config (@Bean registration) | Manual (Law V) | 0 |
| 7 | Application Config + Flyway | Manual | 0 |

**Key fixes from original plan:**
- Removed Logger from `NotificationDispatchService` (Law VI violation)
- Kafka consumer in `infrastructure/messaging/` (not mixed with domain)
- Domain services via `@Bean` in config (Law V), NOT `@Service`
- `./gradlew test` (not `./mvnw`)
- Seed4J scaffolding replaces manual package-info.java creation
