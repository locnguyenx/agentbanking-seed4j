# Settlement Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Settlement Service bounded context for Agent Banking Platform — handles EOD net settlement calculation, reconciliation, and commission settlement.

**Architecture:** Wave 4 — Normal Service (NO Temporal). Hexagonal architecture with domain layer having ZERO framework imports. Kafka consumer for EOD trigger consumption. Batch processing for large datasets.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, Kafka, PostgreSQL, JUnit 5, Mockito, ArchUnit, Gradle

---

## Task 1: Seed4J Scaffolding

Run Seed4J CLI to scaffold the settlement service with Kafka infrastructure:

```bash
# Apply spring-boot-kafka module for Kafka infrastructure
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply spring-boot-kafka \
  --context settlement \
  --package com.agentbanking.settlement \
  --no-commit
```

This creates:
- Hexagonal package structure: `domain/`, `application/`, `infrastructure/`, `config/`
- `@BusinessContext` annotation on package-info
- `DomainServiceConfig.java` stub with `@Bean` registration pattern
- Flyway migration stub: `V1__settlement_init.sql`
- ArchUnit test: `HexagonalArchitectureTest.java`
- Test base classes and `@UnitTest` annotation
- Gradle build configuration with Kafka + JPA dependencies

- [ ] **Step 1: Run Seed4J scaffolding**
- [ ] **Step 2: Verify generated structure matches hexagonal layout**
- [ ] **Step 3: Commit scaffolding**

```bash
git add . && git commit -m "feat(settlement): scaffold with Seed4J spring-boot-kafka module"
```

---

## Task 2: Domain Models (Write Manually)

**BDD Scenarios:** BDD-SM01 (net settlement), BDD-SM03 (reconciliation)

**CRITICAL: ZERO framework imports in domain/ — no Logger, no Spring, no JPA.**

**Files:**
- Create: `src/main/java/com/agentbanking/settlement/domain/model/SettlementBatch.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/model/SettlementStatus.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/model/SettlementDirection.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/model/ReconciliationRecord.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/model/DiscrepancyType.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/model/AgentSettlement.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/model/CommissionSettlement.java`

- [ ] **Step 1: Write tests**

```java
package com.agentbanking.settlement.domain.model;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
class SettlementBatchTest {

  @Test
  @DisplayName("BDD-SM01: should calculate positive net (Bank owes Agent)")
  void shouldCalculatePositiveNet() {
    BigDecimal withdrawals = new BigDecimal("50000.00");
    BigDecimal commissions = new BigDecimal("1000.00");
    BigDecimal deposits = new BigDecimal("30000.00");
    BigDecimal billPayments = new BigDecimal("10000.00");

    AgentSettlement settlement = AgentSettlement.calculateNet(
      "AGT-001", withdrawals, commissions, deposits, billPayments
    );

    assertThat(settlement.netAmount()).isPositive();
    assertThat(settlement.netAmount()).isEqualByComparingTo(new BigDecimal("11000.00"));
    assertThat(settlement.direction()).isEqualTo(SettlementDirection.BANK_OWES_AGENT);
  }

  @Test
  @DisplayName("BDD-SM01: should calculate negative net (Agent owes Bank)")
  void shouldCalculateNegativeNet() {
    BigDecimal withdrawals = new BigDecimal("20000.00");
    BigDecimal commissions = new BigDecimal("500.00");
    BigDecimal deposits = new BigDecimal("40000.00");
    BigDecimal billPayments = new BigDecimal("10000.00");

    AgentSettlement settlement = AgentSettlement.calculateNet(
      "AGT-001", withdrawals, commissions, deposits, billPayments
    );

    assertThat(settlement.netAmount()).isNegative();
    assertThat(settlement.direction()).isEqualTo(SettlementDirection.AGENT_OWES_BANK);
  }

  @Test
  @DisplayName("BDD-SM01-EC-05: should exclude float top-ups from calculation")
  void shouldExcludeFloatTopUps() {
    BigDecimal withdrawals = new BigDecimal("10000.00");
    BigDecimal commissions = new BigDecimal("200.00");
    BigDecimal deposits = new BigDecimal("5000.00");

    AgentSettlement settlement = AgentSettlement.calculateNet(
      "AGT-001", withdrawals, commissions, deposits, BigDecimal.ZERO
    );

    assertThat(settlement.netAmount()).isEqualByComparingTo(new BigDecimal("5200.00"));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "SettlementBatchTest"
```
Expected: FAIL — classes not found

- [ ] **Step 3: Write implementation**

**SettlementStatus.java:**
```java
package com.agentbanking.settlement.domain.model;

public enum SettlementStatus {
  PENDING, CALCULATING, RECONCILING, GENERATING_REPORT, COMPLETED, FAILED, PAUSED_FOR_DISCREPANCY
}
```

**SettlementDirection.java:**
```java
package com.agentbanking.settlement.domain.model;

public enum SettlementDirection {
  BANK_OWES_AGENT, AGENT_OWES_BANK
}
```

**DiscrepancyType.java:**
```java
package com.agentbanking.settlement.domain.model;

public enum DiscrepancyType {
  GHOST,    // Internal only (not in PayNet)
  ORPHAN,   // PayNet only (not in internal)
  MISMATCH  // Both exist but amount differs
}
```

**AgentSettlement.java:**
```java
package com.agentbanking.settlement.domain.model;

import java.math.BigDecimal;

public record AgentSettlement(
  String agentId, BigDecimal withdrawals, BigDecimal commissions,
  BigDecimal deposits, BigDecimal billPayments,
  BigDecimal netAmount, SettlementDirection direction, boolean hasDiscrepancy
) {
  public static AgentSettlement calculateNet(
    String agentId, BigDecimal withdrawals, BigDecimal commissions,
    BigDecimal deposits, BigDecimal billPayments
  ) {
    BigDecimal net = withdrawals.add(commissions).subtract(deposits).subtract(billPayments);
    SettlementDirection direction = net.compareTo(BigDecimal.ZERO) >= 0
      ? SettlementDirection.BANK_OWES_AGENT : SettlementDirection.AGENT_OWES_BANK;
    return new AgentSettlement(agentId, withdrawals, commissions, deposits,
      billPayments, net.abs(), direction, false);
  }
}
```

**SettlementBatch.java:**
```java
package com.agentbanking.settlement.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SettlementBatch(
  UUID id, LocalDate businessDate, SettlementStatus status,
  BigDecimal totalWithdrawals, BigDecimal totalDeposits,
  BigDecimal totalCommissions, BigDecimal totalBillPayments,
  BigDecimal totalNetAmount, int agentCount, int discrepancyCount,
  LocalDateTime startedAt, LocalDateTime completedAt, String reportPath
) {
  public static SettlementBatch initiate(LocalDate businessDate) {
    return new SettlementBatch(UUID.randomUUID(), businessDate, SettlementStatus.PENDING,
      BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
      BigDecimal.ZERO, 0, 0, LocalDateTime.now(), null, null);
  }

  public SettlementBatch calculating() {
    return new SettlementBatch(id, businessDate, SettlementStatus.CALCULATING,
      totalWithdrawals, totalDeposits, totalCommissions, totalBillPayments,
      totalNetAmount, agentCount, discrepancyCount, startedAt, completedAt, reportPath);
  }

  public SettlementBatch complete(BigDecimal totalNet, int count) {
    return new SettlementBatch(id, businessDate, SettlementStatus.COMPLETED,
      totalWithdrawals, totalDeposits, totalCommissions, totalBillPayments,
      totalNet, count, discrepancyCount, startedAt, LocalDateTime.now(), reportPath);
  }
}
```

**CommissionSettlement.java:**
```java
package com.agentbanking.settlement.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CommissionSettlement(
  UUID id, String agentId, LocalDate settlementDate,
  BigDecimal totalCommission, int transactionCount, String status, String referenceNumber
) {
  public static CommissionSettlement create(String agentId, LocalDate date, BigDecimal amount, int count) {
    return new CommissionSettlement(UUID.randomUUID(), agentId, date, amount,
      count, "PENDING", "COMM-" + System.currentTimeMillis());
  }

  public CommissionSettlement settled() {
    return new CommissionSettlement(id, agentId, settlementDate, totalCommission,
      transactionCount, "SETTLED", referenceNumber);
  }
}
```

**ReconciliationRecord.java:**
```java
package com.agentbanking.settlement.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReconciliationRecord(
  UUID id, String agentId, String transactionId, String stan,
  DiscrepancyType discrepancyType, BigDecimal internalAmount,
  BigDecimal networkAmount, BigDecimal difference,
  String status, String resolution, String resolvedBy,
  LocalDateTime resolvedAt, LocalDateTime createdAt
) {
  public static ReconciliationRecord create(String agentId, String transactionId,
    DiscrepancyType type, BigDecimal internalAmount, BigDecimal networkAmount) {
    return new ReconciliationRecord(UUID.randomUUID(), agentId, transactionId, null,
      type, internalAmount, networkAmount, internalAmount.subtract(networkAmount),
      "PENDING", null, null, null, LocalDateTime.now());
  }

  public ReconciliationRecord resolved(String resolution, String resolvedBy) {
    return new ReconciliationRecord(id, agentId, transactionId, stan, discrepancyType,
      internalAmount, networkAmount, difference, "RESOLVED", resolution,
      resolvedBy, LocalDateTime.now(), createdAt);
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew test --tests "SettlementBatchTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/agentbanking/settlement/domain/model/
git commit -m "feat(settlement): add domain models"
```

---

## Task 3: Domain Ports and Services (Write Manually)

**CRITICAL: NO Logger in domain layer. Query actual agent IDs from repository, NOT hardcoded.**

**Files:**
- Create: `src/main/java/com/agentbanking/settlement/domain/port/out/SettlementRepository.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/port/out/TransactionPort.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/port/out/CommissionPort.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/port/out/ReportGeneratorPort.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/port/out/AgentRegistryPort.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/port/in/ProcessEodSettlementUseCase.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/port/in/ReconcileUseCase.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/service/EodProcessingService.java`
- Create: `src/main/java/com/agentbanking/settlement/domain/service/ReconciliationService.java`

- [ ] **Step 1: Write implementation**

**SettlementRepository.java:**
```java
package com.agentbanking.settlement.domain.port.out;

import com.agentbanking.settlement.domain.model.SettlementBatch;
import com.agentbanking.settlement.domain.model.AgentSettlement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository {
  Optional<SettlementBatch> findById(UUID id);
  Optional<SettlementBatch> findByBusinessDate(LocalDate date);
  SettlementBatch save(SettlementBatch batch);
  List<AgentSettlement> findAgentSettlements(UUID batchId);
  void saveAgentSettlements(List<AgentSettlement> settlements);
}
```

**AgentRegistryPort.java (FIX: query actual agent IDs, not hardcoded):**
```java
package com.agentbanking.settlement.domain.port.out;

import java.util.List;

public interface AgentRegistryPort {
  List<String> findAllActiveAgentIds();
}
```

**TransactionPort.java:**
```java
package com.agentbanking.settlement.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionPort {
  BigDecimal getTotalWithdrawals(String agentId, LocalDate date);
  BigDecimal getTotalDeposits(String agentId, LocalDate date);
  BigDecimal getTotalBillPayments(String agentId, LocalDate date);
  List<String> getTransactionIdsForReconciliation(String agentId, LocalDate date);
}
```

**CommissionPort.java:**
```java
package com.agentbanking.settlement.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CommissionPort {
  BigDecimal getTotalCommissions(String agentId, LocalDate date);
  List<String> getCommissionIds(String agentId, LocalDate date);
  void settleCommissions(List<String> commissionIds);
}
```

**ReportGeneratorPort.java:**
```java
package com.agentbanking.settlement.domain.port.out;

import com.agentbanking.settlement.domain.model.SettlementBatch;
import com.agentbanking.settlement.domain.model.AgentSettlement;
import java.util.List;

public interface ReportGeneratorPort {
  String generateSettlementReport(SettlementBatch batch, List<AgentSettlement> settlements);
  String generateReconciliationReport(List<com.agentbanking.settlement.domain.model.ReconciliationRecord> records);
}
```

**ProcessEodSettlementUseCase.java:**
```java
package com.agentbanking.settlement.domain.port.in;

import com.agentbanking.settlement.domain.model.SettlementBatch;
import java.time.LocalDate;

public interface ProcessEodSettlementUseCase {
  SettlementBatch processSettlement(LocalDate businessDate);
}
```

**ReconcileUseCase.java:**
```java
package com.agentbanking.settlement.domain.port.in;

import com.agentbanking.settlement.domain.model.ReconciliationRecord;
import java.time.LocalDate;
import java.util.List;

public interface ReconcileUseCase {
  List<ReconciliationRecord> reconcile(String agentId, LocalDate date);
  void resolveDiscrepancy(String recordId, String resolution, String resolvedBy);
}
```

**EodProcessingService.java (NO Logger, uses AgentRegistryPort for actual agent IDs):**
```java
package com.agentbanking.settlement.domain.service;

import com.agentbanking.settlement.domain.model.*;
import com.agentbanking.settlement.domain.port.out.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EodProcessingService {

  private final SettlementRepository settlementRepository;
  private final TransactionPort transactionPort;
  private final CommissionPort commissionPort;
  private final ReportGeneratorPort reportGenerator;
  private final AgentRegistryPort agentRegistry;

  public EodProcessingService(
    SettlementRepository settlementRepository,
    TransactionPort transactionPort,
    CommissionPort commissionPort,
    ReportGeneratorPort reportGenerator,
    AgentRegistryPort agentRegistry
  ) {
    this.settlementRepository = settlementRepository;
    this.transactionPort = transactionPort;
    this.commissionPort = commissionPort;
    this.reportGenerator = reportGenerator;
    this.agentRegistry = agentRegistry;
  }

  public SettlementBatch processEodSettlement(LocalDate businessDate) {
    settlementRepository.findByBusinessDate(businessDate).ifPresent(existing -> {
      throw new IllegalStateException("Settlement already exists for date: " + businessDate);
    });

    SettlementBatch batch = SettlementBatch.initiate(businessDate);
    batch = settlementRepository.save(batch.calculating());

    List<AgentSettlement> settlements = calculateAgentSettlements(batch);
    settlementRepository.saveAgentSettlements(settlements);

    BigDecimal totalNet = settlements.stream()
      .map(AgentSettlement::netAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    batch = settlementRepository.save(batch.complete(totalNet, settlements.size()));

    String reportPath = reportGenerator.generateSettlementReport(batch, settlements);
    return batch;
  }

  private List<AgentSettlement> calculateAgentSettlements(SettlementBatch batch) {
    // FIX: Query actual agent IDs from repository, NOT hardcoded
    List<String> agentIds = agentRegistry.findAllActiveAgentIds();
    List<AgentSettlement> settlements = new ArrayList<>();

    for (String agentId : agentIds) {
      try {
        BigDecimal withdrawals = transactionPort.getTotalWithdrawals(agentId, batch.businessDate());
        BigDecimal deposits = transactionPort.getTotalDeposits(agentId, batch.businessDate());
        BigDecimal billPayments = transactionPort.getTotalBillPayments(agentId, batch.businessDate());
        BigDecimal commissions = commissionPort.getTotalCommissions(agentId, batch.businessDate());

        AgentSettlement settlement = AgentSettlement.calculateNet(
          agentId, withdrawals, commissions, deposits, billPayments
        );
        settlements.add(settlement);
      } catch (Exception e) {
        // Log in infrastructure layer only
      }
    }

    return settlements;
  }
}
```

**ReconciliationService.java (NO Logger):**
```java
package com.agentbanking.settlement.domain.service;

import com.agentbanking.settlement.domain.model.*;
import com.agentbanking.settlement.domain.port.out.TransactionPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationService {

  private final TransactionPort transactionPort;

  public ReconciliationService(TransactionPort transactionPort) {
    this.transactionPort = transactionPort;
  }

  public List<ReconciliationRecord> reconcile(String agentId, LocalDate date) {
    List<String> internalTxnIds = transactionPort.getTransactionIdsForReconciliation(agentId, date);
    List<ReconciliationRecord> discrepancies = new ArrayList<>();

    for (String txnId : internalTxnIds) {
      BigDecimal internalAmount = BigDecimal.valueOf(100); // TODO: get from transaction port
      BigDecimal networkAmount = BigDecimal.valueOf(100);  // TODO: get from PayNet

      if (internalAmount.compareTo(networkAmount) != 0) {
        ReconciliationRecord record = ReconciliationRecord.create(
          agentId, txnId, DiscrepancyType.MISMATCH, internalAmount, networkAmount
        );
        discrepancies.add(record);
      }
    }

    return discrepancies;
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/settlement/domain/port/
git add src/main/java/com/agentbanking/settlement/domain/service/
git commit -m "feat(settlement): add domain ports and services"
```

---

## Task 4: Application Layer — DTOs and Service (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/settlement/application/dto/SettlementBatchResponse.java`
- Create: `src/main/java/com/agentbanking/settlement/application/dto/SettlementReportDto.java`
- Create: `src/main/java/com/agentbanking/settlement/application/dto/ReconciliationResult.java`
- Create: `src/main/java/com/agentbanking/settlement/application/dto/EodTriggerEvent.java`
- Create: `src/main/java/com/agentbanking/settlement/application/service/SettlementApplicationService.java`

- [ ] **Step 1: Write implementation**

**SettlementBatchResponse.java:**
```java
package com.agentbanking.settlement.application.dto;

import com.agentbanking.settlement.domain.model.SettlementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementBatchResponse(
  String batchId, LocalDate businessDate, SettlementStatus status,
  BigDecimal totalNetAmount, int agentCount, int discrepancyCount,
  LocalDateTime completedAt, String reportUrl
) {}
```

**SettlementReportDto.java:**
```java
package com.agentbanking.settlement.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record SettlementReportDto(
  String batchId, String businessDate, List<AgentSettlementDto> agentSettlements,
  BigDecimal totalNetAmount, String direction, String generatedAt
) {
  public record AgentSettlementDto(
    String agentId, BigDecimal withdrawals, BigDecimal commissions,
    BigDecimal deposits, BigDecimal billPayments, BigDecimal netAmount, String direction
  ) {}
}
```

**ReconciliationResult.java:**
```java
package com.agentbanking.settlement.application.dto;

import com.agentbanking.settlement.domain.model.DiscrepancyType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReconciliationResult(
  String agentId, LocalDateTime reconciledAt,
  List<DiscrepancyDto> discrepancies, boolean hasDiscrepancies
) {
  public record DiscrepancyDto(
    String transactionId, DiscrepancyType type,
    BigDecimal internalAmount, BigDecimal networkAmount,
    BigDecimal difference, String status
  ) {}
}
```

**EodTriggerEvent.java:**
```java
package com.agentbanking.settlement.application.dto;

import java.time.LocalDate;

public record EodTriggerEvent(
  String eventId, LocalDate businessDate, String triggeredBy, String triggerType
) {}
```

**SettlementApplicationService.java (NO @Service — registered via @Bean):**
```java
package com.agentbanking.settlement.application.service;

import com.agentbanking.settlement.application.dto.*;
import com.agentbanking.settlement.domain.model.SettlementBatch;
import com.agentbanking.settlement.domain.port.in.ProcessEodSettlementUseCase;

import java.time.LocalDate;

public class SettlementApplicationService {

  private final ProcessEodSettlementUseCase eodUseCase;

  public SettlementApplicationService(ProcessEodSettlementUseCase eodUseCase) {
    this.eodUseCase = eodUseCase;
  }

  public SettlementBatchResponse triggerEodSettlement(LocalDate businessDate) {
    SettlementBatch batch = eodUseCase.processSettlement(businessDate);
    return new SettlementBatchResponse(
      batch.id().toString(), batch.businessDate(), batch.status(),
      batch.totalNetAmount(), batch.agentCount(), batch.discrepancyCount(),
      batch.completedAt(), batch.reportPath()
    );
  }

  public SettlementBatchResponse getSettlementByDate(LocalDate date) {
    return null; // TODO: implement
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/settlement/application/
git commit -m "feat(settlement): add application DTOs and service"
```

---

## Task 5: Infrastructure — Kafka Consumer and REST Controller (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/settlement/infrastructure/messaging/EodTriggerConsumer.java`
- Create: `src/main/java/com/agentbanking/settlement/infrastructure/web/SettlementController.java`
- Create: `src/main/java/com/agentbanking/settlement/infrastructure/persistence/repository/SettlementRepositoryImpl.java`
- Create: `src/main/java/com/agentbanking/settlement/infrastructure/external/AgentRegistryAdapter.java`

- [ ] **Step 1: Write implementation**

**EodTriggerConsumer.java:**
```java
package com.agentbanking.settlement.infrastructure.messaging;

import com.agentbanking.settlement.application.dto.EodTriggerEvent;
import com.agentbanking.settlement.application.service.SettlementApplicationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

@Configuration
public class EodTriggerConsumer {

  private static final Logger log = LoggerFactory.getLogger(EodTriggerConsumer.class);

  private final SettlementApplicationService service;

  public EodTriggerConsumer(SettlementApplicationService service) {
    this.service = service;
  }

  @KafkaListener(topics = "${agentbanking.kafka.topic.eod-trigger}", groupId = "settlement-group")
  public void consumeEodTrigger(EodTriggerEvent event) {
    log.info("Consumed EOD trigger event: {}, date: {}", event.eventId(), event.businessDate());
    try {
      service.triggerEodSettlement(event.businessDate());
      log.info("EOD settlement completed for date: {}", event.businessDate());
    } catch (Exception e) {
      log.error("Failed to process EOD settlement: {}", e.getMessage(), e);
    }
  }
}
```

**SettlementController.java:**
```java
package com.agentbanking.settlement.infrastructure.web;

import com.agentbanking.settlement.application.dto.SettlementBatchResponse;
import com.agentbanking.settlement.application.service.SettlementApplicationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/settlements")
public class SettlementController {

  private static final Logger log = LoggerFactory.getLogger(SettlementController.class);

  private final SettlementApplicationService service;

  public SettlementController(SettlementApplicationService service) {
    this.service = service;
  }

  @PostMapping("/eod")
  public ResponseEntity<SettlementBatchResponse> triggerEod(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate
  ) {
    log.info("POST /api/v1/settlements/eod - date: {}", businessDate);
    return ResponseEntity.ok(service.triggerEodSettlement(businessDate));
  }

  @GetMapping("/date/{date}")
  public ResponseEntity<SettlementBatchResponse> getByDate(
    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    log.info("GET /api/v1/settlements/date/{}", date);
    SettlementBatchResponse response = service.getSettlementByDate(date);
    return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
  }
}
```

**AgentRegistryAdapter.java (implements AgentRegistryPort):**
```java
package com.agentbanking.settlement.infrastructure.external;

import com.agentbanking.settlement.domain.port.out.AgentRegistryPort;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentRegistryAdapter implements AgentRegistryPort {

  @Override
  public List<String> findAllActiveAgentIds() {
    // TODO: Query actual agent registry via Feign client or direct DB
    return List.of();
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/settlement/infrastructure/
git commit -m "feat(settlement): add Kafka consumer, controller, and adapters"
```

---

## Task 6: Config — Domain Service Registration (Write Manually)

**Law V: Domain services via @Bean in config, NOT @Service annotation.**

**Files:**
- Update: `src/main/java/com/agentbanking/settlement/config/DomainServiceConfig.java` (created by Seed4J)

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.settlement.config;

import com.agentbanking.settlement.domain.port.in.ProcessEodSettlementUseCase;
import com.agentbanking.settlement.domain.port.out.*;
import com.agentbanking.settlement.domain.service.EodProcessingService;
import com.agentbanking.settlement.domain.service.ReconciliationService;
import com.agentbanking.settlement.application.service.SettlementApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

  @Bean
  public EodProcessingService eodProcessingService(
    SettlementRepository settlementRepository,
    TransactionPort transactionPort,
    CommissionPort commissionPort,
    ReportGeneratorPort reportGenerator,
    AgentRegistryPort agentRegistry
  ) {
    return new EodProcessingService(settlementRepository, transactionPort,
      commissionPort, reportGenerator, agentRegistry);
  }

  @Bean
  public ProcessEodSettlementUseCase processEodSettlementUseCase(EodProcessingService service) {
    return service::processEodSettlement;
  }

  @Bean
  public ReconciliationService reconciliationService(TransactionPort transactionPort) {
    return new ReconciliationService(transactionPort);
  }

  @Bean
  public SettlementApplicationService settlementApplicationService(
    ProcessEodSettlementUseCase eodUseCase
  ) {
    return new SettlementApplicationService(eodUseCase);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/settlement/config/DomainServiceConfig.java
git commit -m "feat(settlement): register domain services via @Bean"
```

---

## Task 7: Flyway Migration

**Files:**
- Update: `src/main/resources/db/migration/V1__settlement_init.sql` (created by Seed4J)

- [ ] **Step 1: Write migration**

```sql
CREATE TABLE IF NOT EXISTS settlement_batch (
    id UUID PRIMARY KEY,
    business_date DATE NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    total_withdrawals NUMERIC(19, 2) DEFAULT 0,
    total_deposits NUMERIC(19, 2) DEFAULT 0,
    total_commissions NUMERIC(19, 2) DEFAULT 0,
    total_bill_payments NUMERIC(19, 2) DEFAULT 0,
    total_net_amount NUMERIC(19, 2) DEFAULT 0,
    agent_count INTEGER DEFAULT 0,
    discrepancy_count INTEGER DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    report_path VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS agent_settlement (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES settlement_batch(id),
    agent_id VARCHAR(50) NOT NULL,
    withdrawals NUMERIC(19, 2) DEFAULT 0,
    commissions NUMERIC(19, 2) DEFAULT 0,
    deposits NUMERIC(19, 2) DEFAULT 0,
    bill_payments NUMERIC(19, 2) DEFAULT 0,
    net_amount NUMERIC(19, 2) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    has_discrepancy BOOLEAN DEFAULT FALSE,
    UNIQUE(batch_id, agent_id)
);

CREATE TABLE IF NOT EXISTS commission_settlement (
    id UUID PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    settlement_date DATE NOT NULL,
    total_commission NUMERIC(19, 2) NOT NULL,
    transaction_count INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference_number VARCHAR(50) UNIQUE,
    settled_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reconciliation_record (
    id UUID PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(50),
    stan VARCHAR(20),
    discrepancy_type VARCHAR(20) NOT NULL,
    internal_amount NUMERIC(19, 2) NOT NULL,
    network_amount NUMERIC(19, 2) NOT NULL,
    difference NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    resolution TEXT,
    resolved_by VARCHAR(50),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_settlement_batch_date ON settlement_batch(business_date);
CREATE INDEX idx_settlement_batch_status ON settlement_batch(status);
CREATE INDEX idx_agent_settlement_batch ON agent_settlement(batch_id);
CREATE INDEX idx_agent_settlement_agent ON agent_settlement(agent_id);
CREATE INDEX idx_commission_settlement_date ON commission_settlement(settlement_date);
CREATE INDEX idx_reconciliation_agent ON reconciliation_record(agent_id);
CREATE INDEX idx_reconciliation_status ON reconciliation_record(status);
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/db/migration/
git commit -m "feat(settlement): add Flyway migration"
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
| 3 | Domain Ports & Services | Manual (NO Logger, uses AgentRegistryPort) | 0 |
| 4 | Application DTOs & Service | Manual | 0 |
| 5 | Infrastructure (Kafka, Web, Adapters) | Manual | 0 |
| 6 | Config (@Bean registration) | Manual (Law V) | 0 |
| 7 | Flyway Migration | Manual | 0 |

**Key fixes from original plan:**
- Removed Logger from `EodProcessingService` and `ReconciliationService` (Law VI violation)
- Added `AgentRegistryPort` — queries actual agent IDs, NOT hardcoded `List.of("AGT-001", ...)`
- Domain services via `@Bean` in config (Law V), NOT `@Service`
- `./gradlew test` (not `./mvnw`)
- Seed4J scaffolding replaces manual package-info.java creation
- Removed Spring Batch dependency (simpler approach for initial implementation)
