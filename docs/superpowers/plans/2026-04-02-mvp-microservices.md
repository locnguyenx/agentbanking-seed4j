# MVP Microservices Scaffolding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold 5 MVP microservices with hexagonal architecture following Seed4J patterns

**Architecture:** Each service follows hexagonal (Ports & Adapters) pattern: domain/ (zero framework imports), application/ (use case orchestration), infrastructure/ (adapters). Services communicate via REST (sync) and Kafka (async).

**Tech Stack:** Java 25, Spring Boot 4, Gradle (Kotlin DSL), PostgreSQL, Flyway, Kafka, Axon Framework, ArchUnit

---

## File Structure

```
src/main/java/com/agentbanking/
├── rules/                          # Rules Service
│   ├── application/
│   │   └── RulesApplicationService.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── FeeConfig.java
│   │   │   └── VelocityRule.java
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   └── CalculateFeePort.java
│   │   │   └── out/
│   │   │       └── FeeConfigRepository.java
│   │   └── service/
│   │       └── FeeCalculationService.java
│   └── infrastructure/
│       ├── primary/
│       │   └── RulesResource.java
│       └── secondary/
│           └── DatabaseFeeConfigRepository.java
├── ledger/                         # Ledger & Float Service
│   ├── application/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── AgentFloat.java
│   │   │   └── JournalEntry.java
│   │   ├── port/
│   │   │   ├── in/
│   │   │   └── out/
│   │   └── service/
│   └── infrastructure/
├── onboarding/                     # Onboarding Service
│   ├── application/
│   ├── domain/
│   │   ├── model/
│   │   │   └── KycVerification.java
│   │   ├── port/
│   │   │   ├── in/
│   │   │   └── out/
│   │   └── service/
│   └── infrastructure/
├── switch/                         # Switch Adapter Service
│   ├── application/
│   ├── domain/
│   │   ├── model/
│   │   │   └── IsoMessage.java
│   │   ├── port/
│   │   │   ├── in/
│   │   │   └── out/
│   │   └── service/
│   └── infrastructure/
└── biller/                         # Biller Service
    ├── application/
    ├── domain/
    │   ├── model/
    │   │   └── BillPayment.java
    │   ├── port/
    │   │   ├── in/
    │   │   └── out/
    │   └── service/
    └── infrastructure/
```

---

## Task 1: Rules Service

**BDD Scenarios:** Implements BDD Scenario 5: Add Spring Boot with Hexagonal Architecture

**BRD Requirements:** Fulfills US-S02: Generate MVP services with hexagonal architecture

**Files:**
- Create: `src/main/java/com/agentbanking/rules/application/RulesApplicationService.java`
- Create: `src/main/java/com/agentbanking/rules/domain/model/FeeConfig.java`
- Create: `src/main/java/com/agentbanking/rules/domain/model/VelocityRule.java`
- Create: `src/main/java/com/agentbanking/rules/domain/port/in/CalculateFeePort.java`
- Create: `src/main/java/com/agentbanking/rules/domain/port/out/FeeConfigRepository.java`
- Create: `src/main/java/com/agentbanking/rules/domain/service/FeeCalculationService.java`
- Create: `src/main/java/com/agentbanking/rules/infrastructure/primary/RulesResource.java`
- Create: `src/main/java/com/agentbanking/rules/infrastructure/secondary/DatabaseFeeConfigRepository.java`
- Create: `src/main/resources/db/migration/V20260402090000__rules_init.sql`
- Create: `src/test/java/com/agentbanking/rules/HexagonalArchTest.java`

- [ ] **Step 1: Create domain model (FeeConfig)**

```java
package com.agentbanking.rules.domain.model;

import java.math.BigDecimal;

public record FeeConfig(
    String id,
    String agentTier,
    BigDecimal fixedFee,
    BigDecimal percentageFee,
    BigDecimal minFee,
    BigDecimal maxFee
) {}
```

- [ ] **Step 2: Create domain model (VelocityRule)**

```java
package com.agentbanking.rules.domain.model;

public record VelocityRule(
    String id,
    int maxTransactionsPerDay,
    int maxTransactionsPerHour,
    BigDecimal maxAmountPerDay
) {}
```

- [ ] **Step 3: Create inbound port (CalculateFeePort)**

```java
package com.agentbanking.rules.domain.port.in;

import com.agentbanking.rules.domain.model.FeeConfig;
import java.math.BigDecimal;

public interface CalculateFeePort {
    FeeCalculationResult calculateFee(String agentTier, BigDecimal transactionAmount);

    record FeeCalculationResult(
        BigDecimal customerFee,
        BigDecimal agentCommission,
        BigDecimal bankShare
    ) {}
}
```

- [ ] **Step 4: Create outbound port (FeeConfigRepository)**

```java
package com.agentbanking.rules.domain.port.out;

import com.agentbanking.rules.domain.model.FeeConfig;
import java.util.Optional;

public interface FeeConfigRepository {
    Optional<FeeConfig> findByAgentTier(String agentTier);
}
```

- [ ] **Step 5: Create domain service (FeeCalculationService)**

```java
package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.port.in.CalculateFeePort;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class FeeCalculationService implements CalculateFeePort {

    private final FeeConfigRepository feeConfigRepository;

    public FeeCalculationService(FeeConfigRepository feeConfigRepository) {
        this.feeConfigRepository = feeConfigRepository;
    }

    @Override
    public FeeCalculationResult calculateFee(String agentTier, BigDecimal transactionAmount) {
        FeeConfig config = feeConfigRepository.findByAgentTier(agentTier)
            .orElseThrow(() -> new IllegalArgumentException("No fee config for tier: " + agentTier));

        BigDecimal percentageFee = transactionAmount.multiply(config.percentageFee())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalFee = config.fixedFee().add(percentageFee);
        totalFee = totalFee.max(config.minFee()).min(config.maxFee());

        BigDecimal agentCommission = totalFee.multiply(BigDecimal.valueOf(0.3));
        BigDecimal bankShare = totalFee.subtract(agentCommission);

        return new FeeCalculationResult(totalFee, agentCommission, bankShare);
    }
}
```

- [ ] **Step 6: Create REST controller (RulesResource)**

```java
package com.agentbanking.rules.infrastructure.primary;

import com.agentbanking.rules.domain.port.in.CalculateFeePort;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rules")
class RulesResource {

    private final CalculateFeePort calculateFeePort;

    RulesResource(CalculateFeePort calculateFeePort) {
        this.calculateFeePort = calculateFeePort;
    }

    @PostMapping("/calculate-fee")
    CalculateFeePort.FeeCalculationResult calculateFee(
        @RequestParam String agentTier,
        @RequestParam BigDecimal transactionAmount
    ) {
        return calculateFeePort.calculateFee(agentTier, transactionAmount);
    }
}
```

- [ ] **Step 7: Create Flyway migration**

```sql
CREATE TABLE fee_config (
    id VARCHAR(36) PRIMARY KEY,
    agent_tier VARCHAR(50) NOT NULL,
    fixed_fee DECIMAL(10,2) NOT NULL,
    percentage_fee DECIMAL(5,4) NOT NULL,
    min_fee DECIMAL(10,2) NOT NULL,
    max_fee DECIMAL(10,2) NOT NULL
);

CREATE TABLE velocity_rule (
    id VARCHAR(36) PRIMARY KEY,
    max_transactions_per_day INT NOT NULL,
    max_transactions_per_hour INT NOT NULL,
    max_amount_per_day DECIMAL(15,2) NOT NULL
);
```

- [ ] **Step 8: Create ArchUnit test**

```java
package com.agentbanking.rules;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchTest {

    @Test
    void domain_should_not_depend_on_spring() {
        noClasses()
            .that().resideInAPackage("..rules.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
            .check(new ClassFileImporter().importPackages("com.agentbanking.rules"));
    }
}
```

- [ ] **Step 9: Run tests**

Run: `./gradlew test --tests "*rules*"`

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/agentbanking/rules/ src/test/java/com/agentbanking/rules/ src/main/resources/db/migration/V20260402090000__rules_init.sql
git commit -m "feat(rules): scaffold Rules Service with hexagonal architecture"
```

---

## Task 2: Ledger & Float Service

**Files:**
- Create: `src/main/java/com/agentbanking/ledger/` (full hexagonal structure)
- Create: `src/main/resources/db/migration/V20260402090001__ledger_init.sql`

- [ ] **Step 1: Create domain model (AgentFloat)**

```java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;

public record AgentFloat(
    String agentId,
    BigDecimal balance,
    BigDecimal blockedAmount,
    BigDecimal availableBalance
) {
    public AgentFloat block(BigDecimal amount) {
        return new AgentFloat(agentId, balance, blockedAmount.add(amount), availableBalance.subtract(amount));
    }

    public AgentFloat release(BigDecimal amount) {
        return new AgentFloat(agentId, balance, blockedAmount.subtract(amount), availableBalance.add(amount));
    }

    public AgentFloat commit(BigDecimal amount) {
        return new AgentFloat(agentId, balance.subtract(amount), blockedAmount.subtract(amount), availableBalance);
    }
}
```

- [ ] **Step 2: Create domain model (JournalEntry)**

```java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JournalEntry(
    String id,
    String transactionId,
    String accountDebit,
    String accountCredit,
    BigDecimal amount,
    LocalDateTime timestamp
) {}
```

- [ ] **Step 3: Create inbound ports**

```java
package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;

public interface FloatPort {
    BigDecimal getBalance(String agentId);
    void block(String agentId, BigDecimal amount);
    void commit(String agentId, BigDecimal amount);
    void release(String agentId, BigDecimal amount);
}

public interface JournalPort {
    void recordTransaction(String transactionId, String accountDebit, String accountCredit, BigDecimal amount);
}
```

- [ ] **Step 4: Create outbound ports**

```java
package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.AgentFloat;
import com.agentbanking.ledger.domain.model.JournalEntry;
import java.util.Optional;

public interface FloatRepository {
    Optional<AgentFloat> findByAgentId(String agentId);
    void save(AgentFloat agentFloat);
}

public interface JournalRepository {
    void save(JournalEntry entry);
}
```

- [ ] **Step 5: Create domain service**

```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.AgentFloat;
import com.agentbanking.ledger.domain.model.JournalEntry;
import com.agentbanking.ledger.domain.port.in.FloatPort;
import com.agentbanking.ledger.domain.port.in.JournalPort;
import com.agentbanking.ledger.domain.port.out.FloatRepository;
import com.agentbanking.ledger.domain.port.out.JournalRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerService implements FloatPort, JournalPort {

    private final FloatRepository floatRepository;
    private final JournalRepository journalRepository;

    public LedgerService(FloatRepository floatRepository, JournalRepository journalRepository) {
        this.floatRepository = floatRepository;
        this.journalRepository = journalRepository;
    }

    @Override
    public BigDecimal getBalance(String agentId) {
        return floatRepository.findByAgentId(agentId)
            .map(AgentFloat::availableBalance)
            .orElse(BigDecimal.ZERO);
    }

    @Override
    public void block(String agentId, BigDecimal amount) {
        AgentFloat agentFloat = floatRepository.findByAgentId(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
        floatRepository.save(agentFloat.block(amount));
    }

    @Override
    public void commit(String agentId, BigDecimal amount) {
        AgentFloat agentFloat = floatRepository.findByAgentId(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
        floatRepository.save(agentFloat.commit(amount));
    }

    @Override
    public void release(String agentId, BigDecimal amount) {
        AgentFloat agentFloat = floatRepository.findByAgentId(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
        floatRepository.save(agentFloat.release(amount));
    }

    @Override
    public void recordTransaction(String transactionId, String accountDebit, String accountCredit, BigDecimal amount) {
        JournalEntry entry = new JournalEntry(
            UUID.randomUUID().toString(),
            transactionId,
            accountDebit,
            accountCredit,
            amount,
            LocalDateTime.now()
        );
        journalRepository.save(entry);
    }
}
```

- [ ] **Step 6: Create REST controller**

```java
package com.agentbanking.ledger.infrastructure.primary;

import com.agentbanking.ledger.domain.port.in.FloatPort;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ledger")
class LedgerResource {

    private final FloatPort floatPort;

    LedgerResource(FloatPort floatPort) {
        this.floatPort = floatPort;
    }

    @GetMapping("/balance/{agentId}")
    BigDecimal getBalance(@PathVariable String agentId) {
        return floatPort.getBalance(agentId);
    }

    @PostMapping("/block")
    void block(@RequestParam String agentId, @RequestParam BigDecimal amount) {
        floatPort.block(agentId, amount);
    }

    @PostMapping("/commit")
    void commit(@RequestParam String agentId, @RequestParam BigDecimal amount) {
        floatPort.commit(agentId, amount);
    }
}
```

- [ ] **Step 7: Create Flyway migration**

```sql
CREATE TABLE agent_float (
    agent_id VARCHAR(50) PRIMARY KEY,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    blocked_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    available_balance DECIMAL(15,2) NOT NULL DEFAULT 0
);

CREATE TABLE journal_entry (
    id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL,
    account_debit VARCHAR(50) NOT NULL,
    account_credit VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    timestamp TIMESTAMP NOT NULL
);

CREATE INDEX idx_journal_transaction ON journal_entry(transaction_id);
```

- [ ] **Step 8: Run tests**

Run: `./gradlew test --tests "*ledger*"`

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/agentbanking/ledger/ src/test/java/com/agentbanking/ledger/ src/main/resources/db/migration/V20260402090001__ledger_init.sql
git commit -m "feat(ledger): scaffold Ledger & Float Service with hexagonal architecture"
```

---

## Task 3: Onboarding Service

**Files:**
- Create: `src/main/java/com/agentbanking/onboarding/` (full hexagonal structure)
- Create: `src/main/resources/db/migration/V20260402090002__onboarding_init.sql`

- [ ] **Step 1: Create domain model (KycVerification)**

```java
package com.agentbanking.onboarding.domain.model;

import java.time.LocalDateTime;

public record KycVerification(
    String id,
    String mykadNumber,
    String name,
    VerificationStatus status,
    LocalDateTime verifiedAt
) {
    public enum VerificationStatus {
        PENDING, MATCHED, MISMATCHED, AML_FAILED, AUTO_APPROVED, MANUAL_REVIEW
    }
}
```

- [ ] **Step 2: Create inbound ports**

```java
package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.KycVerification;

public interface KycPort {
    KycVerification verifyMyKad(String mykadNumber);
    KycVerification processVerificationResult(String verificationId, boolean match, boolean amlClean, int age);
}

public interface JpnGatewayPort {
    JpnVerificationResult queryJpn(String mykadNumber);

    record JpnVerificationResult(
        boolean match,
        boolean amlClean,
        int age,
        String name
    ) {}
}
```

- [ ] **Step 3: Create domain service**

```java
package com.agentbanking.onboarding.domain.service;

import com.agentbanking.onboarding.domain.model.KycVerification;
import com.agentbanking.onboarding.domain.model.KycVerification.VerificationStatus;
import com.agentbanking.onboarding.domain.port.in.JpnGatewayPort;
import com.agentbanking.onboarding.domain.port.in.KycPort;
import com.agentbanking.onboarding.domain.port.out.KycVerificationRepository;
import java.time.LocalDateTime;
import java.util.UUID;

public class KycService implements KycPort {

    private final JpnGatewayPort jpnGatewayPort;
    private final KycVerificationRepository repository;

    public KycService(JpnGatewayPort jpnGatewayPort, KycVerificationRepository repository) {
        this.jpnGatewayPort = jpnGatewayPort;
        this.repository = repository;
    }

    @Override
    public KycVerification verifyMyKad(String mykadNumber) {
        JpnGatewayPort.JpnVerificationResult result = jpnGatewayPort.queryJpn(mykadNumber);

        VerificationStatus status;
        if (result.match() && result.amlClean() && result.age() >= 18) {
            status = VerificationStatus.AUTO_APPROVED;
        } else if (result.match() && result.amlClean()) {
            status = VerificationStatus.MANUAL_REVIEW;
        } else if (!result.match()) {
            status = VerificationStatus.MISMATCHED;
        } else {
            status = VerificationStatus.AML_FAILED;
        }

        KycVerification verification = new KycVerification(
            UUID.randomUUID().toString(), mykadNumber, result.name(), status, LocalDateTime.now()
        );
        repository.save(verification);
        return verification;
    }

    @Override
    public KycVerification processVerificationResult(String verificationId, boolean match, boolean amlClean, int age) {
        // Implementation for manual review processing
        throw new UnsupportedOperationException("Not implemented");
    }
}
```

- [ ] **Step 4: Create Flyway migration**

```sql
CREATE TABLE kyc_verification (
    id VARCHAR(36) PRIMARY KEY,
    mykad_number VARCHAR(12) NOT NULL,
    name VARCHAR(200),
    status VARCHAR(20) NOT NULL,
    verified_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_kyc_mykad ON kyc_verification(mykad_number);
```

- [ ] **Step 5: Run tests and commit**

```bash
./gradlew test --tests "*onboarding*"
git add src/main/java/com/agentbanking/onboarding/ src/test/java/com/agentbanking/onboarding/ src/main/resources/db/migration/V20260402090002__onboarding_init.sql
git commit -m "feat(onboarding): scaffold Onboarding Service with hexagonal architecture"
```

---

## Task 4: Switch Adapter Service

**Files:**
- Create: `src/main/java/com/agentbanking/switch/` (full hexagonal structure)
- Create: `src/main/resources/db/migration/V20260402090003__switch_init.sql`

- [ ] **Step 1: Create domain model (IsoMessage)**

```java
package com.agentbanking.switch.domain.model;

import java.time.LocalDateTime;

public record IsoMessage(
    String id,
    String mti,
    String processingCode,
    String pan,
    LocalDateTime transmissionDateTime,
    String responseCode,
    MessageStatus status
) {
    public enum MessageStatus {
        PENDING, SENT, RECEIVED, FAILED, REVERSED
    }
}
```

- [ ] **Step 2: Create inbound/outbound ports**

```java
package com.agentbanking.switch.domain.port.in;

import com.agentbanking.switch.domain.model.IsoMessage;

public interface SwitchPort {
    IsoMessage sendTransaction(IsoMessage message);
    IsoMessage reverseTransaction(String transactionId);
}

package com.agentbanking.switch.domain.port.out;

import com.agentbanking.switch.domain.model.IsoMessage;

public interface IsoTranslationPort {
    String encode(IsoMessage message);
    IsoMessage decode(String rawData);
}

public interface NetworkManagementPort {
    void sendEchoTest();
    void processNetworkMessage(String message);
}
```

- [ ] **Step 3: Create Flyway migration**

```sql
CREATE TABLE iso_message (
    id VARCHAR(36) PRIMARY KEY,
    mti VARCHAR(4) NOT NULL,
    processing_code VARCHAR(6),
    pan VARCHAR(20),
    transmission_datetime TIMESTAMP,
    response_code VARCHAR(2),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_iso_status ON iso_message(status);
```

- [ ] **Step 4: Run tests and commit**

```bash
./gradlew test --tests "*switch*"
git add src/main/java/com/agentbanking/switch/ src/test/java/com/agentbanking/switch/ src/main/resources/db/migration/V20260402090003__switch_init.sql
git commit -m "feat(switch): scaffold Switch Adapter Service with hexagonal architecture"
```

---

## Task 5: Biller Service

**Files:**
- Create: `src/main/java/com/agentbanking/biller/` (full hexagonal structure)
- Create: `src/main/resources/db/migration/V20260402090004__biller_init.sql`

- [ ] **Step 1: Create domain model (BillPayment)**

```java
package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillPayment(
    String id,
    String billerCode,
    String accountNumber,
    BigDecimal amount,
    PaymentStatus status,
    LocalDateTime paidAt
) {
    public enum PaymentStatus {
        PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED
    }
}
```

- [ ] **Step 2: Create ports**

```java
package com.agentbanking.biller.domain.port.in;

import com.agentbanking.biller.domain.model.BillPayment;
import java.math.BigDecimal;

public interface BillerPort {
    BillPayment initiatePayment(String billerCode, String accountNumber, BigDecimal amount);
    BillPayment getPaymentStatus(String paymentId);
}

package com.agentbanking.biller.domain.port.out;

import com.agentbanking.biller.domain.model.BillPayment;

public interface BillerGatewayPort {
    BillPayment sendToBiller(String billerCode, String accountNumber, BigDecimal amount);
}
```

- [ ] **Step 3: Create Flyway migration**

```sql
CREATE TABLE bill_payment (
    id VARCHAR(36) PRIMARY KEY,
    biller_code VARCHAR(20) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at TIMESTAMP
);

CREATE INDEX idx_bill_biller ON bill_payment(biller_code);
```

- [ ] **Step 4: Run tests and commit**

```bash
./gradlew test --tests "*biller*"
git add src/main/java/com/agentbanking/biller/ src/test/java/com/agentbanking/biller/ src/main/resources/db/migration/V20260402090004__biller_init.sql
git commit -m "feat(biller): scaffold Biller Service with hexagonal architecture"
```

---

## Task 6: Final Verification

- [ ] **Step 1: Run all tests**

Run: `./gradlew test`

- [ ] **Step 2: Run ArchUnit tests**

Run: `./gradlew test --tests "*ArchTest*"`

- [ ] **Step 3: Verify build**

Run: `./gradlew build -x test`

- [ ] **Step 4: Commit final changes**

```bash
git add -A
git commit -m "feat: complete MVP microservices scaffolding"
```

---

## Summary

| Task | Service | Domain Models | User Stories |
|------|---------|---------------|--------------|
| 1 | Rules | FeeConfig, VelocityRule | US-R01, US-R02, US-R03, US-R04 |
| 2 | Ledger & Float | AgentFloat, JournalEntry | US-L01, US-L02, US-L03 |
| 3 | Onboarding | KycVerification | US-O01, US-O02, US-O03 |
| 4 | Switch Adapter | IsoMessage | US-V01 |
| 5 | Biller | BillPayment | US-BO01 |
| 6 | Verification | N/A | N/A |
