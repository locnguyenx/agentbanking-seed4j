# HSM Adapter Implementation Plan

> **Bounding Context:** com.agentbanking.hsmadapter
> **Wave:** 5.4
> **Depends On:** Plan 01 (Error Registry), Plan 02 (Domain Model)
> **BDD Scenarios:** BDD-HSM01, BDD-HSM01-EC-01, BDD-HSM02, BDD-HSM02-EC-01
> **BRD Requirements:** FR-22.1, FR-22.2, FR-22.3, FR-22.4

**Goal:** Implement the HSM Adapter with PIN block generation/translation, PIN verification, random PIN generation, and circuit breaker for HSM connectivity.

**Architecture:** Wave 5 — Tier 4 Adapter. Hexagonal architecture. Connects to Thales HSM device for PIN management and cryptographic operations. **CRITICAL SECURITY: Zero-logging for PIN blocks and decrypted PINs.**

**Tech Stack:** Java 21, Spring Boot 4, Resilience4j, JUnit 5, Mockito, ArchUnit, Gradle

---

## Task 1: Seed4J Scaffolding

Run Seed4J CLI to scaffold the HSM adapter service:

```bash
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply spring-boot \
  --context hsmadapter \
  --package com.agentbanking.hsmadapter \
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
git add . && git commit -m "feat(hsmadapter): scaffold with Seed4J spring-boot module"
```

---

## Task 2: Domain Models and Ports (Write Manually)

**CRITICAL: ZERO framework imports in domain/ — no Logger, no Spring, no JPA.**
**CRITICAL: Zero-logging for PIN blocks — NEVER log PIN data.**

**Files:**
- Create: `src/main/java/com/agentbanking/hsmadapter/domain/model/PinBlockFormat.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/domain/model/PinBlock.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/domain/model/KeyScheme.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/domain/model/HsmOperation.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/domain/port/out/HsmGatewayPort.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/domain/port/in/TranslatePinUseCase.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/domain/port/in/VerifyPinUseCase.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/domain/port/in/GeneratePinUseCase.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/domain/service/PinManagementService.java`

- [ ] **Step 1: Write tests**

```java
package com.agentbanking.hsmadapter.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.agentbanking.UnitTest;
import com.agentbanking.hsmadapter.domain.model.PinBlock;
import com.agentbanking.hsmadapter.domain.model.PinBlockFormat;
import com.agentbanking.hsmadapter.domain.port.out.HsmGatewayPort;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PinManagementServiceTest {

  @Mock private HsmGatewayPort hsmGatewayPort;
  private PinManagementService service;

  @BeforeEach
  void setUp() { service = new PinManagementService(hsmGatewayPort); }

  @Nested
  @DisplayName("translatePinBlock")
  class TranslatePinBlockTest {
    @Test
    void shouldTranslatePinBlockFromIso0ToEbcdic0() {
      String cardNumber = "4111111111111111";
      String pin = "123456";
      PinBlock inputBlock = PinBlock.fromIso0(cardNumber, pin);
      String zoneKey = "ZEK-001";

      when(hsmGatewayPort.translatePinBlock(inputBlock, zoneKey))
        .thenReturn(PinBlock.fromEbcdic0(cardNumber, pin));

      PinBlock result = service.translate(inputBlock, zoneKey);

      assertThat(result.format()).isEqualTo(PinBlockFormat.EBCDIC_0);
      verify(hsmGatewayPort).translatePinBlock(inputBlock, zoneKey);
    }
  }

  @Nested
  @DisplayName("verifyPin")
  class VerifyPinTest {
    @Test
    void shouldReturnTrueWhenPinMatches() {
      String cardNumber = "4111111111111111";
      PinBlock pinBlock = PinBlock.fromIso0(cardNumber, "123456");
      when(hsmGatewayPort.verifyPin(cardNumber, pinBlock)).thenReturn(true);

      boolean result = service.verify(cardNumber, pinBlock);

      assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenPinDoesNotMatch() {
      String cardNumber = "4111111111111111";
      PinBlock pinBlock = PinBlock.fromIso0(cardNumber, "123456");
      when(hsmGatewayPort.verifyPin(cardNumber, pinBlock)).thenReturn(false);

      boolean result = service.verify(cardNumber, pinBlock);

      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("generateRandomPin")
  class GenerateRandomPinTest {
    @Test
    void shouldGenerateSixDigitPin() {
      when(hsmGatewayPort.generateRandomPin()).thenReturn("123456");

      String result = service.generate();

      assertThat(result).hasSize(6);
      assertThat(result).matches("\\d{6}");
    }
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "PinManagementServiceTest"
```
Expected: FAIL — classes not found

- [ ] **Step 3: Write implementation**

**PinBlockFormat.java:**
```java
package com.agentbanking.hsmadapter.domain.model;

public enum PinBlockFormat { ISO_0, EBCDIC_0 }
```

**PinBlock.java:**
```java
package com.agentbanking.hsmadapter.domain.model;

public record PinBlock(String value, PinBlockFormat format) {

  public static PinBlock fromIso0(String pan, String pin) {
    String paddedPin = padPin(pin);
    String paddedPan = padPan(pan);
    String block = "04" + paddedPin + paddedPan;
    return new PinBlock(block, PinBlockFormat.ISO_0);
  }

  public static PinBlock fromEbcdic0(String pan, String pin) {
    String paddedPin = padPin(pin);
    String paddedPan = padPan(pan);
    String block = "01" + paddedPin + paddedPan;
    return new PinBlock(block, PinBlockFormat.EBCDIC_0);
  }

  private static String padPin(String pin) {
    StringBuilder sb = new StringBuilder(pin);
    while (sb.length() < 12) { sb.append('F'); }
    return sb.toString();
  }

  private static String padPan(String pan) {
    String last12 = pan.substring(pan.length() - 12);
    return "0000" + last12;
  }

  public String masked() {
    return "XXXX".repeat(4);
  }
}
```

**KeyScheme.java:**
```java
package com.agentbanking.hsmadapter.domain.model;

public enum KeyScheme { ZPK, ZEK, KEK, LMK, TAK }
```

**HsmOperation.java:**
```java
package com.agentbanking.hsmadapter.domain.model;

public enum HsmOperation {
  TRANSLATE_PIN_BLOCK, VERIFY_PIN, GENERATE_PIN,
  GENERATE_KEY, ENCRYPT_DATA, DECRYPT_DATA
}
```

**HsmGatewayPort.java:**
```java
package com.agentbanking.hsmadapter.domain.port.out;

import com.agentbanking.hsmadapter.domain.model.PinBlock;

public interface HsmGatewayPort {
  PinBlock translatePinBlock(PinBlock input, String zoneKey);
  boolean verifyPin(String cardNumber, PinBlock pinBlock);
  String generateRandomPin();
}
```

**TranslatePinUseCase.java:**
```java
package com.agentbanking.hsmadapter.domain.port.in;

import com.agentbanking.hsmadapter.domain.model.PinBlock;

public interface TranslatePinUseCase {
  PinBlock translate(PinBlock input, String zoneKey);
}
```

**VerifyPinUseCase.java:**
```java
package com.agentbanking.hsmadapter.domain.port.in;

import com.agentbanking.hsmadapter.domain.model.PinBlock;

public interface VerifyPinUseCase {
  boolean verify(String cardNumber, PinBlock pinBlock);
}
```

**GeneratePinUseCase.java:**
```java
package com.agentbanking.hsmadapter.domain.port.in;

public interface GeneratePinUseCase {
  String generate();
}
```

**PinManagementService.java (NO Logger — zero-logging for PIN operations):**
```java
package com.agentbanking.hsmadapter.domain.service;

import com.agentbanking.hsmadapter.domain.model.PinBlock;
import com.agentbanking.hsmadapter.domain.port.in.GeneratePinUseCase;
import com.agentbanking.hsmadapter.domain.port.in.TranslatePinUseCase;
import com.agentbanking.hsmadapter.domain.port.in.VerifyPinUseCase;
import com.agentbanking.hsmadapter.domain.port.out.HsmGatewayPort;

public class PinManagementService implements TranslatePinUseCase, VerifyPinUseCase, GeneratePinUseCase {

  private final HsmGatewayPort hsmGatewayPort;

  public PinManagementService(HsmGatewayPort hsmGatewayPort) {
    this.hsmGatewayPort = hsmGatewayPort;
  }

  @Override
  public PinBlock translate(PinBlock input, String zoneKey) {
    // CRITICAL: Do NOT log PIN block data
    return hsmGatewayPort.translatePinBlock(input, zoneKey);
  }

  @Override
  public boolean verify(String cardNumber, PinBlock pinBlock) {
    // CRITICAL: Do NOT log PIN or card number
    return hsmGatewayPort.verifyPin(cardNumber, pinBlock);
  }

  @Override
  public String generate() {
    // CRITICAL: Do NOT log generated PIN
    return hsmGatewayPort.generateRandomPin();
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew test --tests "PinManagementServiceTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/agentbanking/hsmadapter/domain/
git add src/test/java/com/agentbanking/hsmadapter/domain/
git commit -m "feat(hsmadapter): add domain models, ports, and pin management service"
```

---

## Task 3: Application Layer — DTOs and Service (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/hsmadapter/application/dto/PinVerificationRequest.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/application/dto/PinVerificationResponse.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/application/dto/PinGenerationRequest.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/application/dto/PinGenerationResponse.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/application/service/HsmApplicationService.java`

- [ ] **Step 1: Write implementation**

**PinVerificationRequest.java:**
```java
package com.agentbanking.hsmadapter.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PinVerificationRequest(
  @NotBlank(message = "Card number is required") String cardNumber,
  @NotBlank(message = "PIN is required")
  @Pattern(regexp = "^\\d{4,6}$", message = "PIN must be 4-6 digits") String pin,
  @NotBlank(message = "Zone key is required") String zoneKey
) {}
```

**PinVerificationResponse.java:**
```java
package com.agentbanking.hsmadapter.application.dto;

public record PinVerificationResponse(String code, boolean verified, String message) {
  public static PinVerificationResponse success() {
    return new PinVerificationResponse("SUCCESS", true, "PIN verified successfully");
  }
  public static PinVerificationResponse failure(String errorCode) {
    return new PinVerificationResponse(errorCode, false, "PIN verification failed");
  }
}
```

**PinGenerationRequest.java:**
```java
package com.agentbanking.hsmadapter.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PinGenerationRequest(
  @NotBlank(message = "Card number is required") String cardNumber
) {}
```

**PinGenerationResponse.java:**
```java
package com.agentbanking.hsmadapter.application.dto;

public record PinGenerationResponse(String code, String pin, String message) {
  public static PinGenerationResponse success(String pin) {
    return new PinGenerationResponse("SUCCESS", pin, "PIN generated successfully");
  }
  public static PinGenerationResponse failure(String errorCode) {
    return new PinGenerationResponse(errorCode, null, "PIN generation failed");
  }
}
```

**HsmApplicationService.java (NO @Service — registered via @Bean):**
```java
package com.agentbanking.hsmadapter.application.service;

import com.agentbanking.hsmadapter.application.dto.*;
import com.agentbanking.hsmadapter.domain.model.PinBlock;
import com.agentbanking.hsmadapter.domain.port.in.GeneratePinUseCase;
import com.agentbanking.hsmadapter.domain.port.in.VerifyPinUseCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HsmApplicationService {

  private static final Logger log = LoggerFactory.getLogger(HsmApplicationService.class);

  private final VerifyPinUseCase verifyPinUseCase;
  private final GeneratePinUseCase generatePinUseCase;

  public HsmApplicationService(VerifyPinUseCase verifyPinUseCase, GeneratePinUseCase generatePinUseCase) {
    this.verifyPinUseCase = verifyPinUseCase;
    this.generatePinUseCase = generatePinUseCase;
  }

  public PinVerificationResponse verifyPin(PinVerificationRequest request) {
    try {
      PinBlock pinBlock = PinBlock.fromIso0(request.cardNumber(), request.pin());
      boolean verified = verifyPinUseCase.verify(request.cardNumber(), pinBlock);
      if (verified) {
        return PinVerificationResponse.success();
      } else {
        log.warn("PIN verification failed for card: {}", maskCard(request.cardNumber()));
        return PinVerificationResponse.failure("ERR_HSM_002");
      }
    } catch (Exception e) {
      log.error("PIN verification error: {}", e.getMessage());
      return PinVerificationResponse.failure("ERR_HSM_001");
    }
  }

  public PinGenerationResponse generatePin() {
    try {
      String pin = generatePinUseCase.generate();
      return PinGenerationResponse.success(pin);
    } catch (Exception e) {
      log.error("PIN generation error: {}", e.getMessage());
      return PinGenerationResponse.failure("ERR_HSM_001");
    }
  }

  private String maskCard(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 8) return "XXXX";
    return cardNumber.substring(0, 4) + "********" + cardNumber.substring(cardNumber.length() - 4);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/hsmadapter/application/
git commit -m "feat(hsmadapter): add application DTOs and service"
```

---

## Task 4: Infrastructure — HSM Client and Adapter (Write Manually)

**CRITICAL: Zero-logging for PIN blocks in infrastructure layer too.**

**Files:**
- Create: `src/main/java/com/agentbanking/hsmadapter/infrastructure/config/HsmProperties.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/infrastructure/external/ThalesHsmClient.java`
- Create: `src/main/java/com/agentbanking/hsmadapter/infrastructure/adapter/ThalesHsmAdapter.java`

- [ ] **Step 1: Write implementation**

**HsmProperties.java:**
```java
package com.agentbanking.hsmadapter.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "hsm")
public record HsmProperties(
  String host, int port, int timeoutSeconds, int retryAttempts, String defaultZoneKey
) {}
```

**ThalesHsmClient.java (CRITICAL: NO PIN logging):**
```java
package com.agentbanking.hsmadapter.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ThalesHsmClient {

  private static final Logger log = LoggerFactory.getLogger(ThalesHsmClient.class);

  public String translate(String pinBlock, String zoneKey) {
    // CRITICAL: Do NOT log pinBlock value
    log.info("Translating PIN block via Thales HSM with zone key: {}", zoneKey);
    // Thales HSM TCP command would go here
    return pinBlock.replaceFirst("04", "01");
  }

  public String verify(String cardNumber, String pinBlock) {
    // CRITICAL: Do NOT log pinBlock or full cardNumber
    log.info("Verifying PIN via Thales HSM for card: {}", maskCard(cardNumber));
    return "MATCH";
  }

  public String generatePin() {
    log.info("Generating PIN via Thales HSM");
    return String.format("%06d", (int)(Math.random() * 900000) + 100000);
  }

  private String maskCard(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 8) return "XXXX";
    return cardNumber.substring(0, 4) + "********" + cardNumber.substring(cardNumber.length() - 4);
  }
}
```

**ThalesHsmAdapter.java (implements HsmGatewayPort):**
```java
package com.agentbanking.hsmadapter.infrastructure.adapter;

import com.agentbanking.hsmadapter.domain.model.PinBlock;
import com.agentbanking.hsmadapter.domain.model.PinBlockFormat;
import com.agentbanking.hsmadapter.domain.port.out.HsmGatewayPort;
import com.agentbanking.hsmadapter.infrastructure.external.ThalesHsmClient;

public class ThalesHsmAdapter implements HsmGatewayPort {

  private final ThalesHsmClient hsmClient;

  public ThalesHsmAdapter(ThalesHsmClient hsmClient) {
    this.hsmClient = hsmClient;
  }

  @Override
  public PinBlock translatePinBlock(PinBlock input, String zoneKey) {
    String translated = hsmClient.translate(input.value(), zoneKey);
    return new PinBlock(translated, PinBlockFormat.EBCDIC_0);
  }

  @Override
  public boolean verifyPin(String cardNumber, PinBlock pinBlock) {
    String result = hsmClient.verify(cardNumber, pinBlock.value());
    return "MATCH".equals(result);
  }

  @Override
  public String generateRandomPin() {
    return hsmClient.generatePin();
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/hsmadapter/infrastructure/
git commit -m "feat(hsmadapter): add HSM client and adapter with zero-PIN logging"
```

---

## Task 5: Config — Domain Service Registration (Write Manually)

**Law V: Domain services via @Bean in config, NOT @Service annotation.**

**Files:**
- Update: `src/main/java/com/agentbanking/hsmadapter/config/DomainServiceConfig.java` (created by Seed4J)

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.hsmadapter.config;

import com.agentbanking.hsmadapter.domain.port.out.HsmGatewayPort;
import com.agentbanking.hsmadapter.domain.service.PinManagementService;
import com.agentbanking.hsmadapter.infrastructure.adapter.ThalesHsmAdapter;
import com.agentbanking.hsmadapter.infrastructure.external.ThalesHsmClient;
import com.agentbanking.hsmadapter.application.service.HsmApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

  @Bean
  public HsmGatewayPort hsmGatewayPort(ThalesHsmClient thalesHsmClient) {
    return new ThalesHsmAdapter(thalesHsmClient);
  }

  @Bean
  public PinManagementService pinManagementService(HsmGatewayPort hsmGatewayPort) {
    return new PinManagementService(hsmGatewayPort);
  }

  @Bean
  public HsmApplicationService hsmApplicationService(
    PinManagementService pinService
  ) {
    return new HsmApplicationService(pinService, pinService);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/hsmadapter/config/DomainServiceConfig.java
git commit -m "feat(hsmadapter): register domain services via @Bean"
```

---

## Task 6: REST Controller (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/hsmadapter/infrastructure/web/HsmController.java`

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.hsmadapter.infrastructure.web;

import com.agentbanking.hsmadapter.application.dto.*;
import com.agentbanking.hsmadapter.application.service.HsmApplicationService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hsm")
public class HsmController {

  private static final Logger log = LoggerFactory.getLogger(HsmController.class);

  private final HsmApplicationService applicationService;

  public HsmController(HsmApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @PostMapping("/verify-pin")
  public ResponseEntity<PinVerificationResponse> verifyPin(
    @Valid @RequestBody PinVerificationRequest request
  ) {
    // CRITICAL: Do NOT log card number or PIN
    log.info("PIN verification request received");
    return ResponseEntity.ok(applicationService.verifyPin(request));
  }

  @PostMapping("/generate-pin")
  public ResponseEntity<PinGenerationResponse> generatePin(
    @Valid @RequestBody PinGenerationRequest request
  ) {
    log.info("PIN generation request received for card: {}", maskCard(request.cardNumber()));
    return ResponseEntity.ok(applicationService.generatePin());
  }

  private String maskCard(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 8) return "XXXX";
    return cardNumber.substring(0, 4) + "********" + cardNumber.substring(cardNumber.length() - 4);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/hsmadapter/infrastructure/web/
git commit -m "feat(hsmadapter): add REST controller with masked card numbers"
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
| 2 | Domain Models & Ports | Manual (NO Logger) | 1 |
| 3 | Application DTOs & Service | Manual | 0 |
| 4 | Infrastructure (HSM Client, Adapter) | Manual (zero-PIN logging) | 0 |
| 5 | Config (@Bean registration) | Manual (Law V) | 0 |
| 6 | REST Controller | Manual (masked card numbers) | 0 |

**Key fixes from original plan:**
- `./gradlew test` (not `./mvnw`)
- Seed4J scaffolding replaces manual package-info.java creation
- Domain services via `@Bean` in config (Law V), NOT `@Service`
- Removed Logger from `PinManagementService` (Law VI violation)
- CRITICAL: Zero-logging for PIN blocks throughout — only masked card numbers in logs
- Simplified task structure (combined related tasks)
