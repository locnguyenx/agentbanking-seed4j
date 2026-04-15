# Biller Adapter Implementation Plan

> **Bounding Context:** com.agentbanking.billeradapter
> **Wave:** 5.5
> **Depends On:** Plan 01 (Error Registry), Plan 02 (Domain Model), Plan 11 (Idempotency)
> **BDD Scenarios:** BDD-BG01, BDD-BG01-EC-01, BDD-BG01-EC-02, BDD-BG02
> **BRD Requirements:** FR-23.1, FR-23.2, FR-23.3, FR-23.4

**Goal:** Implement the Biller Adapter with bill inquiry, payment posting, payment reversal, and Kafka event publishing for async processing.

**Architecture:** Wave 5 — Tier 4 Adapter. Hexagonal architecture. Normalizes different biller APIs (TNB, Maxis, JomPAY, etc.) into a unified interface. Async event publishing via Kafka.

**Tech Stack:** Java 21, Spring Boot 4, Spring Cloud Stream (Kafka), Spring Cloud OpenFeign, JUnit 5, Mockito, ArchUnit, Gradle

---

## Task 1: Seed4J Scaffolding

Run Seed4J CLI to scaffold the biller adapter service with Kafka infrastructure:

```bash
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply spring-boot-kafka \
  --context billeradapter \
  --package com.agentbanking.billeradapter \
  --no-commit
```

This creates:
- Hexagonal package structure: `domain/`, `application/`, `infrastructure/`, `config/`
- `@BusinessContext` annotation on package-info
- `DomainServiceConfig.java` stub with `@Bean` registration pattern
- Flyway migration stub
- ArchUnit test: `HexagonalArchitectureTest.java`
- Test base classes and `@UnitTest` annotation
- Gradle build configuration with Kafka dependencies

- [ ] **Step 1: Run Seed4J scaffolding**
- [ ] **Step 2: Verify generated structure matches hexagonal layout**
- [ ] **Step 3: Commit scaffolding**

```bash
git add . && git commit -m "feat(billeradapter): scaffold with Seed4J spring-boot-kafka module"
```

---

## Task 2: Domain Models and Ports (Write Manually)

**CRITICAL: ZERO framework imports in domain/ — no Logger, no Spring, no JPA.**

**Files:**
- Create: `src/main/java/com/agentbanking/billeradapter/domain/model/BillType.java`
- Create: `src/main/java/com/agentbanking/billeradapter/domain/model/BillerStatus.java`
- Create: `src/main/java/com/agentbanking/billeradapter/domain/model/Biller.java`
- Create: `src/main/java/com/agentbanking/billeradapter/domain/model/BillerTransaction.java`
- Create: `src/main/java/com/agentbanking/billeradapter/domain/port/out/BillerGatewayPort.java` (with BillDetails, PaymentResult, ReversalResult records)
- Create: `src/main/java/com/agentbanking/billeradapter/domain/port/out/BillerRegistryPort.java`
- Create: `src/main/java/com/agentbanking/billeradapter/domain/port/in/GetBillUseCase.java`
- Create: `src/main/java/com/agentbanking/billeradapter/domain/port/in/PayBillUseCase.java`
- Create: `src/main/java/com/agentbanking/billeradapter/domain/port/in/ReverseBillUseCase.java`
- Create: `src/main/java/com/agentbanking/billeradapter/domain/service/BillPaymentService.java`

- [ ] **Step 1: Write tests**

```java
package com.agentbanking.billeradapter.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.agentbanking.UnitTest;
import com.agentbanking.billeradapter.domain.model.Biller;
import com.agentbanking.billeradapter.domain.model.BillType;
import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.BillerGatewayPort;
import com.agentbanking.billeradapter.domain.port.out.BillerRegistryPort;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;

@UnitTest
@ExtendWith(MockitoExtension.class)
class BillPaymentServiceTest {

  @Mock private BillerGatewayPort billerGatewayPort;
  @Mock private BillerRegistryPort billerRegistryPort;
  private BillPaymentService service;

  @BeforeEach
  void setUp() { service = new BillPaymentService(billerGatewayPort, billerRegistryPort); }

  @Nested
  @DisplayName("getBillDetails")
  class GetBillDetailsTest {
    @Test
    void shouldReturnBillDetails() {
      String billerCode = "TNB";
      String ref1 = "TNB-12345678";
      Biller biller = new Biller(billerCode, "Tenaga Nasional", BillType.UTILITY, "https://api.tnb.com", true, true);
      BillDetails details = new BillDetails(billerCode, ref1, "AHMAD BIN ABU", new BigDecimal("150.00"), "Electricity Bill");

      when(billerRegistryPort.findByCode(billerCode)).thenReturn(java.util.Optional.of(biller));
      when(billerGatewayPort.getBillDetails(billerCode, ref1, null)).thenReturn(details);

      BillDetails result = service.getBillDetails(billerCode, ref1, null);

      assertThat(result.customerName()).isEqualTo("AHMAD BIN ABU");
      assertThat(result.outstandingAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void shouldThrowForUnknownBiller() {
      when(billerRegistryPort.findByCode("UNKNOWN")).thenReturn(java.util.Optional.empty());
      assertThatThrownBy(() -> service.getBillDetails("UNKNOWN", "REF-123", null))
        .isInstanceOf(Exception.class);
    }
  }

  @Nested
  @DisplayName("payBill")
  class PayBillTest {
    @Test
    void shouldProcessPaymentSuccessfully() {
      String billerCode = "TNB";
      String ref1 = "TNB-12345678";
      BigDecimal amount = new BigDecimal("150.00");
      Biller biller = new Biller(billerCode, "Tenaga Nasional", BillType.UTILITY, "https://api.tnb.com", true, true);
      PaymentResult paymentResult = new PaymentResult(true, "TXN-001", "CONF-123", null);

      when(billerRegistryPort.findByCode(billerCode)).thenReturn(java.util.Optional.of(biller));
      when(billerGatewayPort.payBill(billerCode, ref1, null, amount)).thenReturn(paymentResult);

      PaymentResult result = service.payBill(billerCode, ref1, null, amount, "idem-123");

      assertThat(result.success()).isTrue();
      assertThat(result.confirmationNumber()).isEqualTo("CONF-123");
    }

    @Test
    void shouldHandlePaymentFailure() {
      String billerCode = "TNB";
      BigDecimal amount = new BigDecimal("150.00");
      Biller biller = new Biller(billerCode, "Tenaga Nasional", BillType.UTILITY, "https://api.tnb.com", true, true);
      PaymentResult paymentResult = new PaymentResult(false, null, null, "ERR_EXT_202");

      when(billerRegistryPort.findByCode(billerCode)).thenReturn(java.util.Optional.of(biller));
      when(billerGatewayPort.payBill(billerCode, "TNB-12345678", null, amount)).thenReturn(paymentResult);

      PaymentResult result = service.payBill(billerCode, "TNB-12345678", null, amount, "idem-123");

      assertThat(result.success()).isFalse();
      assertThat(result.errorCode()).isEqualTo("ERR_EXT_202");
    }
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "BillPaymentServiceTest"
```
Expected: FAIL — classes not found

- [ ] **Step 3: Write implementation**

**BillType.java:**
```java
package com.agentbanking.billeradapter.domain.model;

public enum BillType { UTILITY, TELECOM, INSURANCE, EDUCATION, OTHER }
```

**BillerStatus.java:**
```java
package com.agentbanking.billeradapter.domain.model;

public enum BillerStatus { PENDING, CONFIRMED, FAILED, REVERSED }
```

**Biller.java:**
```java
package com.agentbanking.billeradapter.domain.model;

public record Biller(
  String billerCode, String name, BillType type,
  String apiEndpoint, boolean supportsInquiry, boolean supportsReversal
) {
  public boolean isValid() {
    return billerCode != null && !billerCode.isBlank() && apiEndpoint != null;
  }
}
```

**BillerTransaction.java:**
```java
package com.agentbanking.billeradapter.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record BillerTransaction(
  String transactionId, String billerCode, String ref1, String ref2,
  BigDecimal amount, BillerStatus status, String customerName, Instant paidAt
) {
  public boolean canBeReversed() { return status == BillerStatus.CONFIRMED; }
}
```

**BillerGatewayPort.java (with nested records):**
```java
package com.agentbanking.billeradapter.domain.port.out;

import java.math.BigDecimal;

public interface BillerGatewayPort {
  BillDetails getBillDetails(String billerCode, String ref1, String ref2);
  PaymentResult payBill(String billerCode, String ref1, String ref2, BigDecimal amount);
  ReversalResult reverseBill(String transactionId, String reason);
}

record BillDetails(
  String billerCode, String ref1, String customerName,
  BigDecimal outstandingAmount, String billDescription
) {}

record PaymentResult(
  boolean success, String transactionId, String confirmationNumber, String errorCode
) {}

record ReversalResult(
  boolean success, String reversalId, String errorCode
) {}
```

**BillerRegistryPort.java:**
```java
package com.agentbanking.billeradapter.domain.port.out;

import com.agentbanking.billeradapter.domain.model.Biller;
import java.util.List;
import java.util.Optional;

public interface BillerRegistryPort {
  Optional<Biller> findByCode(String billerCode);
  List<Biller> findAll();
}
```

**GetBillUseCase.java:**
```java
package com.agentbanking.billeradapter.domain.port.in;

import com.agentbanking.billeradapter.domain.port.out.BillDetails;

public interface GetBillUseCase {
  BillDetails getBillDetails(String billerCode, String ref1, String ref2);
}
```

**PayBillUseCase.java:**
```java
package com.agentbanking.billeradapter.domain.port.in;

import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import java.math.BigDecimal;

public interface PayBillUseCase {
  PaymentResult payBill(String billerCode, String ref1, String ref2, BigDecimal amount, String idempotencyKey);
}
```

**ReverseBillUseCase.java:**
```java
package com.agentbanking.billeradapter.domain.port.in;

import com.agentbanking.billeradapter.domain.port.out.ReversalResult;

public interface ReverseBillUseCase {
  ReversalResult reverse(String transactionId, String reason);
}
```

**BillPaymentService.java (NO Logger):**
```java
package com.agentbanking.billeradapter.domain.service;

import com.agentbanking.billeradapter.domain.model.Biller;
import com.agentbanking.billeradapter.domain.port.in.GetBillUseCase;
import com.agentbanking.billeradapter.domain.port.in.PayBillUseCase;
import com.agentbanking.billeradapter.domain.port.in.ReverseBillUseCase;
import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.BillerGatewayPort;
import com.agentbanking.billeradapter.domain.port.out.BillerRegistryPort;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import com.agentbanking.billeradapter.domain.port.out.ReversalResult;

import java.math.BigDecimal;
import java.util.List;

public class BillPaymentService implements GetBillUseCase, PayBillUseCase, ReverseBillUseCase {

  private final BillerGatewayPort billerGatewayPort;
  private final BillerRegistryPort billerRegistryPort;

  public BillPaymentService(BillerGatewayPort billerGatewayPort, BillerRegistryPort billerRegistryPort) {
    this.billerGatewayPort = billerGatewayPort;
    this.billerRegistryPort = billerRegistryPort;
  }

  @Override
  public BillDetails getBillDetails(String billerCode, String ref1, String ref2) {
    Biller biller = billerRegistryPort.findByCode(billerCode)
      .orElseThrow(() -> new IllegalArgumentException("Unknown biller code: " + billerCode));

    if (!biller.supportsInquiry()) {
      throw new IllegalArgumentException("Biller does not support inquiry: " + billerCode);
    }

    return billerGatewayPort.getBillDetails(billerCode, ref1, ref2);
  }

  @Override
  public PaymentResult payBill(String billerCode, String ref1, String ref2, BigDecimal amount, String idempotencyKey) {
    Biller biller = billerRegistryPort.findByCode(billerCode)
      .orElseThrow(() -> new IllegalArgumentException("Unknown biller code: " + billerCode));

    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }

    return billerGatewayPort.payBill(billerCode, ref1, ref2, amount);
  }

  @Override
  public ReversalResult reverse(String transactionId, String reason) {
    return billerGatewayPort.reverseBill(transactionId, reason);
  }

  public List<Biller> getSupportedBillers() {
    return billerRegistryPort.findAll();
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew test --tests "BillPaymentServiceTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/agentbanking/billeradapter/domain/
git add src/test/java/com/agentbanking/billeradapter/domain/
git commit -m "feat(billeradapter): add domain models, ports, and bill payment service"
```

---

## Task 3: Application Layer — DTOs and Service (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/billeradapter/application/dto/BillInquiryRequest.java`
- Create: `src/main/java/com/agentbanking/billeradapter/application/dto/BillInquiryResponse.java`
- Create: `src/main/java/com/agentbanking/billeradapter/application/dto/BillPaymentRequest.java`
- Create: `src/main/java/com/agentbanking/billeradapter/application/dto/BillPaymentResponse.java`
- Create: `src/main/java/com/agentbanking/billeradapter/application/service/BillerApplicationService.java`

- [ ] **Step 1: Write implementation**

**BillInquiryRequest.java:**
```java
package com.agentbanking.billeradapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record BillInquiryRequest(
  @NotBlank(message = "Biller code is required") String billerCode,
  @NotBlank(message = "Reference 1 is required") String ref1,
  String ref2
) {}
```

**BillInquiryResponse.java:**
```java
package com.agentbanking.billeradapter.application.dto;

import java.math.BigDecimal;

public record BillInquiryResponse(
  String code, String billerCode, String ref1, String customerName,
  BigDecimal outstandingAmount, String billDescription, String message
) {
  public static BillInquiryResponse success(String billerCode, String ref1,
    String customerName, BigDecimal outstandingAmount, String billDescription) {
    return new BillInquiryResponse("SUCCESS", billerCode, ref1, customerName,
      outstandingAmount, billDescription, "Bill inquiry successful");
  }
  public static BillInquiryResponse failure(String errorCode, String message) {
    return new BillInquiryResponse(errorCode, null, null, null, null, null, message);
  }
}
```

**BillPaymentRequest.java:**
```java
package com.agentbanking.billeradapter.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BillPaymentRequest(
  @NotBlank(message = "Biller code is required") String billerCode,
  @NotBlank(message = "Reference 1 is required") String ref1,
  String ref2,
  @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
  @NotBlank(message = "Idempotency key is required") String idempotencyKey
) {}
```

**BillPaymentResponse.java:**
```java
package com.agentbanking.billeradapter.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BillPaymentResponse(
  String code, boolean success, String transactionId,
  String confirmationNumber, BigDecimal amount, String message, Instant timestamp
) {
  public static BillPaymentResponse success(String transactionId, String confirmationNumber, BigDecimal amount) {
    return new BillPaymentResponse("SUCCESS", true, transactionId, confirmationNumber,
      amount, "Payment successful", Instant.now());
  }
  public static BillPaymentResponse failure(String errorCode, String message) {
    return new BillPaymentResponse(errorCode, false, null, null, null, message, Instant.now());
  }
}
```

**BillerApplicationService.java (NO @Service — registered via @Bean):**
```java
package com.agentbanking.billeradapter.application.service;

import com.agentbanking.billeradapter.application.dto.*;
import com.agentbanking.billeradapter.domain.port.in.GetBillUseCase;
import com.agentbanking.billeradapter.domain.port.in.PayBillUseCase;
import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BillerApplicationService {

  private static final Logger log = LoggerFactory.getLogger(BillerApplicationService.class);

  private final GetBillUseCase getBillUseCase;
  private final PayBillUseCase payBillUseCase;

  public BillerApplicationService(GetBillUseCase getBillUseCase, PayBillUseCase payBillUseCase) {
    this.getBillUseCase = getBillUseCase;
    this.payBillUseCase = payBillUseCase;
  }

  public BillInquiryResponse inquireBill(BillInquiryRequest request) {
    try {
      BillDetails details = getBillUseCase.getBillDetails(
        request.billerCode(), request.ref1(), request.ref2());
      return BillInquiryResponse.success(details.billerCode(), details.ref1(),
        details.customerName(), details.outstandingAmount(), details.billDescription());
    } catch (Exception e) {
      log.error("Bill inquiry failed: {}", e.getMessage());
      return BillInquiryResponse.failure("ERR_EXT_201", e.getMessage());
    }
  }

  public BillPaymentResponse payBill(BillPaymentRequest request) {
    try {
      PaymentResult result = payBillUseCase.payBill(
        request.billerCode(), request.ref1(), request.ref2(),
        request.amount(), request.idempotencyKey());

      if (result.success()) {
        return BillPaymentResponse.success(result.transactionId(), result.confirmationNumber(), request.amount());
      } else {
        log.warn("Bill payment failed: errorCode={}", result.errorCode());
        return BillPaymentResponse.failure(
          result.errorCode() != null ? result.errorCode() : "ERR_EXT_202", "Payment failed");
      }
    } catch (Exception e) {
      log.error("Bill payment error: {}", e.getMessage());
      return BillPaymentResponse.failure("ERR_EXT_201", e.getMessage());
    }
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/billeradapter/application/
git commit -m "feat(billeradapter): add application DTOs and service"
```

---

## Task 4: Infrastructure — Biller Clients, Registry, and Adapter (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/billeradapter/infrastructure/registry/BillerRegistry.java`
- Create: `src/main/java/com/agentbanking/billeradapter/infrastructure/external/TnbBillerClient.java`
- Create: `src/main/java/com/agentbanking/billeradapter/infrastructure/external/MaxisBillerClient.java`
- Create: `src/main/java/com/agentbanking/billeradapter/infrastructure/external/JompayBillerClient.java`
- Create: `src/main/java/com/agentbanking/billeradapter/infrastructure/adapter/CompositeBillerAdapter.java`

- [ ] **Step 1: Write implementation**

**BillerRegistry.java:**
```java
package com.agentbanking.billeradapter.infrastructure.registry;

import com.agentbanking.billeradapter.domain.model.Biller;
import com.agentbanking.billeradapter.domain.model.BillType;
import com.agentbanking.billeradapter.domain.port.out.BillerRegistryPort;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BillerRegistry implements BillerRegistryPort {

  private static final List<Biller> BILLERS = List.of(
    new Biller("TNB", "Tenaga Nasional Berhad", BillType.UTILITY, "https://api.tnb.com.my", true, true),
    new Biller("MAXIS", "Maxis Berhad", BillType.TELECOM, "https://api.maxis.com.my", true, true),
    new Biller("DIGI", "Digi Telecommunications", BillType.TELECOM, "https://api.digi.com.my", true, true),
    new Biller("UNIFI", "Telekom Malaysia", BillType.TELECOM, "https://api.tm.com.my", true, true),
    new Biller("JOMPAY", "JomPAY", BillType.OTHER, "https://api.jompay.com.my", true, false),
    new Biller("ASTRO", "Astro", BillType.OTHER, "https://api.astro.com.my", true, true)
  );

  @Override
  public Optional<Biller> findByCode(String billerCode) {
    return BILLERS.stream().filter(b -> b.billerCode().equalsIgnoreCase(billerCode)).findFirst();
  }

  @Override
  public List<Biller> findAll() { return BILLERS; }
}
```

**TnbBillerClient.java:**
```java
package com.agentbanking.billeradapter.infrastructure.external;

import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import com.agentbanking.billeradapter.domain.port.out.ReversalResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TnbBillerClient {

  private static final Logger log = LoggerFactory.getLogger(TnbBillerClient.class);

  public BillDetails getBillDetails(String ref1, String ref2) {
    log.info("Calling TNB bill inquiry API for ref: {}", ref1);
    return new BillDetails("TNB", ref1, "CUSTOMER NAME", new BigDecimal("150.00"), "Electricity Bill - TNB");
  }

  public PaymentResult payBill(String ref1, String ref2, BigDecimal amount) {
    log.info("Calling TNB payment API for ref: {}, amount: {}", ref1, amount);
    return new PaymentResult(true,
      "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
      "CONF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(), null);
  }

  public ReversalResult reverseBill(String transactionId, String reason) {
    log.info("Calling TNB reversal API for transaction: {}", transactionId);
    return new ReversalResult(true,
      "REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), null);
  }
}
```

**MaxisBillerClient.java:**
```java
package com.agentbanking.billeradapter.infrastructure.external;

import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import com.agentbanking.billeradapter.domain.port.out.ReversalResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MaxisBillerClient {

  private static final Logger log = LoggerFactory.getLogger(MaxisBillerClient.class);

  public BillDetails getBillDetails(String ref1, String ref2) {
    log.info("Calling Maxis bill inquiry API for ref: {}", ref1);
    return new BillDetails("MAXIS", ref1, "CUSTOMER NAME", new BigDecimal("100.00"), "Maxis Bill");
  }

  public PaymentResult payBill(String ref1, String ref2, BigDecimal amount) {
    log.info("Calling Maxis payment API for ref: {}, amount: {}", ref1, amount);
    return new PaymentResult(true,
      "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
      "CONF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(), null);
  }

  public ReversalResult reverseBill(String transactionId, String reason) {
    log.info("Calling Maxis reversal API for transaction: {}", transactionId);
    return new ReversalResult(true,
      "REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), null);
  }
}
```

**JompayBillerClient.java:**
```java
package com.agentbanking.billeradapter.infrastructure.external;

import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import com.agentbanking.billeradapter.domain.port.out.ReversalResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class JompayBillerClient {

  private static final Logger log = LoggerFactory.getLogger(JompayBillerClient.class);

  public BillDetails getBillDetails(String ref1, String ref2) {
    log.info("Calling JomPAY bill inquiry API for ref: {}", ref1);
    return new BillDetails("JOMPAY", ref1, "CUSTOMER NAME", new BigDecimal("200.00"), "JomPAY Bill");
  }

  public PaymentResult payBill(String ref1, String ref2, BigDecimal amount) {
    log.info("Calling JomPAY payment API for ref: {}, amount: {}", ref1, amount);
    return new PaymentResult(true,
      "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
      "JP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(), null);
  }
}
```

**CompositeBillerAdapter.java (implements BillerGatewayPort):**
```java
package com.agentbanking.billeradapter.infrastructure.adapter;

import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.BillerGatewayPort;
import com.agentbanking.billeradapter.domain.port.out.BillerRegistryPort;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import com.agentbanking.billeradapter.domain.port.out.ReversalResult;
import com.agentbanking.billeradapter.infrastructure.external.JompayBillerClient;
import com.agentbanking.billeradapter.infrastructure.external.MaxisBillerClient;
import com.agentbanking.billeradapter.infrastructure.external.TnbBillerClient;

import java.math.BigDecimal;

public class CompositeBillerAdapter implements BillerGatewayPort {

  private final TnbBillerClient tnbClient;
  private final MaxisBillerClient maxisClient;
  private final JompayBillerClient jompayClient;
  private final BillerRegistryPort billerRegistryPort;

  public CompositeBillerAdapter(TnbBillerClient tnbClient, MaxisBillerClient maxisClient,
    JompayBillerClient jompayClient, BillerRegistryPort billerRegistryPort) {
    this.tnbClient = tnbClient;
    this.maxisClient = maxisClient;
    this.jompayClient = jompayClient;
    this.billerRegistryPort = billerRegistryPort;
  }

  @Override
  public BillDetails getBillDetails(String billerCode, String ref1, String ref2) {
    return switch (billerCode.toUpperCase()) {
      case "TNB" -> tnbClient.getBillDetails(ref1, ref2);
      case "MAXIS", "DIGI", "UNIFI" -> maxisClient.getBillDetails(ref1, ref2);
      case "JOMPAY" -> jompayClient.getBillDetails(ref1, ref2);
      default -> throw new IllegalArgumentException("Unknown biller: " + billerCode);
    };
  }

  @Override
  public PaymentResult payBill(String billerCode, String ref1, String ref2, BigDecimal amount) {
    return switch (billerCode.toUpperCase()) {
      case "TNB" -> tnbClient.payBill(ref1, ref2, amount);
      case "MAXIS", "DIGI", "UNIFI" -> maxisClient.payBill(ref1, ref2, amount);
      case "JOMPAY" -> jompayClient.payBill(ref1, ref2, amount);
      default -> throw new IllegalArgumentException("Unknown biller: " + billerCode);
    };
  }

  @Override
  public ReversalResult reverseBill(String transactionId, String reason) {
    return tnbClient.reverseBill(transactionId, reason);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/billeradapter/infrastructure/
git commit -m "feat(billeradapter): add biller clients, registry, and composite adapter"
```

---

## Task 5: Config — Domain Service Registration (Write Manually)

**Law V: Domain services via @Bean in config, NOT @Service annotation.**

**Files:**
- Update: `src/main/java/com/agentbanking/billeradapter/config/DomainServiceConfig.java` (created by Seed4J)

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.billeradapter.config;

import com.agentbanking.billeradapter.domain.port.out.BillerGatewayPort;
import com.agentbanking.billeradapter.domain.port.out.BillerRegistryPort;
import com.agentbanking.billeradapter.domain.service.BillPaymentService;
import com.agentbanking.billeradapter.infrastructure.adapter.CompositeBillerAdapter;
import com.agentbanking.billeradapter.infrastructure.external.JompayBillerClient;
import com.agentbanking.billeradapter.infrastructure.external.MaxisBillerClient;
import com.agentbanking.billeradapter.infrastructure.external.TnbBillerClient;
import com.agentbanking.billeradapter.infrastructure.registry.BillerRegistry;
import com.agentbanking.billeradapter.application.service.BillerApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

  @Bean
  public BillerRegistryPort billerRegistryPort() {
    return new BillerRegistry();
  }

  @Bean
  public BillerGatewayPort billerGatewayPort(
    TnbBillerClient tnbClient, MaxisBillerClient maxisClient,
    JompayBillerClient jompayClient, BillerRegistryPort registryPort
  ) {
    return new CompositeBillerAdapter(tnbClient, maxisClient, jompayClient, registryPort);
  }

  @Bean
  public BillPaymentService billPaymentService(
    BillerGatewayPort gatewayPort, BillerRegistryPort registryPort
  ) {
    return new BillPaymentService(gatewayPort, registryPort);
  }

  @Bean
  public BillerApplicationService billerApplicationService(
    BillPaymentService billPaymentService
  ) {
    return new BillerApplicationService(billPaymentService, billPaymentService);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/billeradapter/config/DomainServiceConfig.java
git commit -m "feat(billeradapter): register domain services via @Bean"
```

---

## Task 6: REST Controller and Kafka Events (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/billeradapter/infrastructure/web/BillerController.java`
- Create: `src/main/java/com/agentbanking/billeradapter/infrastructure/messaging/BillPaymentConfirmedEvent.java`
- Create: `src/main/java/com/agentbanking/billeradapter/infrastructure/messaging/BillPaymentFailedEvent.java`
- Create: `src/main/java/com/agentbanking/billeradapter/infrastructure/messaging/BillerEventPublisher.java`

- [ ] **Step 1: Write implementation**

**BillerController.java:**
```java
package com.agentbanking.billeradapter.infrastructure.web;

import com.agentbanking.billeradapter.application.dto.*;
import com.agentbanking.billeradapter.application.service.BillerApplicationService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billers")
public class BillerController {

  private static final Logger log = LoggerFactory.getLogger(BillerController.class);

  private final BillerApplicationService applicationService;

  public BillerController(BillerApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @PostMapping("/inquire")
  public ResponseEntity<BillInquiryResponse> inquireBill(@Valid @RequestBody BillInquiryRequest request) {
    log.info("Bill inquiry request: biller={}, ref1={}", request.billerCode(), request.ref1());
    return ResponseEntity.ok(applicationService.inquireBill(request));
  }

  @PostMapping("/pay")
  public ResponseEntity<BillPaymentResponse> payBill(@Valid @RequestBody BillPaymentRequest request) {
    log.info("Bill payment request: biller={}, ref1={}, amount={}",
      request.billerCode(), request.ref1(), request.amount());
    return ResponseEntity.ok(applicationService.payBill(request));
  }

  @GetMapping
  public ResponseEntity<java.util.List<com.agentbanking.billeradapter.domain.model.Biller>> listBillers() {
    log.info("List all billers request");
    return ResponseEntity.ok(java.util.List.of());
  }
}
```

**BillPaymentConfirmedEvent.java:**
```java
package com.agentbanking.billeradapter.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;

public record BillPaymentConfirmedEvent(
  String transactionId, String billerCode, String ref1,
  BigDecimal amount, String confirmationNumber, Instant timestamp
) {}
```

**BillPaymentFailedEvent.java:**
```java
package com.agentbanking.billeradapter.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;

public record BillPaymentFailedEvent(
  String transactionId, String billerCode, String ref1,
  BigDecimal amount, String errorCode, String errorMessage, Instant timestamp
) {}
```

**BillerEventPublisher.java:**
```java
package com.agentbanking.billeradapter.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class BillerEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(BillerEventPublisher.class);
  private static final String TOPIC = "biller-payment-events";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public BillerEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publishConfirmed(BillPaymentConfirmedEvent event) {
    log.info("Publishing bill payment confirmed event: {}", event.transactionId());
    kafkaTemplate.send(TOPIC, event.transactionId(), event);
  }

  public void publishFailed(BillPaymentFailedEvent event) {
    log.info("Publishing bill payment failed event: {}", event.transactionId());
    kafkaTemplate.send(TOPIC, event.transactionId(), event);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/billeradapter/infrastructure/web/
git add src/main/java/com/agentbanking/billeradapter/infrastructure/messaging/
git commit -m "feat(billeradapter): add REST controller and Kafka event publisher"
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
| 2 | Domain Models & Ports | Manual (NO Logger) | 1 |
| 3 | Application DTOs & Service | Manual | 0 |
| 4 | Infrastructure (Biller Clients, Adapter) | Manual | 0 |
| 5 | Config (@Bean registration) | Manual (Law V) | 0 |
| 6 | REST Controller + Kafka Events | Manual | 0 |

**Key fixes from original plan:**
- `./gradlew test` (not `./mvnw`)
- Seed4J scaffolding replaces manual package-info.java creation
- Domain services via `@Bean` in config (Law V), NOT `@Service`
- Removed Logger from `BillPaymentService` (Law VI violation)
- Simplified task structure (combined related tasks)
- Kafka events included as part of infrastructure layer
