# 17. CBS Adapter Sub-Plan

> **Bounding Context:** com.agentbanking.cbsadapter
> **Wave:** 5.3
> **Depends On:** 01 (error registry), 02 (domain model)
> **BDD Scenarios:** BDD-CBS01, BDD-CBS01-EC-01, BDD-CBS02, BDD-CBS02-EC-01
> **BRD Requirements:** US-CBS01, FR-21.1 through FR-21.4

**Goal:** Implement CBS connector with account inquiry, debit/credit operations, error code mapping, and circuit breaker protection.

**Architecture:** Wave 5 — Tier 4 Adapter. Hexagonal architecture. Translates internal JSON to CBS legacy protocols. Manages async request/reply patterns with Resilience4j circuit breaker.

**Tech Stack:** Java 21, Spring Boot 4, Spring Cloud OpenFeign, Resilience4j, JUnit 5, Mockito, ArchUnit, Gradle

---

## Task 1: Seed4J Scaffolding

Run Seed4J CLI to scaffold the CBS adapter service:

```bash
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply spring-boot \
  --context cbsadapter \
  --package com.agentbanking.cbsadapter \
  --no-commit
```

This creates:
- Hexagonal package structure: `domain/`, `application/`, `infrastructure/`, `config/`
- `@BusinessContext` annotation on package-info
- `DomainServiceConfig.java` stub with `@Bean` registration pattern
- ArchUnit test: `HexagonalArchitectureTest.java`
- Test base classes and `@UnitTest` annotation
- Gradle build configuration

- [ ] **Step 1: Run Seed4J scaffolding**
- [ ] **Step 2: Verify generated structure matches hexagonal layout**
- [ ] **Step 3: Commit scaffolding**

```bash
git add . && git commit -m "feat(cbsadapter): scaffold with Seed4J spring-boot module"
```

---

## Task 2: Domain Models (Write Manually)

**CRITICAL: ZERO framework imports in domain/ — no Logger, no Spring, no JPA.**

**Files:**
- Create: `src/main/java/com/agentbanking/cbsadapter/domain/model/CbsAccount.java`
- Create: `src/main/java/com/agentbanking/cbsadapter/domain/model/CbsTransaction.java`
- Create: `src/main/java/com/agentbanking/cbsadapter/domain/model/CbsResponse.java`

- [ ] **Step 1: Write implementation**

**CbsAccount.java:**
```java
package com.agentbanking.cbsadapter.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CbsAccount(
  String accountId, String accountNumber, String accountType,
  String status, BigDecimal availableBalance, BigDecimal ledgerBalance,
  String currency, Instant lastTransactionDate
) {
  public static CbsAccount empty(String accountId) {
    return new CbsAccount(accountId, null, null, "UNKNOWN",
      BigDecimal.ZERO, BigDecimal.ZERO, "MYR", null);
  }
}
```

**CbsTransaction.java:**
```java
package com.agentbanking.cbsadapter.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CbsTransaction(
  String transactionId, String accountId, String transactionType,
  BigDecimal amount, BigDecimal runningBalance, String reference,
  String status, Instant timestamp
) {}
```

**CbsResponse.java:**
```java
package com.agentbanking.cbsadapter.domain.model;

public record CbsResponse(
  boolean success, String cbsCode, String message, String correlationId, Object data
) {
  public static CbsResponse success(String correlationId, Object data) {
    return new CbsResponse(true, "00", "Success", correlationId, data);
  }
  public static CbsResponse failure(String cbsCode, String message) {
    return new CbsResponse(false, cbsCode, message, null, null);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/cbsadapter/domain/model/
git commit -m "feat(cbsadapter): add domain models"
```

---

## Task 3: Domain Ports and Services (Write Manually)

**CRITICAL: NO Logger in domain layer.**

**Files:**
- Create: `src/main/java/com/agentbanking/cbsadapter/domain/port/in/InquiryAccountUseCase.java`
- Create: `src/main/java/com/agentbanking/cbsadapter/domain/port/in/CreditAccountUseCase.java`
- Create: `src/main/java/com/agentbanking/cbsadapter/domain/port/in/DebitAccountUseCase.java`
- Create: `src/main/java/com/agentbanking/cbsadapter/domain/port/out/CbsGatewayPort.java`
- Create: `src/main/java/com/agentbanking/cbsadapter/domain/service/CbsErrorMapper.java`
- Create: `src/main/java/com/agentbanking/cbsadapter/domain/service/CbsIntegrationService.java`

- [ ] **Step 1: Write implementation**

**InquiryAccountUseCase.java:**
```java
package com.agentbanking.cbsadapter.domain.port.in;

import com.agentbanking.cbsadapter.domain.model.CbsAccount;

public interface InquiryAccountUseCase {
  CbsAccount inquiry(String accountId);
}
```

**CreditAccountUseCase.java:**
```java
package com.agentbanking.cbsadapter.domain.port.in;

import com.agentbanking.cbsadapter.domain.model.CbsResponse;
import java.math.BigDecimal;

public interface CreditAccountUseCase {
  CbsResponse credit(String accountId, BigDecimal amount, String reference);
}
```

**DebitAccountUseCase.java:**
```java
package com.agentbanking.cbsadapter.domain.port.in;

import com.agentbanking.cbsadapter.domain.model.CbsResponse;
import java.math.BigDecimal;

public interface DebitAccountUseCase {
  CbsResponse debit(String accountId, BigDecimal amount, String reference);
}
```

**CbsGatewayPort.java:**
```java
package com.agentbanking.cbsadapter.domain.port.out;

import com.agentbanking.cbsadapter.domain.model.CbsResponse;
import java.util.Map;

public interface CbsGatewayPort {
  CbsResponse sendRequest(String operation, Map<String, Object> payload, String correlationId);
}
```

**CbsErrorMapper.java (NO Logger):**
```java
package com.agentbanking.cbsadapter.domain.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CbsErrorMapper {

  private static final Map<String, ErrorMapping> CBS_ERRORS = new ConcurrentHashMap<>();

  static {
    CBS_ERRORS.put("ACCOUNT_NOT_FOUND", new ErrorMapping("ERR_VAL_005", "Account not found", "DECLINE"));
    CBS_ERRORS.put("INSUFFICIENT_FUNDS", new ErrorMapping("ERR_BIZ_001", "Insufficient funds", "DECLINE"));
    CBS_ERRORS.put("ACCOUNT_LOCKED", new ErrorMapping("ERR_BIZ_002", "Account is locked", "DECLINE"));
    CBS_ERRORS.put("ACCOUNT_CLOSED", new ErrorMapping("ERR_BIZ_003", "Account is closed", "DECLINE"));
    CBS_ERRORS.put("INVALID_AMOUNT", new ErrorMapping("ERR_VAL_004", "Invalid amount", "DECLINE"));
    CBS_ERRORS.put("DUPLICATE_REFERENCE", new ErrorMapping("ERR_BIZ_004", "Duplicate reference", "DECLINE"));
    CBS_ERRORS.put("CBS_TIMEOUT", new ErrorMapping("ERR_EXT_101", "CBS timeout", "RETRY"));
    CBS_ERRORS.put("CBS_ERROR", new ErrorMapping("ERR_EXT_102", "CBS error", "RETRY"));
    CBS_ERRORS.put("CONNECTION_FAILED", new ErrorMapping("ERR_EXT_103", "Connection to CBS failed", "RETRY"));
  }

  public record ErrorMapping(String code, String message, String action) {}

  public static ErrorMapping map(String cbsErrorCode) {
    ErrorMapping mapping = CBS_ERRORS.get(cbsErrorCode);
    if (mapping == null) {
      return new ErrorMapping("ERR_EXT_999", "Unknown CBS error: " + cbsErrorCode, "RETRY");
    }
    return mapping;
  }
}
```

**CbsIntegrationService.java (NO Logger):**
```java
package com.agentbanking.cbsadapter.domain.service;

import com.agentbanking.cbsadapter.domain.model.CbsAccount;
import com.agentbanking.cbsadapter.domain.model.CbsResponse;
import com.agentbanking.cbsadapter.domain.port.in.CreditAccountUseCase;
import com.agentbanking.cbsadapter.domain.port.in.DebitAccountUseCase;
import com.agentbanking.cbsadapter.domain.port.in.InquiryAccountUseCase;
import com.agentbanking.cbsadapter.domain.port.out.CbsGatewayPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CbsIntegrationService implements InquiryAccountUseCase, CreditAccountUseCase, DebitAccountUseCase {

  private final CbsGatewayPort gatewayPort;

  public CbsIntegrationService(CbsGatewayPort gatewayPort) {
    this.gatewayPort = gatewayPort;
  }

  @Override
  public CbsAccount inquiry(String accountId) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("accountId", accountId);
    String correlationId = UUID.randomUUID().toString();
    CbsResponse response = gatewayPort.sendRequest("GET_ACCOUNT", payload, correlationId);

    if (!response.success()) {
      return CbsAccount.empty(accountId);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.data();
    return new CbsAccount(
      accountId, (String) data.get("accountNumber"), (String) data.get("accountType"),
      (String) data.get("status"),
      new BigDecimal(data.get("availableBalance").toString()),
      new BigDecimal(data.get("ledgerBalance").toString()),
      (String) data.getOrDefault("currency", "MYR"),
      Instant.parse((String) data.get("lastTransactionDate"))
    );
  }

  @Override
  public CbsResponse credit(String accountId, BigDecimal amount, String reference) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("accountId", accountId);
    payload.put("amount", amount);
    payload.put("reference", reference);
    payload.put("transactionType", "CREDIT");
    String correlationId = UUID.randomUUID().toString();
    return gatewayPort.sendRequest("CREDIT_ACCOUNT", payload, correlationId);
  }

  @Override
  public CbsResponse debit(String accountId, BigDecimal amount, String reference) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("accountId", accountId);
    payload.put("amount", amount);
    payload.put("reference", reference);
    payload.put("transactionType", "DEBIT");
    String correlationId = UUID.randomUUID().toString();
    return gatewayPort.sendRequest("DEBIT_ACCOUNT", payload, correlationId);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/cbsadapter/domain/
git commit -m "feat(cbsadapter): add domain ports, error mapper, and integration service"
```

---

## Task 4: Application Layer — DTOs and Service (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/cbsadapter/application/dto/CbsAccountRequest.java`
- Create: `src/main/java/com/agentbanking/cbsadapter/application/dto/CbsTransferRequest.java`
- Create: `src/main/java/com/agentbanking/cbsadapter/application/service/CbsApplicationService.java`

- [ ] **Step 1: Write implementation**

**CbsAccountRequest.java:**
```java
package com.agentbanking.cbsadapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CbsAccountRequest(
  @NotBlank String accountId, String accountNumber, String accountType
) {}
```

**CbsTransferRequest.java:**
```java
package com.agentbanking.cbsadapter.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CbsTransferRequest(
  @NotBlank String fromAccountId, @NotBlank String toAccountId,
  @NotNull @Positive BigDecimal amount, @NotBlank String reference, String description
) {}
```

**CbsApplicationService.java (NO @Service — registered via @Bean):**
```java
package com.agentbanking.cbsadapter.application.service;

import com.agentbanking.cbsadapter.domain.model.CbsAccount;
import com.agentbanking.cbsadapter.domain.model.CbsResponse;
import com.agentbanking.cbsadapter.domain.port.in.CreditAccountUseCase;
import com.agentbanking.cbsadapter.domain.port.in.DebitAccountUseCase;
import com.agentbanking.cbsadapter.domain.port.in.InquiryAccountUseCase;

import java.math.BigDecimal;

public class CbsApplicationService {

  private final InquiryAccountUseCase inquiryAccountUseCase;
  private final CreditAccountUseCase creditAccountUseCase;
  private final DebitAccountUseCase debitAccountUseCase;

  public CbsApplicationService(
    InquiryAccountUseCase inquiryAccountUseCase,
    CreditAccountUseCase creditAccountUseCase,
    DebitAccountUseCase debitAccountUseCase
  ) {
    this.inquiryAccountUseCase = inquiryAccountUseCase;
    this.creditAccountUseCase = creditAccountUseCase;
    this.debitAccountUseCase = debitAccountUseCase;
  }

  public CbsAccount inquiryAccount(String accountId) {
    return inquiryAccountUseCase.inquiry(accountId);
  }

  public CbsResponse creditAccount(String accountId, BigDecimal amount, String reference) {
    return creditAccountUseCase.credit(accountId, amount, reference);
  }

  public CbsResponse debitAccount(String accountId, BigDecimal amount, String reference) {
    return debitAccountUseCase.debit(accountId, amount, reference);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/cbsadapter/application/
git commit -m "feat(cbsadapter): add application DTOs and service"
```

---

## Task 5: Infrastructure — Feign Client and Gateway Port (Write Manually)

**FIX: Feign URL from config property, NOT hardcoded.**

**Files:**
- Create: `src/main/java/com/agentbanking/cbsadapter/infrastructure/external/CbsFeignClient.java`
- Create: `src/main/java/com/agentbanking/cbsadapter/infrastructure/external/CbsGatewayPortImpl.java`

- [ ] **Step 1: Write implementation**

**CbsFeignClient.java (FIX: url from config property):**
```java
package com.agentbanking.cbsadapter.infrastructure.external;

import com.agentbanking.cbsadapter.application.dto.CbsAccountRequest;
import com.agentbanking.cbsadapter.application.dto.CbsTransferRequest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
  name = "cbs-client",
  url = "${agentbanking.cbs.url:http://cbs-gateway:8080}"
)
public interface CbsFeignClient {

  @PostMapping("/api/v1/cbs/account/inquiry")
  Map<String, Object> inquiryAccount(@RequestBody CbsAccountRequest request);

  @PostMapping("/api/v1/cbs/account/credit")
  Map<String, Object> creditAccount(@RequestBody Map<String, Object> request);

  @PostMapping("/api/v1/cbs/account/debit")
  Map<String, Object> debitAccount(@RequestBody Map<String, Object> request);

  @PostMapping("/api/v1/cbs/transfer")
  Map<String, Object> transfer(@RequestBody CbsTransferRequest request);
}
```

**CbsGatewayPortImpl.java:**
```java
package com.agentbanking.cbsadapter.infrastructure.external;

import com.agentbanking.cbsadapter.application.dto.CbsAccountRequest;
import com.agentbanking.cbsadapter.domain.model.CbsResponse;
import com.agentbanking.cbsadapter.domain.port.out.CbsGatewayPort;
import com.agentbanking.cbsadapter.domain.service.CbsErrorMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CbsGatewayPortImpl implements CbsGatewayPort {

  private static final Logger log = LoggerFactory.getLogger(CbsGatewayPortImpl.class);

  private final CbsFeignClient feignClient;

  public CbsGatewayPortImpl(CbsFeignClient feignClient) {
    this.feignClient = feignClient;
  }

  @Override
  @CircuitBreaker(name = "cbs", fallbackMethod = "fallback")
  public CbsResponse sendRequest(String operation, Map<String, Object> payload, String correlationId) {
    log.info("Sending CBS request: operation={}, correlationId={}", operation, correlationId);

    try {
      Map<String, Object> response = switch (operation) {
        case "GET_ACCOUNT" -> feignClient.inquiryAccount(
          new CbsAccountRequest((String) payload.get("accountId"), null, null)
        );
        case "CREDIT_ACCOUNT" -> feignClient.creditAccount(payload);
        case "DEBIT_ACCOUNT" -> feignClient.debitAccount(payload);
        default -> throw new IllegalArgumentException("Unknown operation: " + operation);
      };

      return parseResponse(response, correlationId);
    } catch (Exception e) {
      log.error("CBS request failed: {}", e.getMessage());
      return CbsResponse.failure("CBS_ERROR", e.getMessage());
    }
  }

  private CbsResponse parseResponse(Map<String, Object> response, String correlationId) {
    String cbsCode = (String) response.get("code");
    if ("00".equals(cbsCode)) {
      return CbsResponse.success(correlationId, response.get("data"));
    }
    CbsErrorMapper.ErrorMapping mapping = CbsErrorMapper.map(cbsCode);
    return new CbsResponse(false, mapping.code(), mapping.message(), correlationId, null);
  }

  public CbsResponse fallback(String operation, Map<String, Object> payload, String correlationId, Exception e) {
    log.error("CBS circuit breaker fallback: operation={}, error={}", operation, e.getMessage());
    return CbsResponse.failure("ERR_EXT_101", "CBS service unavailable");
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/cbsadapter/infrastructure/
git commit -m "feat(cbsadapter): add Feign client and gateway port with circuit breaker"
```

---

## Task 6: Config — Domain Service Registration (Write Manually)

**Law V: Domain services via @Bean in config, NOT @Service annotation.**

**Files:**
- Update: `src/main/java/com/agentbanking/cbsadapter/config/DomainServiceConfig.java` (created by Seed4J)

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.cbsadapter.config;

import com.agentbanking.cbsadapter.domain.port.in.CreditAccountUseCase;
import com.agentbanking.cbsadapter.domain.port.in.DebitAccountUseCase;
import com.agentbanking.cbsadapter.domain.port.in.InquiryAccountUseCase;
import com.agentbanking.cbsadapter.domain.port.out.CbsGatewayPort;
import com.agentbanking.cbsadapter.domain.service.CbsIntegrationService;
import com.agentbanking.cbsadapter.application.service.CbsApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

  @Bean
  public CbsIntegrationService cbsIntegrationService(CbsGatewayPort gatewayPort) {
    return new CbsIntegrationService(gatewayPort);
  }

  @Bean
  public InquiryAccountUseCase inquiryAccountUseCase(CbsIntegrationService service) {
    return service;
  }

  @Bean
  public CreditAccountUseCase creditAccountUseCase(CbsIntegrationService service) {
    return service;
  }

  @Bean
  public DebitAccountUseCase debitAccountUseCase(CbsIntegrationService service) {
    return service;
  }

  @Bean
  public CbsApplicationService cbsApplicationService(
    InquiryAccountUseCase inquiryUseCase,
    CreditAccountUseCase creditUseCase,
    DebitAccountUseCase debitUseCase
  ) {
    return new CbsApplicationService(inquiryUseCase, creditUseCase, debitUseCase);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/cbsadapter/config/DomainServiceConfig.java
git commit -m "feat(cbsadapter): register domain services via @Bean"
```

---

## Task 7: Application Configuration and Tests

**Files:**
- Create: `src/main/resources/application.yaml`
- Create: `src/test/java/com/agentbanking/cbsadapter/domain/service/CbsErrorMapperTest.java`

- [ ] **Step 1: Write implementation**

**application.yaml:**
```yaml
spring:
  application:
    name: cbs-adapter

agentbanking:
  cbs:
    url: ${CBS_URL:http://cbs-gateway:8080}
    connection-timeout-seconds: 30
    read-timeout-seconds: 60

resilience4j:
  circuitbreaker:
    instances:
      cbs:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
  retry:
    instances:
      cbs:
        maxAttempts: 3
        waitDuration: 5s
```

**CbsErrorMapperTest.java:**
```java
package com.agentbanking.cbsadapter.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CbsErrorMapperTest {

  @Test
  @DisplayName("should map ACCOUNT_NOT_FOUND to business error")
  void shouldMapAccountNotFound() {
    var mapping = CbsErrorMapper.map("ACCOUNT_NOT_FOUND");
    assertEquals("ERR_VAL_005", mapping.code());
    assertEquals("Account not found", mapping.message());
    assertEquals("DECLINE", mapping.action());
  }

  @Test
  @DisplayName("should map INSUFFICIENT_FUNDS to business error")
  void shouldMapInsufficientFunds() {
    var mapping = CbsErrorMapper.map("INSUFFICIENT_FUNDS");
    assertEquals("ERR_BIZ_001", mapping.code());
    assertEquals("Insufficient funds", mapping.message());
  }

  @Test
  @DisplayName("should map CBS_TIMEOUT to retry action")
  void shouldMapTimeout() {
    var mapping = CbsErrorMapper.map("CBS_TIMEOUT");
    assertEquals("ERR_EXT_101", mapping.code());
    assertEquals("RETRY", mapping.action());
  }

  @Test
  @DisplayName("should map unknown error to default")
  void shouldMapUnknownToDefault() {
    var mapping = CbsErrorMapper.map("UNKNOWN_ERROR");
    assertEquals("ERR_EXT_999", mapping.code());
    assertTrue(mapping.message().contains("UNKNOWN_ERROR"));
  }
}
```

- [ ] **Step 2: Run tests**

```bash
./gradlew test --tests "CbsErrorMapperTest"
```
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yaml
git add src/test/java/com/agentbanking/cbsadapter/
git commit -m "feat(cbsadapter): add application config and unit tests"
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
| 1 | Scaffolding | **Seed4J CLI** (spring-boot) | ArchUnit |
| 2 | Domain Models | Manual | 0 |
| 3 | Domain Ports & Services | Manual (NO Logger) | 0 |
| 4 | Application DTOs & Service | Manual | 0 |
| 5 | Infrastructure (Feign, Gateway) | Manual (configurable URL) | 0 |
| 6 | Config (@Bean registration) | Manual (Law V) | 0 |
| 7 | Application Config + Tests | Manual | 1 |

**Key fixes from original plan:**
- `./gradlew test` (not `./mvnw`)
- Seed4J scaffolding replaces manual package-info.java creation
- Domain services via `@Bean` in config (Law V), NOT `@Service`
- Removed Logger from `CbsErrorMapper` and `CbsIntegrationService` (Law VI violation)
- Feign URL from config property `${agentbanking.cbs.url}`
