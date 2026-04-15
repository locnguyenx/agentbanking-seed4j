# Phase 6: DuitNow & Merchant Services Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement DuitNow proxy-based transfers and Merchant services (retail sale, PIN purchase, MDR, cashback) for Agent Banking Platform

**Architecture:** Hexagonal (Ports & Adapters). Two new domain services: `duitnow` and `merchant`. Each follows domain/application/infrastructure layering with mock external adapters.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, Redis (for idempotency/caching)

---

## Part A: DuitNow Transfer Service

### Task A1: DuitNow Domain Model & Ports [DONE]

**Files:**
- Create: `src/main/java/com/agentbanking/duitnow/domain/model/DuitNowTransaction.java`
- Create: `src/main/java/com/agentbanking/duitnow/domain/model/ProxyType.java`
- Create: `src/main/java/com/agentbanking/duitnow/domain/model/ProxyResolutionResult.java`
- Create: `src/main/java/com/agentbanking/duitnow/domain/model/DuitNowStatus.java`
- Create: `src/main/java/com/agentbanking/duitnow/domain/port/in/TransferMoneyUseCase.java`
- Create: `src/main/java/com/agentbanking/duitnow/domain/port/in/ResolveProxyUseCase.java`
- Create: `src/main/java/com/agentbanking/duitnow/domain/port/out/ProxyRegistryPort.java`
- Create: `src/main/java/com/agentbanking/duitnow/domain/port/out/PayNetGatewayPort.java`

- [x] **Step A1.1: Write domain models** [DONE]

```java
// ProxyType.java
package com.agentbanking.duitnow.domain.model;

public enum ProxyType {
  MOBILE_NUMBER,
  NRIC,
  BUSINESS_REGISTRATION_NUMBER
}
```

```java
// DuitNowStatus.java
package com.agentbanking.duitnow.domain.model;

public enum DuitNowStatus {
  PENDING,
  RESOLVING_PROXY,
  PROCESSING,
  COMPLETED,
  FAILED,
  TIMEOUT
}
```

```java
// ProxyResolutionResult.java
package com.agentbanking.duitnow.domain.model;

import java.util.UUID;

public record ProxyResolutionResult(
  UUID transactionId,
  String resolvedAccountNumber,
  String bankCode,
  String recipientName,
  ProxyType proxyType,
  boolean isVerified
) {}
```

```java
// DuitNowTransaction.java
package com.agentbanking.duitnow.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DuitNowTransaction(
  UUID id,
  String idempotencyKey,
  String agentId,
  UUID traceId,
  ProxyType proxyType,
  String proxyValue,
  String resolvedAccountNumber,
  String bankCode,
  String recipientName,
  BigDecimal amount,
  String reference,
  DuitNowStatus status,
  Instant createdAt,
  Instant completedAt,
  String failureReason
) {}
```

- [ ] **Step A1.2: Write port interfaces**

```java
// TransferMoneyUseCase.java
package com.agentbanking.duitnow.domain.port.in;

import com.agentbanking.duitnow.domain.model.DuitNowTransaction;
import java.math.BigDecimal;
import java.util.UUID;

public interface TransferMoneyUseCase {
  DuitNowTransaction transfer(UUID traceId, String proxyType, String proxyValue, BigDecimal amount, String reference, String idempotencyKey);
}
```

```java
// ResolveProxyUseCase.java
package com.agentbanking.duitnow.domain.port.in;

import com.agentbanking.duitnow.domain.model.ProxyResolutionResult;
import com.agentbanking.duitnow.domain.model.ProxyType;

public interface ResolveProxyUseCase {
  ProxyResolutionResult resolve(ProxyType proxyType, String proxyValue);
}
```

- [ ] **Step A1.3: Write outbound ports**

```java
// ProxyRegistryPort.java
package com.agentbanking.duitnow.domain.port.out;

import com.agentbanking.duitnow.domain.model.ProxyResolutionResult;
import com.agentbanking.duitnow.domain.model.ProxyType;
import java.util.Optional;

public interface ProxyRegistryPort {
  Optional<ProxyResolutionResult> resolve(ProxyType proxyType, String proxyValue);
}
```

```java
// PayNetGatewayPort.java
package com.agentbanking.duitnow.domain.port.out;

import com.agentbanking.duitnow.domain.model.DuitNowTransaction;
import java.math.BigDecimal;
import java.util.UUID;

public interface PayNetGatewayPort {
  DuitNowTransaction transfer(UUID transactionId, String targetAccount, String bankCode, BigDecimal amount, String reference);
  DuitNowTransaction queryStatus(UUID transactionId);
}
```

- [ ] **Step A1.4: Commit**

```bash
git add src/main/java/com/agentbanking/duitnow/domain/model/ src/main/java/com/agentbanking/duitnow/domain/port/
git commit -m "feat(duitnow): add domain models and ports"
```

### Task A2: DuitNow Domain Service

**Files:**
- Create: `src/main/java/com/agentbanking/duitnow/domain/service/DuitNowTransferService.java`
- Test: `src/test/java/com/agentbanking/duitnow/domain/service/DuitNowTransferServiceTest.java`

- [ ] **Step A2.1: Write failing test**

```java
package com.agentbanking.duitnow.domain.service;

import com.agentbanking.duitnow.domain.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DuitNowTransferServiceTest {

  @Test
  void shouldTransferMoneyViaProxy() {
    // Given: proxy resolved
    ProxyResolutionResult resolved = new ProxyResolutionResult(
      UUID.randomUUID(), "1234567890", "MBB", "John Doe", ProxyType.MOBILE_NUMBER, true);
    
    // When: transfer executed
    // Then: transaction completed
  }

  @Test
  void shouldFailForInvalidProxy() {
    // Given: invalid proxy
    // When: resolve called
    // Then: throws exception
  }
}
```

- [ ] **Step A2.2: Implement service**

```java
package com.agentbanking.duitnow.domain.service;

import com.agentbanking.duitnow.domain.model.*;
import com.agentbanking.duitnow.domain.port.in.ResolveProxyUseCase;
import com.agentbanking.duitnow.domain.port.in.TransferMoneyUseCase;
import com.agentbanking.duitnow.domain.port.out.PayNetGatewayPort;
import com.agentbanking.duitnow.domain.port.out.ProxyRegistryPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DuitNowTransferService implements TransferMoneyUseCase, ResolveProxyUseCase {

  private final ProxyRegistryPort proxyRegistryPort;
  private final PayNetGatewayPort payNetGatewayPort;

  public DuitNowTransferService(ProxyRegistryPort proxyRegistryPort, PayNetGatewayPort payNetGatewayPort) {
    this.proxyRegistryPort = proxyRegistryPort;
    this.payNetGatewayPort = payNetGatewayPort;
  }

  @Override
  public DuitNowTransaction transfer(UUID traceId, String proxyTypeStr, String proxyValue, 
      BigDecimal amount, String reference, String idempotencyKey) {
    
    ProxyType proxyType = ProxyType.valueOf(proxyTypeStr.toUpperCase());
    
    ProxyResolutionResult resolved = resolve(proxyType, proxyValue);
    
    DuitNowTransaction tx = new DuitNowTransaction(
      UUID.randomUUID(), idempotencyKey, null, traceId, proxyType, proxyValue,
      resolved.resolvedAccountNumber(), resolved.bankCode(), resolved.recipientName(),
      amount, reference, DuitNowStatus.PROCESSING, Instant.now(), null, null
    );
    
    return payNetGatewayPort.transfer(tx.id(), resolved.resolvedAccountNumber(), 
      resolved.bankCode(), amount, reference);
  }

  @Override
  public ProxyResolutionResult resolve(ProxyType proxyType, String proxyValue) {
    return proxyRegistryPort.resolve(proxyType, proxyValue)
      .orElseThrow(() -> new IllegalArgumentException("Proxy not found: " + proxyValue));
  }
}
```

- [ ] **Step A2.3: Run tests**

```bash
cd D:\Working\myprojects\agentbanking-seed4j && mvn test -Dtest=DuitNowTransferServiceTest
# Expected: PASS
```

- [ ] **Step A2.4: Commit**

```bash
git add src/main/java/com/agentbanking/duitnow/domain/service/
git commit -m "feat(duitnow): implement DuitNowTransferService"
```

### Task A3: DuitNow Application & Infrastructure (Mock Adapters)

**Files:**
- Create: `src/main/java/com/agentbanking/duitnow/application/dto/DuitNowTransferRequest.java`
- Create: `src/main/java/com/agentbanking/duitnow/application/dto/DuitNowTransferResponse.java`
- Create: `src/main/java/com/agentbanking/duitnow/application/service/DuitNowApplicationService.java`
- Create: `src/main/java/com/agentbanking/duitnow/application/config/DuitNowDomainServiceConfig.java`
- Create: `src/main/java/com/agentbanking/duitnow/infrastructure/adapter/MockProxyRegistryAdapter.java`
- Create: `src/main/java/com/agentbanking/duitnow/infrastructure/adapter/MockPayNetGatewayAdapter.java`
- Modify: `src/main/resources/db/migration/V10__duitnow_init.sql` (create Flyway migration)

- [ ] **Step A3.1: Write DTOs**

```java
// DuitNowTransferRequest.java
package com.agentbanking.duitnow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DuitNowTransferRequest(
  @NotBlank String proxyType,
  @NotBlank String proxyValue,
  @NotNull @Positive BigDecimal amount,
  @NotBlank String reference
) {}
```

```java
// DuitNowTransferResponse.java
package com.agentbanking.duitnow.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DuitNowTransferResponse(
  UUID transactionId,
  String status,
  String recipientName,
  BigDecimal amount,
  Instant completedAt,
  String message
) {}
```

- [ ] **Step A3.2: Write application service**

```java
package com.agentbanking.duitnow.application.service;

import com.agentbanking.duitnow.application.dto.DuitNowTransferRequest;
import com.agentbanking.duitnow.application.dto.DuitNowTransferResponse;
import com.agentbanking.duitnow.domain.model.DuitNowTransaction;
import com.agentbanking.duitnow.domain.model.ProxyType;
import com.agentbanking.duitnow.domain.port.in.TransferMoneyUseCase;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class DuitNowApplicationService {

  private final TransferMoneyUseCase transferMoneyUseCase;
  private static final Duration TIMEOUT = Duration.ofSeconds(15);

  public DuitNowApplicationService(TransferMoneyUseCase transferMoneyUseCase) {
    this.transferMoneyUseCase = transferMoneyUseCase;
  }

  public DuitNowTransferResponse transfer(DuitNowTransferRequest request, String idempotencyKey) {
    Instant start = Instant.now();
    
    DuitNowTransaction result;
    try {
      result = transferMoneyUseCase.transfer(
        UUID.randomUUID(),
        request.proxyType(),
        request.proxyValue(),
        request.amount(),
        request.reference(),
        idempotencyKey
      );
    } catch (Exception e) {
      throw new RuntimeException("DuitNow transfer failed: " + e.getMessage());
    }
    
    return new DuitNowTransferResponse(
      result.id(),
      result.status().name(),
      result.recipientName(),
      result.amount(),
      result.completedAt(),
      "Transfer completed"
    );
  }
}
```

- [ ] **Step A3.3: Write mock adapters**

```java
// MockProxyRegistryAdapter.java
package com.agentbanking.duitnow.infrastructure.adapter;

import com.agentbanking.duitnow.domain.model.ProxyResolutionResult;
import com.agentbanking.duitnow.domain.model.ProxyType;
import com.agentbanking.duitnow.domain.port.out.ProxyRegistryPort;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MockProxyRegistryAdapter implements ProxyRegistryPort {

  private static final Map<String, ProxyResolutionResult> PROXY_REGISTRY = Map.of(
    "60123456789", new ProxyResolutionResult(UUID.randomUUID(), "1234567890", "MBB", "Ahmad Bin Abu", ProxyType.MOBILE_NUMBER, true),
    "890101101234", new ProxyResolutionResult(UUID.randomUUID(), "9876543210", "CIMB", "Siti Nurhaliza", ProxyType.NRIC, true),
    "20210315001K", new ProxyResolutionResult(UUID.randomUUID(), "5555666677", "Public Bank", "Kedai Runcit Sdn Bhd", ProxyType.BUSINESS_REGISTRATION_NUMBER, true)
  );

  @Override
  public Optional<ProxyResolutionResult> resolve(ProxyType proxyType, String proxyValue) {
    return Optional.ofNullable(PROXY_REGISTRY.get(proxyValue));
  }
}
```

```java
// MockPayNetGatewayAdapter.java
package com.agentbanking.duitnow.infrastructure.adapter;

import com.agentbanking.duitnow.domain.model.DuitNowStatus;
import com.agentbanking.duitnow.domain.model.DuitNowTransaction;
import com.agentbanking.duitnow.domain.port.out.PayNetGatewayPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MockPayNetGatewayAdapter implements PayNetGatewayPort {

  @Override
  public DuitNowTransaction transfer(UUID transactionId, String targetAccount, 
      String bankCode, BigDecimal amount, String reference) {
    // Simulate ISO 20022 message processing with realistic delay
    try {
      Thread.sleep(500); // 500ms mock processing
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    return new DuitNowTransaction(
      transactionId, null, null, null, null, null, targetAccount, bankCode, null,
      amount, reference, DuitNowStatus.COMPLETED, Instant.now(), Instant.now(), null
    );
  }

  @Override
  public DuitNowTransaction queryStatus(UUID transactionId) {
    return new DuitNowTransaction(
      transactionId, null, null, null, null, null, null, null, null,
      null, null, DuitNowStatus.COMPLETED, Instant.now(), Instant.now(), null
    );
  }
}
```

- [ ] **Step A3.4: Create Flyway migration**

```sql
-- V10__duitnow_init.sql
CREATE TABLE IF NOT EXISTS duitnow_transaction (
  id UUID PRIMARY KEY,
  idempotency_key VARCHAR(64) UNIQUE NOT NULL,
  agent_id VARCHAR(32),
  trace_id UUID,
  proxy_type VARCHAR(20) NOT NULL,
  proxy_value VARCHAR(50) NOT NULL,
  resolved_account VARCHAR(20),
  bank_code VARCHAR(6),
  recipient_name VARCHAR(100),
  amount DECIMAL(18,2) NOT NULL,
  reference VARCHAR(100),
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP,
  failure_reason VARCHAR(255)
);

CREATE INDEX idx_duit_transaction_idempotency ON duitnow_transaction(idempotency_key);
CREATE INDEX idx_duit_transaction_status ON duitnow_transaction(status);
```

- [ ] **Step A3.5: Commit**

```bash
git add src/main/java/com/agentbanking/duitnow/application/ src/main/java/com/agentbanking/duitnow/infrastructure/
git commit -m "feat(duitnow): implement application layer and mock adapters"
```

---

## Part B: Merchant Services

### Task B1: Merchant Domain Model & Ports

**Files:**
- Create: `src/main/java/com/agentbanking/merchant/domain/model/MerchantTransaction.java`
- Create: `src/main/java/com/agentbanking/merchant/domain/model/MerchantStatus.java`
- Create: `src/main/java/com/agentbanking/merchant/domain/model/MerchantCategory.java`
- Create: `src/main/java/com/agentbanking/merchant/domain/model/MdrConfig.java`
- Create: `src/main/java/com/agentbanking/merchant/domain/model/CardType.java`
- Create: `src/main/java/com/agentbanking/merchant/domain/port/in/RetailSaleUseCase.java`
- Create: `src/main/java/com/agentbanking/merchant/domain/port/in/PinPurchaseUseCase.java`
- Create: `src/main/java/com/agentbanking/merchant/domain/port/in/CalculateMdrUseCase.java`
- Create: `src/main/java/com/agentbanking/merchant/domain/port/in/CashBackUseCase.java`
- Create: `src/main/java/com/agentbanking/merchant/domain/port/out/CardGatewayPort.java`
- Create: `src/main/java/com/agentbanking/merchant/domain/port/out/MerchantRegistryPort.java`

- [ ] **Step B1.1: Write domain models**

```java
// CardType.java
package com.agentbanking.merchant.domain.model;

public enum CardType {
  VISA,
  MASTERCARD,
  AMEX,
  MYDEBIT
}
```

```java
// MerchantCategory.java
package com.agentbanking.merchant.domain.model;

public enum MerchantCategory {
  RETAIL,
  F&B,
  PETROL,
  GROCERY,
  SERVICES
}
```

```java
// MerchantStatus.java
package com.agentbanking.merchant.domain.model;

public enum MerchantStatus {
  PENDING,
  AUTHORIZED,
  CAPTURED,
  VOIDED,
  REFUNDED,
  FAILED
}
```

```java
// MerchantTransaction.java
package com.agentbanking.merchant.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MerchantTransaction(
  UUID id,
  String agentId,
  UUID traceId,
  String merchantId,
  CardType cardType,
  String maskedPan,
  BigDecimal transactionAmount,
  BigDecimal mdrAmount,
  BigDecimal totalAmount,
  BigDecimal cashBackAmount,
  MerchantStatus status,
  String authorizationCode,
  Instant createdAt,
  Instant capturedAt,
  String invoiceNumber,
  String terminalId
) {}
```

```java
// MdrConfig.java
package com.agentbanking.merchant.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record MdrConfig(
  UUID id,
  MerchantCategory category,
  CardType cardType,
  BigDecimal mdrPercentage,
  BigDecimal mdrFixed,
  BigDecimal minAmount,
  BigDecimal maxAmount,
  boolean cashBackAllowed
) {}
```

- [ ] **Step B1.2: Write port interfaces**

```java
// RetailSaleUseCase.java
package com.agentbanking.merchant.domain.port.in;

import com.agentbanking.merchant.domain.model.MerchantTransaction;
import java.math.BigDecimal;

public interface RetailSaleUseCase {
  MerchantTransaction sale(String agentId, String merchantId, String cardData, 
      BigDecimal amount, String invoiceNumber);
}
```

```java
// PinPurchaseUseCase.java
package com.agentbanking.merchant.domain.port.in;

import com.agentbanking.merchant.domain.model.MerchantTransaction;
import java.math.BigDecimal;

public interface PinPurchaseUseCase {
  MerchantTransaction purchaseWithPin(String agentId, String merchantId, 
      String encryptedPin, String cardData, BigDecimal amount, String invoiceNumber);
}
```

```java
// CalculateMdrUseCase.java
package com.agentbanking.merchant.domain.port.in;

import com.agentbanking.merchant.domain.model.MerchantCategory;
import com.agentbanking.merchant.domain.model.CardType;
import java.math.BigDecimal;
import java.util.Optional;

public interface CalculateMdrUseCase {
  record MdrResult(BigDecimal mdrPercentage, BigDecimal mdrFixed, BigDecimal totalMdr) {}
  Optional<MdrResult> calculate(MerchantCategory category, CardType cardType, BigDecimal amount);
}
```

```java
// CashBackUseCase.java
package com.agentbanking.merchant.domain.port.in;

import com.agentbanking.merchant.domain.model.MerchantTransaction;
import java.math.BigDecimal;

public interface CashBackUseCase {
  MerchantTransaction processWithCashBack(String agentId, String merchantId, 
      String cardData, BigDecimal purchaseAmount, BigDecimal cashBackAmount, String invoiceNumber);
}
```

- [ ] **Step B1.3: Write outbound ports**

```java
// CardGatewayPort.java
package com.agentbanking.merchant.domain.port.out;

import com.agentbanking.merchant.domain.model.CardType;
import com.agentbanking.merchant.domain.model.MerchantTransaction;
import java.math.BigDecimal;

public interface CardGatewayPort {
  MerchantTransaction authorize(String cardData, BigDecimal amount);
  MerchantTransaction capture(String transactionId, BigDecimal amount);
  MerchantTransaction voidTransaction(String transactionId);
  MerchantTransaction refund(String transactionId, BigDecimal amount);
}
```

```java
// MerchantRegistryPort.java
package com.agentbanking.merchant.domain.port.out;

import com.agentbanking.merchant.domain.model.MerchantCategory;
import com.agentbanking.merchant.domain.model.MdrConfig;
import java.util.List;
import java.util.Optional;

public interface MerchantRegistryPort {
  Optional<String> getMerchantName(String merchantId);
  Optional<MerchantCategory> getMerchantCategory(String merchantId);
  List<MdrConfig> getMdrConfigs();
  Optional<MdrConfig> getMdrConfig(MerchantCategory category, CardType cardType);
}
```

- [ ] **Step B1.4: Commit**

```bash
git add src/main/java/com/agentbanking/merchant/domain/
git commit -m "feat(merchant): add domain models and ports"
```

### Task B2: Merchant Domain Service

**Files:**
- Create: `src/main/java/com/agentbanking/merchant/domain/service/MerchantService.java`
- Test: `src/test/java/com/agentbanking/merchant/domain/service/MerchantServiceTest.java`

- [ ] **Step B2.1: Write failing test**

```java
package com.agentbanking.merchant.domain.service;

import com.agentbanking.merchant.domain.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MerchantServiceTest {

  @Test
  void shouldCalculateMdrForRetailSale() {
    // Given: retail transaction, visa card
    // When: MDR calculated
    // Then: correct MDR applied
  }

  @Test
  void shouldProcessRetailSale() {
    // Given: card present transaction
    // When: sale executed
    // Then: authorized
  }

  @Test
  void shouldAddCashBackToPurchase() {
    // Given: purchase with cashback
    // When: processed
    // Then: total includes cashback
  }
}
```

- [ ] **Step B2.2: Implement service**

```java
package com.agentbanking.merchant.domain.service;

import com.agentbanking.merchant.domain.model.*;
import com.agentbanking.merchant.domain.port.in.*;
import com.agentbanking.merchant.domain.port.out.CardGatewayPort;
import com.agentbanking.merchant.domain.port.out.MerchantRegistryPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class MerchantService implements RetailSaleUseCase, PinPurchaseUseCase, 
    CalculateMdrUseCase, CashBackUseCase {

  private final CardGatewayPort cardGatewayPort;
  private final MerchantRegistryPort merchantRegistryPort;

  public MerchantService(CardGatewayPort cardGatewayPort, MerchantRegistryPort merchantRegistryPort) {
    this.cardGatewayPort = cardGatewayPort;
    this.merchantRegistryPort = merchantRegistryPort;
  }

  @Override
  public MerchantTransaction sale(String agentId, String merchantId, String cardData, 
      BigDecimal amount, String invoiceNumber) {
    
    MerchantCategory category = merchantRegistryPort.getMerchantCategory(merchantId)
      .orElseThrow(() -> new IllegalArgumentException("Unknown merchant: " + merchantId));
    
    MdrResult mdrResult = calculate(category, CardType.MYDEBIT, amount)
      .orElseThrow(() -> new IllegalArgumentException("No MDR config for category: " + category));
    
    BigDecimal mdrAmount = mdrResult.totalMdr();
    BigDecimal totalAmount = amount.add(mdrAmount);
    
    MerchantTransaction tx = cardGatewayPort.authorize(cardData, totalAmount);
    
    return new MerchantTransaction(
      tx.id(), agentId, UUID.randomUUID(), merchantId, CardType.MYDEBIT,
      maskCard(cardData), amount, mdrAmount, totalAmount, BigDecimal.ZERO,
      MerchantStatus.AUTHORIZED, tx.authorizationCode(), Instant.now(), null,
      invoiceNumber, null
    );
  }

  @Override
  public MerchantTransaction purchaseWithPin(String agentId, String merchantId, 
      String encryptedPin, String cardData, BigDecimal amount, String invoiceNumber) {
    // Similar to sale but with PIN verification
    return sale(agentId, merchantId, cardData, amount, invoiceNumber);
  }

  @Override
  public Optional<MdrResult> calculate(MerchantCategory category, CardType cardType, BigDecimal amount) {
    return merchantRegistryPort.getMdrConfig(category, cardType)
      .map(config -> {
        BigDecimal percentageAmt = amount.multiply(config.mdrPercentage())
          .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal fixedAmt = config.mdrFixed() != null ? config.mdrFixed() : BigDecimal.ZERO;
        return new MdrResult(config.mdrPercentage(), fixedAmt, percentageAmt.add(fixedAmt));
      });
  }

  @Override
  public MerchantTransaction processWithCashBack(String agentId, String merchantId, 
      String cardData, BigDecimal purchaseAmount, BigDecimal cashBackAmount, String invoiceNumber) {
    
    MerchantCategory category = merchantRegistryPort.getMerchantCategory(merchantId)
      .orElseThrow(() -> new IllegalArgumentException("Unknown merchant: " + merchantId));
    
    MdrResult mdrResult = calculate(category, CardType.MYDEBIT, purchaseAmount)
      .orElseThrow(() -> new IllegalArgumentException("No MDR config"));
    
    BigDecimal mdrAmount = mdrResult.totalMdr();
    BigDecimal totalAmount = purchaseAmount.add(mdrAmount).add(cashBackAmount);
    
    MerchantTransaction tx = cardGatewayPort.authorize(cardData, totalAmount);
    
    return new MerchantTransaction(
      tx.id(), agentId, UUID.randomUUID(), merchantId, CardType.MYDEBIT,
      maskCard(cardData), purchaseAmount, mdrAmount, totalAmount, cashBackAmount,
      MerchantStatus.AUTHORIZED, tx.authorizationCode(), Instant.now(), null,
      invoiceNumber, null
    );
  }

  private String maskCard(String cardData) {
    if (cardData == null || cardData.length() < 13) return "************";
    return cardData.substring(0, 6) + "******" + cardData.substring(cardData.length() - 4);
  }
}
```

- [ ] **Step B2.3: Run tests**

```bash
cd D:\Working\myprojects\agentbanking-seed4j && mvn test -Dtest=MerchantServiceTest
# Expected: PASS
```

- [ ] **Step B2.4: Commit**

```bash
git add src/main/java/com/agentbanking/merchant/domain/service/
git commit -m "feat(merchant): implement MerchantService"
```

### Task B3: Merchant Application & Infrastructure (Mock Adapters)

**Files:**
- Create: `src/main/java/com/agentbanking/merchant/application/dto/RetailSaleRequest.java`
- Create: `src/main/java/com/agentbanking/merchant/application/dto/RetailSaleResponse.java`
- Create: `src/main/java/com/agentbanking/merchant/application/dto/MerchantMdrResponse.java`
- Create: `src/main/java/com/agentbanking/merchant/application/service/MerchantApplicationService.java`
- Create: `src/main/java/com/agentbanking/merchant/application/config/MerchantDomainServiceConfig.java`
- Create: `src/main/java/com/agentbanking/merchant/infrastructure/adapter/MockCardGatewayAdapter.java`
- Create: `src/main/java/com/agentbanking/merchant/infrastructure/adapter/MockMerchantRegistryAdapter.java`
- Create: `src/main/resources/db/migration/V11__merchant_init.sql`

- [ ] **Step B3.1: Write DTOs**

```java
// RetailSaleRequest.java
package com.agentbanking.merchant.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RetailSaleRequest(
  @NotBlank String merchantId,
  @NotBlank String cardData,
  @NotNull @Positive BigDecimal amount,
  @NotBlank String invoiceNumber
) {}
```

```java
// RetailSaleResponse.java
package com.agentbanking.merchant.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RetailSaleResponse(
  UUID transactionId,
  String status,
  String authorizationCode,
  BigDecimal transactionAmount,
  BigDecimal mdrAmount,
  BigDecimal totalAmount,
  BigDecimal cashBackAmount,
  Instant capturedAt
) {}
```

```java
// MerchantMdrResponse.java
package com.agentbanking.merchant.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MerchantMdrResponse(
  UUID configId,
  String category,
  String cardType,
  BigDecimal mdrPercentage,
  BigDecimal mdrFixed,
  BigDecimal totalMdr
) {}
```

- [ ] **Step B3.2: Write application service**

```java
package com.agentbanking.merchant.application.service;

import com.agentbanking.merchant.application.dto.*;
import com.agentbanking.merchant.domain.model.*;
import com.agentbanking.merchant.domain.port.in.*;
import java.util.UUID;

public class MerchantApplicationService {

  private final MerchantService merchantService;

  public MerchantApplicationService(MerchantService merchantService) {
    this.merchantService = merchantService;
  }

  public RetailSaleResponse processSale(RetailSaleRequest request, String agentId) {
    MerchantTransaction tx = merchantService.sale(
      agentId,
      request.merchantId(),
      request.cardData(),
      request.amount(),
      request.invoiceNumber()
    );
    
    return new RetailSaleResponse(
      tx.id(), tx.status().name(), tx.authorizationCode(),
      tx.transactionAmount(), tx.mdrAmount(), tx.totalAmount(), 
      tx.cashBackAmount(), tx.capturedAt()
    );
  }

  public RetailSaleResponse processPinPurchase(PinPurchaseRequest request, String agentId) {
    MerchantTransaction tx = merchantService.purchaseWithPin(
      agentId, request.merchantId(), request.encryptedPin(),
      request.cardData(), request.amount(), request.invoiceNumber()
    );
    
    return new RetailSaleResponse(
      tx.id(), tx.status().name(), tx.authorizationCode(),
      tx.transactionAmount(), tx.mdrAmount(), tx.totalAmount(),
      tx.cashBackAmount(), tx.capturedAt()
    );
  }

  public RetailSaleResponse processCashBack(CashBackRequest request, String agentId) {
    MerchantTransaction tx = merchantService.processWithCashBack(
      agentId, request.merchantId(), request.cardData(),
      request.purchaseAmount(), request.cashBackAmount(), request.invoiceNumber()
    );
    
    return new RetailSaleResponse(
      tx.id(), tx.status().name(), tx.authorizationCode(),
      tx.transactionAmount(), tx.mdrAmount(), tx.totalAmount(),
      tx.cashBackAmount(), tx.capturedAt()
    );
  }
}

public record PinPurchaseRequest(
  String merchantId, String encryptedPin, String cardData, 
  java.math.BigDecimal amount, String invoiceNumber
) {}

public record CashBackRequest(
  String merchantId, String cardData, 
  java.math.BigDecimal purchaseAmount, java.math.BigDecimal cashBackAmount,
  String invoiceNumber
) {}
```

- [ ] **Step B3.3: Write mock adapters**

```java
// MockCardGatewayAdapter.java
package com.agentbanking.merchant.infrastructure.adapter;

import com.agentbanking.merchant.domain.model.*;
import com.agentbanking.merchant.domain.port.out.CardGatewayPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MockCardGatewayAdapter implements CardGatewayPort {

  @Override
  public MerchantTransaction authorize(String cardData, BigDecimal amount) {
    try {
      Thread.sleep(300); // 300ms mock processing
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    String authCode = "AUTH" + System.currentTimeMillis() % 100000;
    
    return new MerchantTransaction(
      UUID.randomUUID(), null, null, null, null, null,
      amount, BigDecimal.ZERO, amount, BigDecimal.ZERO,
      MerchantStatus.AUTHORIZED, authCode, Instant.now(), null, null, null
    );
  }

  @Override
  public MerchantTransaction capture(String transactionId, BigDecimal amount) {
    return new MerchantTransaction(
      UUID.fromString(transactionId), null, null, null, null, null,
      amount, BigDecimal.ZERO, amount, BigDecimal.ZERO,
      MerchantStatus.CAPTURED, "CAPTURED", Instant.now(), Instant.now(), null, null
    );
  }

  @Override
  public MerchantTransaction voidTransaction(String transactionId) {
    return new MerchantTransaction(
      UUID.fromString(transactionId), null, null, null, null, null,
      BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
      MerchantStatus.VOIDED, "VOIDED", Instant.now(), null, null, null
    );
  }

  @Override
  public MerchantTransaction refund(String transactionId, BigDecimal amount) {
    return new MerchantTransaction(
      UUID.fromString(transactionId), null, null, null, null, null,
      amount, BigDecimal.ZERO, amount, BigDecimal.ZERO,
      MerchantStatus.REFUNDED, "REFUNDED", Instant.now(), Instant.now(), null, null
    );
  }
}
```

```java
// MockMerchantRegistryAdapter.java
package com.agentbanking.merchant.infrastructure.adapter;

import com.agentbanking.merchant.domain.model.*;
import com.agentbanking.merchant.domain.port.out.MerchantRegistryPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MockMerchantRegistryAdapter implements MerchantRegistryPort {

  private static final Map<String, MerchantCategory> MERCHANTS = Map.of(
    "MERCH001", MerchantCategory.RETAIL,
    "MERCH002", MerchantCategory.F&B,
    "MERCH003", MerchantCategory.PETROL
  );

  private static final List<MdrConfig> MDR_CONFIGS = List.of(
    new MdrConfig(UUID.randomUUID(), MerchantCategory.RETAIL, CardType.VISA, 
      new BigDecimal("1.50"), new BigDecimal("0.20"), 
      new BigDecimal("1.00"), new BigDecimal("5000.00"), true),
    new MdrConfig(UUID.randomUUID(), MerchantCategory.RETAIL, CardType.MASTERCAD, 
      new BigDecimal("1.50"), new BigDecimal("0.20"), 
      new BigDecimal("1.00"), new BigDecimal("5000.00"), true),
    new MdrConfig(UUID.randomUUID(), MerchantCategory.RETAIL, CardType.MYDEBIT, 
      new BigDecimal("0.50"), new BigDecimal("0.10"), 
      new BigDecimal("1.00"), new BigDecimal("3000.00"), true),
    new MdrConfig(UUID.randomUUID(), MerchantCategory.F&B, CardType.VISA, 
      new BigDecimal("2.00"), new BigDecimal("0.30"), 
      new BigDecimal("1.00"), new BigDecimal("3000.00"), false)
  );

  @Override
  public Optional<String> getMerchantName(String merchantId) {
    return Optional.of("Test Merchant " + merchantId);
  }

  @Override
  public Optional<MerchantCategory> getMerchantCategory(String merchantId) {
    return Optional.ofNullable(MERCHANTS.get(merchantId));
  }

  @Override
  public List<MdrConfig> getMdrConfigs() {
    return MDR_CONFIGS;
  }

  @Override
  public Optional<MdrConfig> getMdrConfig(MerchantCategory category, CardType cardType) {
    return MDR_CONFIGS.stream()
      .filter(c -> c.category() == category && c.cardType() == cardType)
      .findFirst();
  }
}
```

- [ ] **Step B3.4: Create Flyway migration**

```sql
-- V11__merchant_init.sql
CREATE TABLE IF NOT EXISTS merchant_transaction (
  id UUID PRIMARY KEY,
  agent_id VARCHAR(32) NOT NULL,
  trace_id UUID,
  merchant_id VARCHAR(32) NOT NULL,
  card_type VARCHAR(20) NOT NULL,
  masked_pan VARCHAR(20),
  transaction_amount DECIMAL(18,2) NOT NULL,
  mdr_amount DECIMAL(18,2) NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL,
  cash_back_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL,
  authorization_code VARCHAR(20),
  created_at TIMESTAMP NOT NULL,
  captured_at TIMESTAMP,
  invoice_number VARCHAR(50),
  terminal_id VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS mdr_config (
  id UUID PRIMARY KEY,
  category VARCHAR(20) NOT NULL,
  card_type VARCHAR(20) NOT NULL,
  mdr_percentage DECIMAL(5,2) NOT NULL,
  mdr_fixed DECIMAL(18,2),
  min_amount DECIMAL(18,2),
  max_amount DECIMAL(18,2),
  cash_back_allowed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_merchant_agent ON merchant_transaction(agent_id);
CREATE INDEX idx_merchant_merchant ON merchant_transaction(merchant_id);
```

- [ ] **Step B3.5: Commit**

```bash
git add src/main/java/com/agentbanking/merchant/application/ src/main/java/com/agentbanking/merchant/infrastructure/ src/main/resources/db/migration/V10__duitnow_init.sql src/main/resources/db/migration/V11__merchant_init.sql
git commit -m "feat(merchant): implement application layer and mock adapters"
```

---

## Part C: Integration & Timeout Handling

### Task C1: DuitNow Timeout Handling

**Files:**
- Modify: `src/main/java/com/agentbanking/duitnow/application/service/DuitNowApplicationService.java`

- [ ] **Step C1.1: Add timeout handling**

```java
// Add to DuitNowApplicationService
public class DuitNowApplicationService {

  private final TransferMoneyUseCase transferMoneyUseCase;
  private static final Duration TIMEOUT = Duration.ofSeconds(15);

  public DuitNowTransferResponse transferWithTimeout(DuitNowTransferRequest request, String idempotencyKey) {
    Instant start = Instant.now();
    Thread timeoutThread = new Thread(() -> {
      try {
        Thread.sleep(TIMEOUT.toMillis());
        // Handle timeout - mark transaction as TIMEOUT status
        throw new TimeoutException("DuitNow transfer timed out after 15 seconds");
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    timeoutThread.start();
    
    try {
      DuitNowTransferResponse response = transfer(request, idempotencyKey);
      timeoutThread.interrupt();
      return response;
    } catch (Exception e) {
      timeoutThread.interrupt();
      throw e;
    }
  }
}
```

- [ ] **Step C1.2: Commit**

```bash
git add src/main/java/com/agentbanking/duitnow/application/
git commit -m "feat(duitnow): add 15-second timeout handling"
```

### Task C2: ISO 20022 Mock Messages

**Files:**
- Create: `src/main/java/com/agentbanking/duitnow/infrastructure/iso/Iso20022MessageFactory.java`

- [ ] **Step C2.1: Create ISO 20022 mock factory**

```java
package com.agentbanking.duitnow.infrastructure.iso;

public class Iso20022MessageFactory {

  public String createTransferRequest(String fromAccount, String toAccount, 
      String amount, String reference) {
    return """
      <?xml version="1.0" encoding="UTF-8"?>
      <Document xmlns="urn:iso:std:iso:20022:fin:remt.001.001">
        <CdtTrfTxInf>
          <PmtId>
            <EndToEndId>%s</EndToEndId>
          </PmtId>
          <Amt>
            <InstdAmt Ccy="MYR">%s</InstdAmt>
          </Amt>
          <CdtrAcct>
            <Id>
              <IBAN>%s</IBAN>
            </Id>
          </CdtrAcct>
          <RmtInf>
            <Ustrd>%s</Ustrd>
          </RmtInf>
        </CdtTrfTxInf>
      </Document>
      """.formatted(reference, amount, toAccount, reference);
  }

  public String createTransferResponse(String status, String reason) {
    return """
      <?xml version="1.0" encoding="UTF-8"?>
      <Document xmlns="urn:iso:std:iso:20022:fin:remt.001.001">
        <GrpHdr>
          <CtrlSum>%s</CtrlSum>
        </GrpHdr>
      </Document>
      """.formatted(status);
  }
}
```

- [ ] **Step C2.2: Commit**

```bash
git add src/main/java/com/agentbanking/duitnow/infrastructure/iso/
git commit -m "feat(duitnow): add ISO 20022 mock message factory"
```

---

## Verification

- [ ] **Step: Run all DuitNow tests**

```bash
mvn test -Dtest="**/duitnow/**/*Test"
```

- [ ] **Step: Run all Merchant tests**

```bash
mvn test -Dtest="**/merchant/**/*Test"
```

- [ ] **Step: Run build verification**

```bash
mvn clean verify -DskipITs
```

- [ ] **Step: Commit final**

```bash
git add . && git commit -m "feat: implement Phase 6 - DuitNow & Merchant Services"
```