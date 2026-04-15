# 03. Temporal Infrastructure

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Temporal SDK dependencies, configuration classes, and workflow/activity interface stubs for the Transaction Orchestrator.

**Architecture:** Orchestrator bounded context with hexagonal layout. Temporal SDK integration via Spring Boot configuration. Workflow and activity interfaces in dedicated packages. NO Temporal execution in Wave 2 — this plan provides infrastructure stubs only.

**Tech Stack:** Java 25, Spring Boot 4, Temporal SDK 1.26+, Gradle

**References:**
- Design: `docs/superpowers/specs/agent-banking-platform-design.md` (Section 8: Transaction Orchestrator)

---

## Task 1: Add Temporal Dependencies to build.gradle.kts (MANUAL)

Seed4J CANNOT generate Temporal SDK dependencies. Write manually.

### Add to `gradle/libs.versions.toml`:

```toml
[versions]
temporal = "1.26.0"

[libraries.temporal-spring-boot-starter]
name = "temporal-spring-boot-starter"
group = "io.temporal"

[libraries.temporal-spring-boot-starter.version]
ref = "temporal"

[libraries.temporal-sdk]
name = "temporal-sdk"
group = "io.temporal"

[libraries.temporal-sdk.version]
ref = "temporal"

[libraries.temporal-servicebackend]
name = "temporal-servicebackend"
group = "io.temporal"

[libraries.temporal-servicebackend.version]
ref = "temporal"
```

### Add to `build.gradle.kts` in the `dependencies` block (use seed4j-needle):

```kotlin
implementation(libs.temporal.spring.boot.starter)
implementation(libs.temporal.sdk)
runtimeOnly(libs.temporal.servicebackend)
```

- [ ] `libs.versions.toml` updated with Temporal version and library entries
- [ ] `build.gradle.kts` updated with Temporal dependencies
- [ ] `./gradlew dependencies` resolves without conflicts

---

## Task 2: Seed4J Scaffolding — Orchestrator Package Structure

**Purpose:** Use Seed4J CLI to scaffold the orchestrator bounded context.

**Commands:**

```bash
# List available modules
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar list

# Apply bounded context for orchestrator
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply bounded-context --context orchestrator
```

**Seed4J scaffolds (DO NOT write manually):**
- Package structure: `orchestrator/config/`, `orchestrator/domain/`, `orchestrator/infrastructure/`, `orchestrator/application/`
- `@BusinessContext` annotation on `package-info.java`
- Config class boilerplate (`OrchestratorDatabaseConfig.java` stub)
- Test templates with `@UnitTest`

**After scaffolding, verify:**
- [ ] `src/main/java/com/agentbanking/orchestrator/` exists with hexagonal layout
- [ ] `package-info.java` has `@BusinessContext`
- [ ] Config class stub created

---

## Task 3: Temporal Configuration Classes (MANUAL)

Seed4J scaffolds the package structure, but Temporal-specific configuration must be written manually.

### TemporalProperties.java

Create: `src/main/java/com/agentbanking/orchestrator/config/TemporalProperties.java`

```java
package com.agentbanking.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentbanking.temporal")
public class TemporalProperties {

  private String namespace = "agentbanking.default";
  private String host = "localhost";
  private int port = 7233;
  private String taskQueue = "TRANSACTION_TASK_QUEUE";
  private String connectionTimeout = "30s";
  private String executionTimeout = "5m";

  public String getNamespace() { return namespace; }
  public void setNamespace(String namespace) { this.namespace = namespace; }
  public String getHost() { return host; }
  public void setHost(String host) { this.host = host; }
  public int getPort() { return port; }
  public void setPort(int port) { this.port = port; }
  public String getTaskQueue() { return taskQueue; }
  public void setTaskQueue(String taskQueue) { this.taskQueue = taskQueue; }
  public String getConnectionTimeout() { return connectionTimeout; }
  public void setConnectionTimeout(String connectionTimeout) { this.connectionTimeout = connectionTimeout; }
  public String getExecutionTimeout() { return executionTimeout; }
  public void setExecutionTimeout(String executionTimeout) { this.executionTimeout = executionTimeout; }
}
```

### TemporalConfig.java

Create: `src/main/java/com/agentbanking/orchestrator/config/TemporalConfig.java`

```java
package com.agentbanking.orchestrator.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

  @Bean
  public WorkflowServiceStubs workflowServiceStubs(TemporalProperties props) {
    WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
            .setTarget(props.getHost() + ":" + props.getPort())
            .build();
    return WorkflowServiceStubs.newServiceStubs(options);
  }

  @Bean
  public WorkflowClient workflowClient(WorkflowServiceStubs stubs, TemporalProperties props) {
    return WorkflowClient.newInstance(stubs,
            WorkflowClientOptions.newBuilder()
                    .setNamespace(props.getNamespace())
                    .build());
  }
}
```

- [ ] `TemporalProperties.java` created with `@ConfigurationProperties`
- [ ] `TemporalConfig.java` created with `@Bean` methods (Law V — domain services via config, not annotations)
- [ ] `application.yaml` updated with `agentbanking.temporal.*` properties

### application.yaml additions

Add to `src/main/resources/application.yaml`:

```yaml
agentbanking:
  temporal:
    namespace: agentbanking.default
    host: localhost
    port: 7233
    task-queue: TRANSACTION_TASK_QUEUE
    connection-timeout: 30s
    execution-timeout: 5m
```

---

## Task 4: Workflow Interfaces (MANUAL — Temporal SDK specific)

These are Temporal SDK-specific interfaces. Seed4J cannot generate them.

### TransactionWorkflow.java

Create: `src/main/java/com/agentbanking/orchestrator/workflow/TransactionWorkflow.java`

```java
package com.agentbanking.orchestrator.workflow;

import com.agentbanking.orchestrator.workflow.model.WorkflowInput;
import com.agentbanking.orchestrator.workflow.model.WorkflowResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TransactionWorkflow {

  @WorkflowMethod
  WorkflowResult execute(WorkflowInput input);

  @QueryMethod
  String getStatus();

  @SignalMethod
  void cancel();

  @SignalMethod
  void pause(String reason);
}
```

### CashWithdrawalWorkflow.java (Stub)

Create: `src/main/java/com/agentbanking/orchestrator/workflow/CashWithdrawalWorkflow.java`

```java
package com.agentbanking.orchestrator.workflow;

import com.agentbanking.orchestrator.workflow.dto.CashWithdrawalInput;
import com.agentbanking.orchestrator.workflow.dto.CashWithdrawalResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CashWithdrawalWorkflow {

  @WorkflowMethod
  CashWithdrawalResult execute(CashWithdrawalInput input);
}
```

### CashDepositWorkflow.java (Stub)

Create: `src/main/java/com/agentbanking/orchestrator/workflow/CashDepositWorkflow.java`

```java
package com.agentbanking.orchestrator.workflow;

import com.agentbanking.orchestrator.workflow.dto.CashDepositInput;
import com.agentbanking.orchestrator.workflow.dto.CashDepositResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CashDepositWorkflow {

  @WorkflowMethod
  CashDepositResult execute(CashDepositInput input);
}
```

- [ ] All 3 workflow interfaces created
- [ ] `@WorkflowInterface`, `@WorkflowMethod`, `@QueryMethod`, `@SignalMethod` from Temporal SDK

---

## Task 5: Activity Interfaces (MANUAL — Temporal SDK specific)

### TransactionActivity.java

Create: `src/main/java/com/agentbanking/orchestrator/activity/TransactionActivity.java`

```java
package com.agentbanking.orchestrator.activity;

import com.agentbanking.orchestrator.domain.model.AgentId;
import com.agentbanking.orchestrator.domain.model.Money;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TransactionActivity {

  @ActivityMethod(name = "validateTransaction")
  void validateTransaction(TransactionInput input);

  @ActivityMethod(name = "lockAgentFloat")
  String lockAgentFloat(AgentId agentId, Money amount, String sagaId);

  @ActivityMethod(name = "debitAgentFloat")
  void debitAgentFloat(AgentId agentId, Money amount, String lockId);

  @ActivityMethod(name = "creditCustomer")
  void creditCustomer(String accountId, Money amount);

  @ActivityMethod(name = "releaseFloatLock")
  void releaseFloatLock(String lockId);

  @ActivityMethod(name = "compensateFloatDebit")
  void compensateFloatDebit(AgentId agentId, Money amount, String debitId);
}
```

### FloatActivity.java

Create: `src/main/java/com/agentbanking/orchestrator/activity/FloatActivity.java`

```java
package com.agentbanking.orchestrator.activity;

import com.agentbanking.orchestrator.domain.model.AgentId;
import com.agentbanking.orchestrator.domain.model.Money;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface FloatActivity {

  @ActivityMethod(name = "lockAgentFloat")
  String lockAgentFloat(AgentId agentId, Money amount, String sagaId);

  @ActivityMethod(name = "debitAgentFloat")
  void debitAgentFloat(AgentId agentId, Money amount, String lockId);

  @ActivityMethod(name = "creditAgentFloat")
  void creditAgentFloat(AgentId agentId, Money amount, String lockId);

  @ActivityMethod(name = "releaseFloatLock")
  void releaseFloatLock(String lockId);
}
```

### SwitchActivity.java

Create: `src/main/java/com/agentbanking/orchestrator/activity/SwitchActivity.java`

```java
package com.agentbanking.orchestrator.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface SwitchActivity {

  @ActivityMethod(name = "sendWithdrawal")
  String sendWithdrawal(String agentId, String customerAccountId, String amount, String stan);

  @ActivityMethod(name = "sendDeposit")
  String sendDeposit(String agentId, String customerAccountId, String amount, String stan);

  @ActivityMethod(name = "sendReversal")
  void sendReversal(String originalStan, String reason);
}
```

### LedgerActivity.java

Create: `src/main/java/com/agentbanking/orchestrator/activity/LedgerActivity.java`

```java
package com.agentbanking.orchestrator.activity;

import com.agentbanking.orchestrator.domain.model.Money;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface LedgerActivity {

  @ActivityMethod(name = "creditAccount")
  void creditAccount(String accountId, Money amount, String reference);

  @ActivityMethod(name = "debitAccount")
  void debitAccount(String accountId, Money amount, String reference);

  @ActivityMethod(name = "reverseCredit")
  void reverseCredit(String accountId, Money amount, String reference);

  @ActivityMethod(name = "reverseDebit")
  void reverseDebit(String accountId, Money amount, String reference);
}
```

### NotificationActivity.java

Create: `src/main/java/com/agentbanking/orchestrator/activity/NotificationActivity.java`

```java
package com.agentbanking.orchestrator.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface NotificationActivity {

  @ActivityMethod(name = "sendSms")
  void sendSms(String phoneNumber, String message);

  @ActivityMethod(name = "sendEmail")
  void sendEmail(String email, String subject, String body);

  @ActivityMethod(name = "sendPush")
  void sendPush(String deviceToken, String title, String body);
}
```

- [ ] All 5 activity interfaces created
- [ ] `@ActivityInterface`, `@ActivityMethod` from Temporal SDK

---

## Task 6: Verify Build

- [ ] `./gradlew compileJava` — compiles without errors
- [ ] `./gradlew test` — passes (stub interfaces have no implementations yet)
- [ ] Temporal dependencies resolve correctly

---

## Implementation Notes

1. **Idempotency**: All activities must be idempotent — safe to retry on failure
2. **PESSIMISTIC_WRITE**: Agent float operations must use pessimistic locking
3. **Saga Step Order**: Steps must be executed in order, with compensation in reverse order
4. **Temporal SDK**: Version 1.26.0 — ensure compatibility with Spring Boot 4
5. **Testing**: Use `@MockBean` for repository in integration tests
6. **Wave 2 Note**: NO Temporal server execution in Wave 2 — only infrastructure stubs

---

## Summary

| Task | What | Seed4J or Manual | Files |
|------|------|-----------------|-------|
| 1 | Temporal deps in `libs.versions.toml` + `build.gradle.kts` | **Manual** | 2 |
| 2 | Orchestrator package scaffolding | **Seed4J** | 0 new |
| 3 | `TemporalProperties`, `TemporalConfig`, `application.yaml` | **Manual** | 3 |
| 4 | 3 Workflow interfaces | **Manual** | 3 |
| 5 | 5 Activity interfaces | **Manual** | 5 |
| 6 | Build verification | **Verify** | 0 |

All Temporal SDK classes use `@Configuration` + `@Bean` (Law V). Workflow/activity interfaces are pure Temporal SDK annotations with no Spring imports.
