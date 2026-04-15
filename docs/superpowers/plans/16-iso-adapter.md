# 16. ISO 8583 Adapter Sub-Plan

> **Bounding Context:** com.agentbanking.isoadapter
> **Wave:** 5.2
> **Depends On:** 01 (error registry), 02 (domain model)
> **BDD Scenarios:** BDD-ISO01, BDD-ISO01-EC-01, BDD-ISO01-EC-02, BDD-ISO02, BDD-ISO02-EC-01, BDD-ISO03, BDD-ISO03-EC-01
> **BRD Requirements:** US-ISO01, FR-20.1 through FR-20.5

**Goal:** Implement ISO 8583 message builder/parser with field mapping, TCP socket connection to PayNet, STAN generation, and MTI handling.

**Architecture:** Wave 5 — Tier 4 Adapter. Hexagonal architecture. Translates internal JSON transaction requests to ISO 8583 binary format for payment networks (PayNet). Parses ISO 8583 responses back to internal format.

**Tech Stack:** Java 21, Spring Boot 4, Spring Cloud OpenFeign, Resilience4j, JUnit 5, Mockito, ArchUnit, Gradle

---

## Task 1: Seed4J Scaffolding

Run Seed4J CLI to scaffold the ISO adapter service:

```bash
# Apply spring-boot module for basic service structure
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply spring-boot \
  --context isoadapter \
  --package com.agentbanking.isoadapter \
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
git add . && git commit -m "feat(isoadapter): scaffold with Seed4J spring-boot module"
```

---

## Task 2: Domain Models (Write Manually)

**CRITICAL: ZERO framework imports in domain/ — no Logger, no Spring, no JPA.**

**Files:**
- Create: `src/main/java/com/agentbanking/isoadapter/domain/model/IsoMessage.java`
- Create: `src/main/java/com/agentbanking/isoadapter/domain/model/IsoField.java`
- Create: `src/main/java/com/agentbanking/isoadapter/domain/model/IsoMessageType.java`

- [ ] **Step 1: Write implementation**

**IsoMessage.java:**
```java
package com.agentbanking.isoadapter.domain.model;

import java.util.Map;

public record IsoMessage(String mti, Map<Integer, String> fields, byte[] bitmap) {}
```

**IsoField.java:**
```java
package com.agentbanking.isoadapter.domain.model;

public record IsoField(int number, String name, String value, int format, int length) {
  public static final int FORMAT_ALPHA = 0;
  public static final int FORMAT_NUMERIC = 1;
  public static final int FORMAT_BINARY = 2;
  public static final int FORMAT_AMOUNT = 3;
}
```

**IsoMessageType.java:**
```java
package com.agentbanking.isoadapter.domain.model;

public enum IsoMessageType {
  MTI_0100_AUTHORIZATION_REQUEST("0100"),
  MTI_0110_AUTHORIZATION_RESPONSE("0110"),
  MTI_0120_REVERSAL_REQUEST("0120"),
  MTI_0121_REVERSAL_RESPONSE("0121"),
  MTI_0200_NETWORK_MANAGEMENT("0200"),
  MTI_0210_NETWORK_MANAGEMENT_RESPONSE("0210"),
  MTI_0400_REVERSAL("0400"),
  MTI_0410_REVERSAL_RESPONSE("0410");

  private final String value;

  IsoMessageType(String value) { this.value = value; }
  public String getValue() { return value; }
  public boolean isRequest() { return value.endsWith("00"); }
  public String getResponseMti() { return String.valueOf(Integer.parseInt(value) + 10); }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/isoadapter/domain/model/
git commit -m "feat(isoadapter): add domain models"
```

---

## Task 3: Domain Ports and Services (Write Manually)

**CRITICAL: NO Logger in domain layer.**

**Files:**
- Create: `src/main/java/com/agentbanking/isoadapter/domain/port/in/TranslateToIsoUseCase.java`
- Create: `src/main/java/com/agentbanking/isoadapter/domain/port/in/TranslateFromIsoUseCase.java`
- Create: `src/main/java/com/agentbanking/isoadapter/domain/port/out/SwitchPort.java`
- Create: `src/main/java/com/agentbanking/isoadapter/domain/service/IsoTranslationService.java`

- [ ] **Step 1: Write implementation**

**TranslateToIsoUseCase.java:**
```java
package com.agentbanking.isoadapter.domain.port.in;

import com.agentbanking.isoadapter.domain.model.IsoMessage;
import com.agentbanking.isoadapter.application.dto.InternalTransactionRequest;

public interface TranslateToIsoUseCase {
  IsoMessage translate(InternalTransactionRequest request);
}
```

**TranslateFromIsoUseCase.java:**
```java
package com.agentbanking.isoadapter.domain.port.in;

import com.agentbanking.isoadapter.application.dto.IsoTransactionResponse;
import com.agentbanking.isoadapter.domain.model.IsoMessage;

public interface TranslateFromIsoUseCase {
  IsoTransactionResponse translate(IsoMessage message);
}
```

**SwitchPort.java:**
```java
package com.agentbanking.isoadapter.domain.port.out;

import com.agentbanking.isoadapter.domain.model.IsoMessage;

public interface SwitchPort {
  IsoMessage send(IsoMessage message);
  IsoMessage sendReversal(IsoMessage originalMessage);
}
```

**IsoTranslationService.java (NO Logger):**
```java
package com.agentbanking.isoadapter.domain.service;

import com.agentbanking.isoadapter.application.dto.InternalTransactionRequest;
import com.agentbanking.isoadapter.application.dto.IsoTransactionResponse;
import com.agentbanking.isoadapter.domain.model.IsoMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class IsoTranslationService {

  private static final AtomicInteger stanCounter = new AtomicInteger(1);

  public IsoMessage translateToIso(InternalTransactionRequest request) {
    String mti = getMtiForTransaction(request.transactionType());
    Map<Integer, String> fields = new HashMap<>();

    fields.put(2, request.pan());
    fields.put(3, getProcessingCode(request.transactionType()));
    fields.put(4, formatAmount(request.amount()));
    fields.put(7, formatTransmissionDateTime());
    fields.put(11, generateStan());
    fields.put(12, formatLocalTime());
    fields.put(13, formatLocalDate());
    fields.put(32, request.acquiringInstitutionId());
    fields.put(37, request.retrievalReferenceNumber());
    fields.put(41, request.terminalId());
    fields.put(42, request.merchantId());

    return new IsoMessage(mti, fields, null);
  }

  public IsoTransactionResponse translateFromIso(IsoMessage message) {
    String responseCode = message.fields().get(39);
    String stan = message.fields().get(11);
    String rrn = message.fields().get(37);

    boolean authorized = "00".equals(responseCode);
    String declineReason = getDeclineReason(responseCode);

    return new IsoTransactionResponse(authorized, responseCode, stan, rrn, declineReason);
  }

  private String getMtiForTransaction(String transactionType) {
    return switch (transactionType) {
      case "CASH_WITHDRAWAL", "CASH_DEPOSIT", "PAYMENT" -> "0100";
      case "REVERSAL" -> "0400";
      default -> "0100";
    };
  }

  private String getProcessingCode(String transactionType) {
    return switch (transactionType) {
      case "CASH_WITHDRAWAL" -> "00";
      case "CASH_DEPOSIT" -> "01";
      case "PAYMENT" -> "00";
      default -> "00";
    };
  }

  private String formatAmount(BigDecimal amount) {
    return String.format("%012d", amount.intValue());
  }

  private String formatTransmissionDateTime() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
  }

  private String formatLocalTime() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
  }

  private String formatLocalDate() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
  }

  private String generateStan() {
    int stan = stanCounter.getAndIncrement();
    if (stan > 999999) { stanCounter.set(1); stan = 1; }
    return String.format("%06d", stan);
  }

  private String getDeclineReason(String responseCode) {
    return switch (responseCode) {
      case "00" -> null;
      case "05" -> "Do Not Honor";
      case "13" -> "Invalid Amount";
      case "51" -> "Insufficient Funds";
      case "54" -> "Expired Card";
      case "91" -> "Issuer Unavailable";
      default -> "Unknown error";
    };
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/isoadapter/domain/
git commit -m "feat(isoadapter): add domain ports and translation service"
```

---

## Task 4: Application Layer — DTOs and Service (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/isoadapter/application/dto/InternalTransactionRequest.java`
- Create: `src/main/java/com/agentbanking/isoadapter/application/dto/IsoTransactionResponse.java`
- Create: `src/main/java/com/agentbanking/isoadapter/application/service/IsoAdapterApplicationService.java`

- [ ] **Step 1: Write implementation**

**InternalTransactionRequest.java:**
```java
package com.agentbanking.isoadapter.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InternalTransactionRequest(
  @NotBlank String transactionType, @NotBlank String pan,
  @NotNull @Positive BigDecimal amount, @NotBlank String terminalId,
  @NotBlank String merchantId, String retrievalReferenceNumber,
  String acquiringInstitutionId
) {}
```

**IsoTransactionResponse.java:**
```java
package com.agentbanking.isoadapter.application.dto;

public record IsoTransactionResponse(
  boolean authorized, String responseCode, String stan,
  String retrievalReferenceNumber, String declineReason
) {
  public static IsoTransactionResponse approved(String stan, String rrn) {
    return new IsoTransactionResponse(true, "00", stan, rrn, null);
  }
  public static IsoTransactionResponse declined(String code, String reason) {
    return new IsoTransactionResponse(false, code, null, null, reason);
  }
}
```

**IsoAdapterApplicationService.java (NO @Service — registered via @Bean):**
```java
package com.agentbanking.isoadapter.application.service;

import com.agentbanking.isoadapter.application.dto.InternalTransactionRequest;
import com.agentbanking.isoadapter.application.dto.IsoTransactionResponse;
import com.agentbanking.isoadapter.domain.model.IsoMessage;
import com.agentbanking.isoadapter.domain.port.out.SwitchPort;
import com.agentbanking.isoadapter.domain.service.IsoTranslationService;

public class IsoAdapterApplicationService {

  private final IsoTranslationService translationService;
  private final SwitchPort switchPort;

  public IsoAdapterApplicationService(IsoTranslationService translationService, SwitchPort switchPort) {
    this.translationService = translationService;
    this.switchPort = switchPort;
  }

  public IsoTransactionResponse processTransaction(InternalTransactionRequest request) {
    IsoMessage isoMessage = translationService.translateToIso(request);
    IsoMessage response = switchPort.send(isoMessage);
    return translationService.translateFromIso(response);
  }

  public IsoTransactionResponse processReversal(InternalTransactionRequest request) {
    IsoMessage isoMessage = translationService.translateToIso(request);
    IsoMessage response = switchPort.sendReversal(isoMessage);
    return translationService.translateFromIso(response);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/isoadapter/application/
git commit -m "feat(isoadapter): add application DTOs and service"
```

---

## Task 5: Infrastructure — Codec, Socket, Feign Client (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/isoadapter/infrastructure/codec/IsoMessageCodec.java`
- Create: `src/main/java/com/agentbanking/isoadapter/infrastructure/socket/IsoSocketClient.java`
- Create: `src/main/java/com/agentbanking/isoadapter/infrastructure/external/SwitchFeignClient.java`
- Create: `src/main/java/com/agentbanking/isoadapter/infrastructure/adapter/SwitchPortAdapter.java`

- [ ] **Step 1: Write implementation**

**IsoMessageCodec.java:**
```java
package com.agentbanking.isoadapter.infrastructure.codec;

import com.agentbanking.isoadapter.domain.model.IsoMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class IsoMessageCodec {

  public byte[] encode(IsoMessage message) {
    StringBuilder sb = new StringBuilder();
    sb.append(message.mti());
    byte[] bitmap = buildBitmap(message.fields().keySet());
    sb.append(new String(bitmap, StandardCharsets.UTF_8));
    for (int fieldNum = 1; fieldNum <= 128; fieldNum++) {
      if (message.fields().containsKey(fieldNum)) {
        sb.append(message.fields().get(fieldNum));
      }
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  public IsoMessage decode(byte[] data) {
    String raw = new String(data, StandardCharsets.UTF_8);
    String mti = raw.substring(0, 4);
    int bitmapStart = 4;
    byte[] bitmap = raw.substring(bitmapStart, bitmapStart + 16).getBytes();
    Map<Integer, String> fields = parseFields(raw, bitmapStart + 16);
    return new IsoMessage(mti, fields, bitmap);
  }

  private byte[] buildBitmap(Set<Integer> fieldNumbers) {
    byte[] bitmap = new byte[16];
    for (int fieldNum : fieldNumbers) {
      if (fieldNum >= 1 && fieldNum <= 128) {
        int byteIndex = (fieldNum - 1) / 8;
        int bitIndex = (fieldNum - 1) % 8;
        bitmap[byteIndex] |= (1 << (7 - bitIndex));
      }
    }
    return bitmap;
  }

  private Map<Integer, String> parseFields(String data, int startIndex) {
    Map<Integer, String> fields = new HashMap<>();
    fields.put(2, data.substring(startIndex, startIndex + 19));
    fields.put(3, data.substring(startIndex + 19, startIndex + 25));
    fields.put(4, data.substring(startIndex + 25, startIndex + 37));
    fields.put(7, data.substring(startIndex + 37, startIndex + 49));
    fields.put(11, data.substring(startIndex + 49, startIndex + 55));
    return fields;
  }
}
```

**IsoSocketClient.java:**
```java
package com.agentbanking.isoadapter.infrastructure.socket;

import com.agentbanking.isoadapter.domain.model.IsoMessage;
import com.agentbanking.isoadapter.infrastructure.codec.IsoMessageCodec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Duration;

@Component
public class IsoSocketClient {

  private static final Logger log = LoggerFactory.getLogger(IsoSocketClient.class);
  private static final int BUFFER_SIZE = 4096;

  @Value("${agentbanking.switch.host:paynet.local}")
  private String host;

  @Value("${agentbanking.switch.port:3000}")
  private int port;

  private final IsoMessageCodec codec = new IsoMessageCodec();

  public IsoMessage send(IsoMessage message) throws IOException {
    try (SocketChannel channel = SocketChannel.open()) {
      channel.configureBlocking(true);
      channel.setOption(java.net.StandardSocketOptions.TCP_NODELAY, true);

      if (!channel.connect(new InetSocketAddress(host, port))) {
        throw new IOException("Failed to connect to PayNet at " + host + ":" + port);
      }

      byte[] request = codec.encode(message);
      channel.write(ByteBuffer.wrap(request));

      ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
      channel.read(readBuffer);

      return codec.decode(readBuffer.array());
    }
  }
}
```

**SwitchFeignClient.java (FIX: url from config property):**
```java
package com.agentbanking.isoadapter.infrastructure.external;

import com.agentbanking.isoadapter.application.dto.InternalTransactionRequest;
import com.agentbanking.isoadapter.application.dto.IsoTransactionResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
  name = "switch-client",
  url = "${agentbanking.switch.url:http://switch-adapter:8080}"
)
public interface SwitchFeignClient {

  @PostMapping("/api/v1/switch/send")
  IsoTransactionResponse sendTransaction(@RequestBody InternalTransactionRequest request);

  @PostMapping("/api/v1/switch/reversal")
  IsoTransactionResponse sendReversal(@RequestBody InternalTransactionRequest request);
}
```

**SwitchPortAdapter.java (implements SwitchPort):**
```java
package com.agentbanking.isoadapter.infrastructure.adapter;

import com.agentbanking.isoadapter.domain.model.IsoMessage;
import com.agentbanking.isoadapter.domain.port.out.SwitchPort;
import com.agentbanking.isoadapter.infrastructure.external.SwitchFeignClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwitchPortAdapter implements SwitchPort {

  private static final Logger log = LoggerFactory.getLogger(SwitchPortAdapter.class);

  private final SwitchFeignClient feignClient;

  public SwitchPortAdapter(SwitchFeignClient feignClient) {
    this.feignClient = feignClient;
  }

  @Override
  public IsoMessage send(IsoMessage message) {
    log.info("Sending ISO message to switch: MTI={}", message.mti());
    // TODO: Convert IsoMessage to request DTO, call feignClient, convert response
    return message;
  }

  @Override
  public IsoMessage sendReversal(IsoMessage originalMessage) {
    log.info("Sending reversal to switch");
    // TODO: Implement reversal
    return originalMessage;
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/isoadapter/infrastructure/
git commit -m "feat(isoadapter): add codec, socket client, Feign client, and port adapter"
```

---

## Task 6: Config — Domain Service Registration (Write Manually)

**Law V: Domain services via @Bean in config, NOT @Service annotation.**

**Files:**
- Update: `src/main/java/com/agentbanking/isoadapter/config/DomainServiceConfig.java` (created by Seed4J)

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.isoadapter.config;

import com.agentbanking.isoadapter.domain.port.out.SwitchPort;
import com.agentbanking.isoadapter.domain.service.IsoTranslationService;
import com.agentbanking.isoadapter.application.service.IsoAdapterApplicationService;
import com.agentbanking.isoadapter.infrastructure.adapter.SwitchPortAdapter;
import com.agentbanking.isoadapter.infrastructure.external.SwitchFeignClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

  @Bean
  public IsoTranslationService isoTranslationService() {
    return new IsoTranslationService();
  }

  @Bean
  public SwitchPort switchPort(SwitchFeignClient feignClient) {
    return new SwitchPortAdapter(feignClient);
  }

  @Bean
  public IsoAdapterApplicationService isoAdapterApplicationService(
    IsoTranslationService translationService, SwitchPort switchPort
  ) {
    return new IsoAdapterApplicationService(translationService, switchPort);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/isoadapter/config/DomainServiceConfig.java
git commit -m "feat(isoadapter): register domain services via @Bean"
```

---

## Task 7: Application Configuration and Tests

**Files:**
- Create: `src/main/resources/application.yaml`
- Create: `src/test/java/com/agentbanking/isoadapter/domain/service/IsoTranslationServiceTest.java`

- [ ] **Step 1: Write implementation**

**application.yaml:**
```yaml
spring:
  application:
    name: iso-adapter

agentbanking:
  switch:
    host: ${SWITCH_HOST:paynet.local}
    port: ${SWITCH_PORT:3000}
    url: ${SWITCH_URL:http://switch-adapter:8080}
    connection-timeout-seconds: 10
    read-timeout-seconds: 30
    heartbeat-interval-seconds: 60

resilience4j:
  circuitbreaker:
    instances:
      switch:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
```

**IsoTranslationServiceTest.java:**
```java
package com.agentbanking.isoadapter.domain.service;

import com.agentbanking.isoadapter.application.dto.InternalTransactionRequest;
import com.agentbanking.isoadapter.application.dto.IsoTransactionResponse;
import com.agentbanking.isoadapter.domain.model.IsoMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IsoTranslationServiceTest {

  private IsoTranslationService service;

  @BeforeEach
  void setUp() { service = new IsoTranslationService(); }

  @Test
  @DisplayName("should translate withdrawal request to ISO 8583")
  void shouldTranslateWithdrawalToIso() {
    InternalTransactionRequest request = new InternalTransactionRequest(
      "CASH_WITHDRAWAL", "4111111111111111", new BigDecimal("500.00"),
      "TERM001", "MERCH001", null, "ACQ001"
    );

    IsoMessage result = service.translateToIso(request);

    assertEquals("0100", result.mti());
    assertNotNull(result.fields().get(2));
    assertNotNull(result.fields().get(3));
    assertNotNull(result.fields().get(4));
    assertNotNull(result.fields().get(11));
  }

  @Test
  @DisplayName("should translate ISO response with approved code")
  void shouldTranslateApprovedResponse() {
    IsoMessage message = new IsoMessage("0110", Map.of(39, "00", 11, "000001", 37, "RRN001"), null);

    IsoTransactionResponse result = service.translateFromIso(message);

    assertTrue(result.authorized());
    assertEquals("00", result.responseCode());
    assertEquals("000001", result.stan());
  }

  @Test
  @DisplayName("should translate ISO response with decline code")
  void shouldTranslateDeclinedResponse() {
    IsoMessage message = new IsoMessage("0110", Map.of(39, "51", 11, "000001", 37, "RRN001"), null);

    IsoTransactionResponse result = service.translateFromIso(message);

    assertFalse(result.authorized());
    assertEquals("51", result.responseCode());
    assertEquals("Insufficient Funds", result.declineReason());
  }
}
```

- [ ] **Step 2: Run tests**

```bash
./gradlew test --tests "IsoTranslationServiceTest"
```
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yaml
git add src/test/java/com/agentbanking/isoadapter/
git commit -m "feat(isoadapter): add application config and unit tests"
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
| 5 | Infrastructure (Codec, Socket, Feign) | Manual | 0 |
| 6 | Config (@Bean registration) | Manual (Law V) | 0 |
| 7 | Application Config + Tests | Manual | 1 |

**Key fixes from original plan:**
- `./gradlew test` (not `./mvnw`)
- Seed4J scaffolding replaces manual package-info.java creation
- Domain services via `@Bean` in config (Law V), NOT `@Service`
- Removed Logger from `IsoTranslationService` (Law VI violation)
- Feign URL from config property `${agentbanking.switch.url}`
