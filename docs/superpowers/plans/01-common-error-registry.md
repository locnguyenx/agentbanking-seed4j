# 01. Common Error Registry

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement centralized error code registry (60+ codes), exception hierarchy, global error schema, and exception handler for the Agent Banking Platform.

**Architecture:** Hexagonal — domain/ holds pure Java enums/exceptions (ZERO Spring imports), infrastructure/primary holds `@RestControllerAdvice`. Shared kernel via `@SharedKernel` annotation.

**Tech Stack:** Java 25, Spring Boot 4, JUnit 5, AssertJ, Gradle

**References:**
- BRD: `docs/superpowers/specs/agent-banking-platform-brd.md`
- BDD: `docs/superpowers/specs/agent-banking-platform-bdd.md` (Section 20: Error Code Reference)
- Design: `docs/superpowers/specs/agent-banking-platform-design.md` (Section 11: Error Handling)

**Existing code to preserve:**
- `shared/error/domain/Assert.java` — fluent assertion utilities (Seed4J-generated, DO NOT modify)
- `shared/error/domain/AssertionException.java` — base for assertion errors (Seed4J-generated, DO NOT modify)
- `shared/error/infrastructure/primary/BeanValidationErrorsHandler.java` — JSR-380 handler (Seed4J-generated, DO NOT modify)
- `shared/error/package-info.java` — `@SharedKernel` annotation (already present)

---

## Task 1: Seed4J Scaffolding

Seed4J has already scaffolded the `shared/error/` package with `@SharedKernel` annotation. Verify the structure exists:

```bash
ls src/main/java/com/agentbanking/shared/error/domain/
# Should see: Assert.java, AssertionException.java, AssertionErrorType.java,
#   MissingMandatoryValueException.java, StringTooLongException.java, etc.
```

**Seed4J already provides (DO NOT write manually):**
- Package structure: `shared/error/domain/`, `shared/error/infrastructure/primary/`
- `package-info.java` with `@SharedKernel`
- `Assert.java` fluent assertion utilities
- `AssertionException.java` base class
- `BeanValidationErrorsHandler.java` for JSR-380 validation errors
- Test template with `@UnitTest` annotation

**No CLI commands needed** — scaffolding is already in place.

- [ ] Verify existing files are present and unmodified

---

## Task 2: Error Code Enum (MANUAL — custom business logic)

### Test First

Create: `src/test/java/com/agentbanking/shared/error/domain/ErrorCodeTest.java`

```java
package com.agentbanking.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
@DisplayName("ErrorCode")
class ErrorCodeTest {

  @Nested
  class CategoryResolution {

    @Test
    void shouldResolveValidationCategory() {
      ErrorCode code = ErrorCode.ERR_VAL_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.VALIDATION);
    }

    @Test
    void shouldResolveBusinessCategory() {
      ErrorCode code = ErrorCode.ERR_BIZ_201;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.BUSINESS);
    }

    @Test
    void shouldResolveExternalCategory() {
      ErrorCode code = ErrorCode.ERR_EXT_101;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.EXTERNAL);
    }

    @Test
    void shouldResolveAuthenticationCategory() {
      ErrorCode code = ErrorCode.ERR_AUTH_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.AUTHENTICATION);
    }

    @Test
    void shouldResolveSystemCategory() {
      ErrorCode code = ErrorCode.ERR_SYS_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.SYSTEM);
    }

    @Test
    void shouldResolveIsoCategory() {
      ErrorCode code = ErrorCode.ERR_ISO_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.ISO_TRANSLATION);
    }

    @Test
    void shouldResolveHsmCategory() {
      ErrorCode code = ErrorCode.ERR_HSM_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.HSM_WRAPPER);
    }

    @Test
    void shouldResolveCbsCategory() {
      ErrorCode code = ErrorCode.ERR_CBS_001;
      assertThat(code.category()).isEqualTo(ErrorCodeCategory.CBS_CONNECTOR);
    }
  }

  @Nested
  class ActionCodeResolution {

    @Test
    void shouldReturnDeclineForValidationErrors() {
      assertThat(ErrorCode.ERR_VAL_001.actionCode()).isEqualTo("DECLINE");
    }

    @Test
    void shouldReturnDeclineForBusinessErrors() {
      assertThat(ErrorCode.ERR_BIZ_201.actionCode()).isEqualTo("DECLINE");
    }

    @Test
    void shouldReturnRetryForExternalErrors() {
      assertThat(ErrorCode.ERR_EXT_101.actionCode()).isEqualTo("RETRY");
    }

    @Test
    void shouldReturnDeclineForAuthErrors() {
      assertThat(ErrorCode.ERR_AUTH_001.actionCode()).isEqualTo("DECLINE");
    }

    @Test
    void shouldReturnReviewForSystemErrors() {
      assertThat(ErrorCode.ERR_SYS_001.actionCode()).isEqualTo("REVIEW");
    }
  }

  @Nested
  class FromCodeLookup {

    @Test
    void shouldFindCodeByValue() {
      assertThat(ErrorCode.fromCode("ERR_VAL_001")).isPresent().contains(ErrorCode.ERR_VAL_001);
    }

    @Test
    void shouldReturnEmptyForUnknownCode() {
      assertThat(ErrorCode.fromCode("UNKNOWN")).isEmpty();
    }
  }

  @Nested
  class HttpStatusMapping {

    @Test
    void shouldReturnBadRequestForValidation() {
      assertThat(ErrorCode.ERR_VAL_001.httpStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturnBadRequestForBusiness() {
      assertThat(ErrorCode.ERR_BIZ_201.httpStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturnGatewayTimeoutForExternalTimeout() {
      assertThat(ErrorCode.ERR_EXT_101.httpStatus()).isEqualTo(504);
    }

    @Test
    void shouldReturnBadGatewayForExternalError() {
      assertThat(ErrorCode.ERR_EXT_102.httpStatus()).isEqualTo(502);
    }

    @Test
    void shouldReturnUnauthorizedForAuth() {
      assertThat(ErrorCode.ERR_AUTH_001.httpStatus()).isEqualTo(401);
    }

    @Test
    void shouldReturnForbiddenForAccountLocked() {
      assertThat(ErrorCode.ERR_AUTH_004.httpStatus()).isEqualTo(429);
    }

    @Test
    void shouldReturnInternalServerErrorForSystem() {
      assertThat(ErrorCode.ERR_SYS_001.httpStatus()).isEqualTo(500);
    }
  }
}
```

### Implementation

Create: `src/main/java/com/agentbanking/shared/error/domain/ErrorCodeCategory.java`

```java
package com.agentbanking.shared.error.domain;

public enum ErrorCodeCategory {
  VALIDATION,
  BUSINESS,
  EXTERNAL,
  AUTHENTICATION,
  SYSTEM,
  ISO_TRANSLATION,
  CBS_CONNECTOR,
  HSM_WRAPPER,
  BILLER_GATEWAY,
  ERROR_MAPPING
}
```

Create: `src/main/java/com/agentbanking/shared/error/domain/ErrorCode.java`

```java
package com.agentbanking.shared.error.domain;

import java.util.Arrays;
import java.util.Optional;

public enum ErrorCode {

  // ERR_VAL_xxx - Validation Errors (14 codes)
  ERR_VAL_001("Invalid request format or missing required field", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_002("Invalid amount format (negative, zero, exceeds precision)", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_003("Invalid account identifier format", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_004("Invalid MyKad format (must be 12 digits)", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_005("Invalid MyKad data format", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_006("Invalid phone number format", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_007("Invalid DuitNow proxy format", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_008("Invalid PIN format or length", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_009("Amount exceeds daily limit for agent tier", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_010("Transaction count exceeds daily limit", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_011("PIN verification failed (3 attempts exceeded)", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_012("Fee configuration not found for transaction type/tier", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_013("Fee configuration expired or not yet effective", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_014("Fee components mismatch between config and request", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),

  // ERR_BIZ_xxx - Business Errors (30 codes)
  ERR_BIZ_201("Insufficient agent float balance", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_202("Insufficient customer balance", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_203("Daily transaction count limit exceeded", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_204("Daily amount limit exceeded", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_205("Agent float cap exceeded", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_206("Agent float account not found", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_207("Agent account is deactivated", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_208("Insufficient e-wallet balance", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_209("Geofence violation - transaction outside agent location", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_210("Self-approval prohibited (Four-Eyes Principle violation)", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_211("Reason code required for manual adjustment", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_212("Evidence attachment required for discrepancy resolution", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_213("Duplicate account detected", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_214("Invalid biller reference number", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_215("Invalid EPF member number", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_216("Biller service timeout", ErrorCodeCategory.BUSINESS, 400, "RETRY"),
  ERR_BIZ_217("Aggregator/aggregator timeout", ErrorCodeCategory.BUSINESS, 400, "RETRY"),
  ERR_BIZ_218("Smurfing/structuring pattern detected", ErrorCodeCategory.BUSINESS, 400, "REVIEW"),
  ERR_BIZ_219("Duplicate agent registration", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_220("Agent has pending transactions", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_221("Account not found for double-entry posting", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_222("PIN voucher inventory depleted", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_223("eSSP service unavailable", ErrorCodeCategory.BUSINESS, 400, "RETRY"),
  ERR_BIZ_301("Reversal window expired (>5 minutes)", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_302("Transaction already reversed", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_303("Settlement paused due to unresolved discrepancies", ErrorCodeCategory.BUSINESS, 400, "REVIEW"),
  ERR_BIZ_304("Amount mismatch in discrepancy resolution", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),

  // ERR_EXT_xxx - External Errors (22 codes)
  ERR_EXT_101("CBS connector timeout", ErrorCodeCategory.EXTERNAL, 504, "RETRY"),
  ERR_EXT_102("CBS connector error", ErrorCodeCategory.EXTERNAL, 502, "RETRY"),
  ERR_EXT_103("CBS settlement file generation failed", ErrorCodeCategory.EXTERNAL, 502, "REVIEW"),
  ERR_EXT_201("Biller gateway timeout", ErrorCodeCategory.EXTERNAL, 504, "RETRY"),
  ERR_EXT_202("Biller gateway error", ErrorCodeCategory.EXTERNAL, 502, "RETRY"),
  ERR_EXT_203("Biller API authentication failure", ErrorCodeCategory.EXTERNAL, 502, "DECLINE"),
  ERR_EXT_204("Biller idempotency conflict", ErrorCodeCategory.EXTERNAL, 409, "DECLINE"),
  ERR_EXT_301("JPN/KYC service unavailable", ErrorCodeCategory.EXTERNAL, 503, "RETRY"),
  ERR_EXT_302("Biometric scanner unavailable", ErrorCodeCategory.EXTERNAL, 503, "RETRY"),
  ERR_EXT_303("Biometric verification mismatch", ErrorCodeCategory.EXTERNAL, 400, "DECLINE"),
  ERR_EXT_401("PayNet ISO timeout", ErrorCodeCategory.EXTERNAL, 504, "RETRY"),
  ERR_EXT_402("PayNet ISO error", ErrorCodeCategory.EXTERNAL, 502, "RETRY"),
  ERR_EXT_403("ISO TCP socket connection lost", ErrorCodeCategory.EXTERNAL, 502, "RETRY"),
  ERR_EXT_404("STAN exhaustion (999999 reached)", ErrorCodeCategory.EXTERNAL, 500, "REVIEW"),

  // ERR_AUTH_xxx - Authentication Errors (10 codes)
  ERR_AUTH_001("Invalid OAuth2 token", ErrorCodeCategory.AUTHENTICATION, 401, "DECLINE"),
  ERR_AUTH_002("Missing authentication token", ErrorCodeCategory.AUTHENTICATION, 401, "DECLINE"),
  ERR_AUTH_003("Service unavailable for authentication", ErrorCodeCategory.AUTHENTICATION, 503, "RETRY"),
  ERR_AUTH_004("Rate limit exceeded", ErrorCodeCategory.AUTHENTICATION, 429, "DECLINE"),
  ERR_AUTH_101("Account/terminal sanctioned or blocked", ErrorCodeCategory.AUTHENTICATION, 403, "REVIEW"),
  ERR_AUTH_102("PIN locked after max attempts", ErrorCodeCategory.AUTHENTICATION, 403, "DECLINE"),
  ERR_AUTH_103("Token expired", ErrorCodeCategory.AUTHENTICATION, 401, "DECLINE"),

  // ERR_SYS_xxx - System Errors (8 codes)
  ERR_SYS_001("Internal server error", ErrorCodeCategory.SYSTEM, 500, "REVIEW"),
  ERR_SYS_002("GPS service unavailable", ErrorCodeCategory.SYSTEM, 503, "RETRY"),
  ERR_SYS_003("Temporal workflow state corruption", ErrorCodeCategory.SYSTEM, 500, "REVIEW"),
  ERR_SYS_004("Temporal workflow not found", ErrorCodeCategory.SYSTEM, 404, "REVIEW"),
  ERR_SYS_005("Temporal activity timeout misconfiguration", ErrorCodeCategory.SYSTEM, 500, "REVIEW"),

  // ERR_ISO_xxx - ISO Translation Errors (4 codes)
  ERR_ISO_001("ISO 8583 bitmap generation failed", ErrorCodeCategory.ISO_TRANSLATION, 500, "REVIEW"),
  ERR_ISO_002("ISO response unmarshal failed", ErrorCodeCategory.ISO_TRANSLATION, 500, "REVIEW"),
  ERR_ISO_003("STAN generation failed", ErrorCodeCategory.ISO_TRANSLATION, 500, "REVIEW"),
  ERR_ISO_004("PayNet heartbeat timeout", ErrorCodeCategory.ISO_TRANSLATION, 504, "RETRY"),

  // ERR_CBS_xxx - CBS Connector Errors (3 codes)
  ERR_CBS_001("SOAP/MQ request timeout", ErrorCodeCategory.CBS_CONNECTOR, 504, "RETRY"),
  ERR_CBS_002("CBS reply queue empty", ErrorCodeCategory.CBS_CONNECTOR, 504, "RETRY"),
  ERR_CBS_003("CBS flat-file format error", ErrorCodeCategory.CBS_CONNECTOR, 500, "REVIEW"),

  // ERR_HSM_xxx - HSM Wrapper Errors (4 codes)
  ERR_HSM_001("HSM PIN translation failed", ErrorCodeCategory.HSM_WRAPPER, 500, "RETRY"),
  ERR_HSM_002("HSM PIN verification failed", ErrorCodeCategory.HSM_WRAPPER, 500, "REVIEW"),
  ERR_HSM_003("HSM key vault access violation", ErrorCodeCategory.HSM_WRAPPER, 500, "STOP_ALERT"),
  ERR_HSM_004("HSM connection lost", ErrorCodeCategory.HSM_WRAPPER, 503, "RETRY"),

  // ERR_BG_xxx - Biller Gateway Errors (2 codes)
  ERR_BG_001("Biller API key injection failed", ErrorCodeCategory.BILLER_GATEWAY, 500, "REVIEW"),
  ERR_BG_002("Biller idempotency key conflict", ErrorCodeCategory.BILLER_GATEWAY, 409, "DECLINE"),

  // ERR_EM_xxx - Error Mapping Errors (3 codes)
  ERR_EM_001("Unknown legacy error code received", ErrorCodeCategory.ERROR_MAPPING, 500, "STOP_ALERT"),
  ERR_EM_002("Fallback mapping applied for unknown error", ErrorCodeCategory.ERROR_MAPPING, 500, "REVIEW"),
  ERR_EM_003("Error normalization pipeline failure", ErrorCodeCategory.ERROR_MAPPING, 500, "REVIEW");

  private final String message;
  private final ErrorCodeCategory category;
  private final int httpStatus;
  private final String actionCode;

  ErrorCode(String message, ErrorCodeCategory category, int httpStatus, String actionCode) {
    this.message = message;
    this.category = category;
    this.httpStatus = httpStatus;
    this.actionCode = actionCode;
  }

  public String message() {
    return message;
  }

  public ErrorCodeCategory category() {
    return category;
  }

  public int httpStatus() {
    return httpStatus;
  }

  public String actionCode() {
    return actionCode;
  }

  public static Optional<ErrorCode> fromCode(String code) {
    return Arrays.stream(values())
      .filter(c -> c.name().equals(code))
      .findFirst();
  }
}
```

- [ ] Test passes: `./gradlew test --tests "ErrorCodeTest"`
- [ ] Implementation created
- [ ] ZERO Spring/JPA imports in `domain/` files

---

## Task 3: Global Error Schema (MANUAL — custom business logic)

### Test First

Create: `src/test/java/com/agentbanking/shared/error/domain/GlobalErrorSchemaTest.java`

```java
package com.agentbanking.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
@DisplayName("GlobalErrorSchema")
class GlobalErrorSchemaTest {

  @Nested
  class Instantiation {

    @Test
    void shouldCreateWithAllFields() {
      Instant now = Instant.now();
      GlobalErrorSchema schema = new GlobalErrorSchema(
        "ERR_VAL_001",
        "Invalid request",
        "DECLINE",
        "trace-123",
        now
      );

      assertThat(schema.code()).isEqualTo("ERR_VAL_001");
      assertThat(schema.message()).isEqualTo("Invalid request");
      assertThat(schema.actionCode()).isEqualTo("DECLINE");
      assertThat(schema.traceId()).isEqualTo("trace-123");
      assertThat(schema.timestamp()).isEqualTo(now);
    }

    @Test
    void shouldCreateWithFactoryMethod() {
      GlobalErrorSchema schema = GlobalErrorSchema.of(
        "ERR_BIZ_201",
        "Insufficient float",
        "DECLINE"
      );

      assertThat(schema.code()).isEqualTo("ERR_BIZ_201");
      assertThat(schema.message()).isEqualTo("Insufficient float");
      assertThat(schema.actionCode()).isEqualTo("DECLINE");
      assertThat(schema.traceId()).isNotNull();
      assertThat(schema.timestamp()).isNotNull();
    }
  }

  @Nested
  class ActionCodeValidation {

    @Test
    void shouldAcceptValidActionCode_DECLINE() {
      GlobalErrorSchema schema = GlobalErrorSchema.of("ERR_VAL_001", "test", "DECLINE");
      assertThat(schema.actionCode()).isEqualTo("DECLINE");
    }

    @Test
    void shouldAcceptValidActionCode_RETRY() {
      GlobalErrorSchema schema = GlobalErrorSchema.of("ERR_EXT_101", "test", "RETRY");
      assertThat(schema.actionCode()).isEqualTo("RETRY");
    }

    @Test
    void shouldAcceptValidActionCode_REVIEW() {
      GlobalErrorSchema schema = GlobalErrorSchema.of("ERR_SYS_001", "test", "REVIEW");
      assertThat(schema.actionCode()).isEqualTo("REVIEW");
    }
  }
}
```

### Implementation

Create: `src/main/java/com/agentbanking/shared/error/domain/GlobalErrorSchema.java`

```java
package com.agentbanking.shared.error.domain;

import java.time.Instant;

public record GlobalErrorSchema(
  String code,
  String message,
  String actionCode,
  String traceId,
  Instant timestamp
) {

  public static GlobalErrorSchema of(String code, String message, String actionCode) {
    return new GlobalErrorSchema(
      code,
      message,
      actionCode,
      generateTraceId(),
      Instant.now()
    );
  }

  private static String generateTraceId() {
    return "trace-" + System.currentTimeMillis();
  }
}
```

- [ ] Test passes: `./gradlew test --tests "GlobalErrorSchemaTest"`
- [ ] Implementation created

---

## Task 4: Exception Hierarchy (MANUAL — custom business logic)

### Test First

Create: `src/test/java/com/agentbanking/shared/error/domain/ValidationExceptionTest.java`

```java
package com.agentbanking.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;

@UnitTest
@DisplayName("ValidationException")
class ValidationExceptionTest {

  @Nested
  class Instantiation {

    @Test
    void shouldCreateWithErrorCode() {
      ValidationException exception = new ValidationException(ErrorCode.ERR_VAL_001, "amount");

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_VAL_001);
      assertThat(exception.getMessage()).contains("amount");
    }

    @Test
    void shouldCreateWithMessage() {
      ValidationException exception = new ValidationException(
        ErrorCode.ERR_VAL_001,
        "Invalid request",
        "amount"
      );

      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ERR_VAL_001);
      assertThat(exception.getMessage()).isEqualTo("Invalid request");
      assertThat(exception.field()).isEqualTo("amount");
    }
  }

  @Nested
  class ExceptionHierarchy {

    @Test
    void shouldBeRuntimeException() {
      ValidationException exception = new ValidationException(ErrorCode.ERR_VAL_001, "field");

      assertThat(exception).isInstanceOf(RuntimeException.class);
      assertThat(exception).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldMapToCorrectHttpStatus() {
      ValidationException exception = new ValidationException(ErrorCode.ERR_VAL_001, "field");

      assertThat(exception.httpStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturnDeclineActionCode() {
      ValidationException exception = new ValidationException(ErrorCode.ERR_VAL_001, "field");

      assertThat(exception.actionCode()).isEqualTo("DECLINE");
    }
  }
}
```

### Implementation

Create: `src/main/java/com/agentbanking/shared/error/domain/BusinessException.java`

```java
package com.agentbanking.shared.error.domain;

public abstract class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  protected BusinessException(ErrorCode errorCode) {
    super(errorCode.message());
    this.errorCode = errorCode;
  }

  protected BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public int httpStatus() {
    return errorCode.httpStatus();
  }

  public String actionCode() {
    return errorCode.actionCode();
  }
}
```

Create: `src/main/java/com/agentbanking/shared/error/domain/ValidationException.java`

```java
package com.agentbanking.shared.error.domain;

public class ValidationException extends BusinessException {

  private final String field;

  public ValidationException(ErrorCode errorCode, String field) {
    super(errorCode);
    this.field = field;
  }

  public ValidationException(ErrorCode errorCode, String message, String field) {
    super(errorCode, message);
    this.field = field;
  }

  public String field() {
    return field;
  }
}
```

Create: `src/main/java/com/agentbanking/shared/error/domain/BusinessRuleException.java`

```java
package com.agentbanking.shared.error.domain;

public class BusinessRuleException extends BusinessException {

  public BusinessRuleException(ErrorCode errorCode) {
    super(errorCode);
  }

  public BusinessRuleException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
```

Create: `src/main/java/com/agentbanking/shared/error/domain/ExternalServiceException.java`

```java
package com.agentbanking.shared.error.domain;

public class ExternalServiceException extends BusinessException {

  private final String serviceName;

  public ExternalServiceException(ErrorCode errorCode, String serviceName) {
    super(errorCode);
    this.serviceName = serviceName;
  }

  public ExternalServiceException(ErrorCode errorCode, String message, String serviceName) {
    super(errorCode, message);
    this.serviceName = serviceName;
  }

  public String serviceName() {
    return serviceName;
  }
}
```

Create: `src/main/java/com/agentbanking/shared/error/domain/AuthenticationException.java`

```java
package com.agentbanking.shared.error.domain;

public class AuthenticationException extends BusinessException {

  public AuthenticationException(ErrorCode errorCode) {
    super(errorCode);
  }

  public AuthenticationException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
```

Create: `src/main/java/com/agentbanking/shared/error/domain/SystemException.java`

```java
package com.agentbanking.shared.error.domain;

public class SystemException extends BusinessException {

  public SystemException(ErrorCode errorCode) {
    super(errorCode);
  }

  public SystemException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
```

- [ ] Test passes: `./gradlew test --tests "ValidationExceptionTest"`
- [ ] All 5 exception classes created
- [ ] ZERO Spring imports in `domain/`

---

## Task 5: Global Exception Handler (MANUAL — custom business logic)

### Test First

Create: `src/test/java/com/agentbanking/shared/error/infrastructure/primary/GlobalExceptionHandlerTest.java`

```java
package com.agentbanking.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import com.agentbanking.UnitTest;
import com.agentbanking.shared.error.domain.*;

@UnitTest
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Nested
  class ValidationExceptionHandling {

    @Test
    void shouldHandleValidationException() {
      ValidationException ex = new ValidationException(ErrorCode.ERR_VAL_001, "amount");
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleValidationException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(400);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_VAL_001");
      assertThat(response.getBody().actionCode()).isEqualTo("DECLINE");
    }
  }

  @Nested
  class BusinessRuleExceptionHandling {

    @Test
    void shouldHandleBusinessRuleException() {
      BusinessRuleException ex = new BusinessRuleException(ErrorCode.ERR_BIZ_201);
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleBusinessRuleException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(400);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_BIZ_201");
    }
  }

  @Nested
  class ExternalServiceExceptionHandling {

    @Test
    void shouldHandleExternalServiceException() {
      ExternalServiceException ex = new ExternalServiceException(ErrorCode.ERR_EXT_101, "CBS");
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleExternalServiceException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(504);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_EXT_101");
      assertThat(response.getBody().actionCode()).isEqualTo("RETRY");
    }
  }

  @Nested
  class AuthenticationExceptionHandling {

    @Test
    void shouldHandleAuthenticationException() {
      AuthenticationException ex = new AuthenticationException(ErrorCode.ERR_AUTH_001);
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleAuthenticationException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(401);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_AUTH_001");
    }
  }

  @Nested
  class SystemExceptionHandling {

    @Test
    void shouldHandleSystemException() {
      SystemException ex = new SystemException(ErrorCode.ERR_SYS_001);
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleSystemException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(500);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_SYS_001");
      assertThat(response.getBody().actionCode()).isEqualTo("REVIEW");
    }
  }

  @Nested
  class GenericExceptionHandling {

    @Test
    void shouldHandleGenericException() {
      RuntimeException ex = new RuntimeException("Unexpected error");
      WebRequest request = mock(WebRequest.class);

      ResponseEntity<GlobalErrorSchema> response = handler.handleGenericException(ex, request);

      assertThat(response.getStatusCode().value()).isEqualTo(500);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().code()).isEqualTo("ERR_SYS_001");
    }
  }
}
```

### Implementation

Create: `src/main/java/com/agentbanking/shared/error/infrastructure/primary/GlobalExceptionHandler.java`

```java
package com.agentbanking.shared.error.infrastructure.primary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import com.agentbanking.shared.error.domain.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<GlobalErrorSchema> handleValidationException(
    ValidationException ex,
    WebRequest request
  ) {
    log.warn("Validation error: {} - {}", ex.getErrorCode(), ex.getMessage());

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ex.getErrorCode().name(),
      ex.getMessage(),
      ex.actionCode()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(schema);
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<GlobalErrorSchema> handleBusinessRuleException(
    BusinessRuleException ex,
    WebRequest request
  ) {
    log.warn("Business rule violation: {} - {}", ex.getErrorCode(), ex.getMessage());

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ex.getErrorCode().name(),
      ex.getMessage(),
      ex.actionCode()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(schema);
  }

  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<GlobalErrorSchema> handleExternalServiceException(
    ExternalServiceException ex,
    WebRequest request
  ) {
    log.error("External service error: {} - {} from {}", ex.getErrorCode(), ex.getMessage(), ex.serviceName());

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ex.getErrorCode().name(),
      ex.getMessage(),
      ex.actionCode()
    );

    HttpStatus status = ex.httpStatus() == 504 ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
    return ResponseEntity.status(status).body(schema);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<GlobalErrorSchema> handleAuthenticationException(
    AuthenticationException ex,
    WebRequest request
  ) {
    log.warn("Authentication error: {} - {}", ex.getErrorCode(), ex.getMessage());

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ex.getErrorCode().name(),
      ex.getMessage(),
      ex.actionCode()
    );

    HttpStatus status = ex.httpStatus() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
    return ResponseEntity.status(status).body(schema);
  }

  @ExceptionHandler(SystemException.class)
  public ResponseEntity<GlobalErrorSchema> handleSystemException(
    SystemException ex,
    WebRequest request
  ) {
    log.error("System error: {} - {}", ex.getErrorCode(), ex.getMessage());

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ex.getErrorCode().name(),
      ex.getMessage(),
      ex.actionCode()
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(schema);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<GlobalErrorSchema> handleGenericException(
    Exception ex,
    WebRequest request
  ) {
    log.error("Unhandled exception: {}", ex.getMessage(), ex);

    GlobalErrorSchema schema = GlobalErrorSchema.of(
      ErrorCode.ERR_SYS_001.name(),
      "An internal error occurred",
      "REVIEW"
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(schema);
  }
}
```

- [ ] Test passes: `./gradlew test --tests "GlobalExceptionHandlerTest"`
- [ ] `@RestControllerAdvice` annotation present (Law VI)

---

## Task 6: Error Code Mapper (MANUAL — custom business logic)

### Test First

Create: `src/test/java/com/agentbanking/shared/error/infrastructure/primary/ErrorCodeMapperTest.java`

```java
package com.agentbanking.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;
import com.agentbanking.shared.error.domain.ErrorCode;

@UnitTest
@DisplayName("ErrorCodeMapper")
class ErrorCodeMapperTest {

  private final ErrorCodeMapper mapper = new ErrorCodeMapper();

  @Nested
  class LegacyErrorMapping {

    @Test
    void shouldMapInvalidRequestToVal001() {
      assertThat(mapper.fromLegacyError("INVALID_REQUEST")).contains(ErrorCode.ERR_VAL_001);
    }

    @Test
    void shouldMapInvalidAmountToVal002() {
      assertThat(mapper.fromLegacyError("INVALID_AMOUNT")).contains(ErrorCode.ERR_VAL_002);
    }

    @Test
    void shouldMapInsufficientFloatToBiz201() {
      assertThat(mapper.fromLegacyError("INSUFFICIENT_FLOAT")).contains(ErrorCode.ERR_BIZ_201);
    }

    @Test
    void shouldMapDailyLimitExceededToBiz203() {
      assertThat(mapper.fromLegacyError("DAILY_LIMIT_EXCEEDED")).contains(ErrorCode.ERR_BIZ_203);
    }

    @Test
    void shouldMapCbsTimeoutToExt101() {
      assertThat(mapper.fromLegacyError("CBS_TIMEOUT")).contains(ErrorCode.ERR_EXT_101);
    }

    @Test
    void shouldMapCbsErrorToExt102() {
      assertThat(mapper.fromLegacyError("CBS_ERROR")).contains(ErrorCode.ERR_EXT_102);
    }

    @Test
    void shouldMapBillerTimeoutToExt201() {
      assertThat(mapper.fromLegacyError("BILLER_TIMEOUT")).contains(ErrorCode.ERR_EXT_201);
    }

    @Test
    void shouldMapBillerErrorToExt202() {
      assertThat(mapper.fromLegacyError("BILLER_ERROR")).contains(ErrorCode.ERR_EXT_202);
    }

    @Test
    void shouldMapInvalidCredentialsToAuth001() {
      assertThat(mapper.fromLegacyError("INVALID_CREDENTIALS")).contains(ErrorCode.ERR_AUTH_001);
    }

    @Test
    void shouldMapAccountLockedToAuth002() {
      assertThat(mapper.fromLegacyError("ACCOUNT_LOCKED")).contains(ErrorCode.ERR_AUTH_004);
    }

    @Test
    void shouldMapSanctionsBlockToAuth101() {
      assertThat(mapper.fromLegacyError("SANCTIONS_BLOCK")).contains(ErrorCode.ERR_AUTH_101);
    }

    @Test
    void shouldMapInternalErrorToSys001() {
      assertThat(mapper.fromLegacyError("INTERNAL_ERROR")).contains(ErrorCode.ERR_SYS_001);
    }

    @Test
    void shouldReturnEmptyForUnknownLegacyError() {
      assertThat(mapper.fromLegacyError("UNKNOWN_ERROR")).isEmpty();
    }
  }

  @Nested
  class IsoErrorMapping {

    @Test
    void shouldMapIsoEncodingFailed() {
      assertThat(mapper.fromLegacyError("ISO_ENCODING_FAILED")).contains(ErrorCode.ERR_ISO_001);
    }

    @Test
    void shouldMapSwitchConnectionFailed() {
      assertThat(mapper.fromLegacyError("SWITCH_CONNECTION_FAILED")).contains(ErrorCode.ERR_ISO_002);
    }
  }

  @Nested
  class HsmErrorMapping {

    @Test
    void shouldMapHsmPinBlockFailed() {
      assertThat(mapper.fromLegacyError("HSM_PIN_BLOCK_FAILED")).contains(ErrorCode.ERR_HSM_001);
    }

    @Test
    void shouldMapHsmHardwareFailed() {
      assertThat(mapper.fromLegacyError("HSM_HARDWARE_FAILED")).contains(ErrorCode.ERR_HSM_002);
    }
  }
}
```

### Implementation

Create: `src/main/java/com/agentbanking/shared/error/infrastructure/primary/ErrorCodeMapper.java`

```java
package com.agentbanking.shared.error.infrastructure.primary;

import java.util.Map;
import java.util.Optional;
import com.agentbanking.shared.error.domain.ErrorCode;

public class ErrorCodeMapper {

  private static final Map<String, ErrorCode> LEGACY_ERROR_MAP = Map.ofEntries(
    Map.entry("INVALID_REQUEST", ErrorCode.ERR_VAL_001),
    Map.entry("INVALID_AMOUNT", ErrorCode.ERR_VAL_002),
    Map.entry("INVALID_ACCOUNT", ErrorCode.ERR_VAL_003),
    Map.entry("INVALID_MYKAD", ErrorCode.ERR_VAL_004),
    Map.entry("INVALID_PHONE", ErrorCode.ERR_VAL_006),
    Map.entry("INVALID_PIN", ErrorCode.ERR_VAL_008),
    Map.entry("FEE_CONFIG_NOT_FOUND", ErrorCode.ERR_VAL_012),
    Map.entry("FEE_CONFIG_EXPIRED", ErrorCode.ERR_VAL_013),

    Map.entry("INSUFFICIENT_FLOAT", ErrorCode.ERR_BIZ_201),
    Map.entry("INSUFFICIENT_BALANCE", ErrorCode.ERR_BIZ_202),
    Map.entry("DAILY_LIMIT_EXCEEDED", ErrorCode.ERR_BIZ_203),
    Map.entry("DAILY_COUNT_LIMIT_EXCEEDED", ErrorCode.ERR_BIZ_204),
    Map.entry("FLOAT_CAP_EXCEEDED", ErrorCode.ERR_BIZ_205),
    Map.entry("AGENT_FLOAT_NOT_FOUND", ErrorCode.ERR_BIZ_206),
    Map.entry("AGENT_DEACTIVATED", ErrorCode.ERR_BIZ_207),
    Map.entry("GEOFENCE_VIOLATION", ErrorCode.ERR_BIZ_209),
    Map.entry("SELF_APPROVAL", ErrorCode.ERR_BIZ_210),
    Map.entry("REVERSAL_WINDOW_EXPIRED", ErrorCode.ERR_BIZ_301),
    Map.entry("ALREADY_REVERSED", ErrorCode.ERR_BIZ_302),

    Map.entry("CBS_TIMEOUT", ErrorCode.ERR_EXT_101),
    Map.entry("CBS_ERROR", ErrorCode.ERR_EXT_102),

    Map.entry("BILLER_TIMEOUT", ErrorCode.ERR_EXT_201),
    Map.entry("BILLER_ERROR", ErrorCode.ERR_EXT_202),

    Map.entry("JPN_UNAVAILABLE", ErrorCode.ERR_EXT_301),
    Map.entry("BIOMETRIC_UNAVAILABLE", ErrorCode.ERR_EXT_302),
    Map.entry("BIOMETRIC_MISMATCH", ErrorCode.ERR_EXT_303),

    Map.entry("PAYNET_TIMEOUT", ErrorCode.ERR_EXT_401),
    Map.entry("PAYNET_ERROR", ErrorCode.ERR_EXT_402),
    Map.entry("ISO_SOCKET_LOST", ErrorCode.ERR_EXT_403),

    Map.entry("INVALID_CREDENTIALS", ErrorCode.ERR_AUTH_001),
    Map.entry("MISSING_TOKEN", ErrorCode.ERR_AUTH_002),
    Map.entry("AUTH_SERVICE_UNAVAILABLE", ErrorCode.ERR_AUTH_003),
    Map.entry("RATE_LIMIT_EXCEEDED", ErrorCode.ERR_AUTH_004),
    Map.entry("ACCOUNT_LOCKED", ErrorCode.ERR_AUTH_004),
    Map.entry("SANCTIONS_BLOCK", ErrorCode.ERR_AUTH_101),
    Map.entry("PIN_LOCKED", ErrorCode.ERR_AUTH_102),
    Map.entry("TOKEN_EXPIRED", ErrorCode.ERR_AUTH_103),

    Map.entry("INTERNAL_ERROR", ErrorCode.ERR_SYS_001),
    Map.entry("GPS_UNAVAILABLE", ErrorCode.ERR_SYS_002),
    Map.entry("TEMPORAL_CORRUPTION", ErrorCode.ERR_SYS_003),
    Map.entry("TEMPORAL_NOT_FOUND", ErrorCode.ERR_SYS_004),

    Map.entry("ISO_ENCODING_FAILED", ErrorCode.ERR_ISO_001),
    Map.entry("ISO_UNMARSHAL_FAILED", ErrorCode.ERR_ISO_002),
    Map.entry("STAN_EXHAUSTED", ErrorCode.ERR_ISO_003),
    Map.entry("SWITCH_CONNECTION_FAILED", ErrorCode.ERR_ISO_002),

    Map.entry("CBS_SOAP_TIMEOUT", ErrorCode.ERR_CBS_001),
    Map.entry("CBS_QUEUE_EMPTY", ErrorCode.ERR_CBS_002),
    Map.entry("CBS_FORMAT_ERROR", ErrorCode.ERR_CBS_003),

    Map.entry("HSM_PIN_BLOCK_FAILED", ErrorCode.ERR_HSM_001),
    Map.entry("HSM_PIN_VERIFY_FAILED", ErrorCode.ERR_HSM_002),
    Map.entry("HSM_KEY_VAULT_VIOLATION", ErrorCode.ERR_HSM_003),
    Map.entry("HSM_CONNECTION_LOST", ErrorCode.ERR_HSM_004),

    Map.entry("BILLER_API_KEY_FAILED", ErrorCode.ERR_BG_001),
    Map.entry("BILLER_IDEMPOTENCY_CONFLICT", ErrorCode.ERR_BG_002)
  );

  public Optional<ErrorCode> fromLegacyError(String legacyError) {
    return Optional.ofNullable(LEGACY_ERROR_MAP.get(legacyError));
  }
}
```

- [ ] Test passes: `./gradlew test --tests "ErrorCodeMapperTest"`
- [ ] Implementation created

---

## Task 7: Verify ArchUnit Passes

- [ ] Run: `./gradlew test --tests "*HexagonalArchTest*"`
- [ ] Verify NO Spring/JPA imports in `shared/error/domain/`

---

## Summary

| Task | What | Seed4J or Manual | Files |
|------|------|-----------------|-------|
| 1 | Package structure, `@SharedKernel`, `Assert.java`, `AssertionException.java` | **Seed4J** (already done) | 0 new |
| 2 | `ErrorCodeCategory`, `ErrorCode` (60+ codes) | **Manual** | 2 |
| 3 | `GlobalErrorSchema` record | **Manual** | 2 (test + impl) |
| 4 | `BusinessException` + 5 subclasses | **Manual** | 6 (test + impl) |
| 5 | `GlobalExceptionHandler` (`@RestControllerAdvice`) | **Manual** | 2 (test + impl) |
| 6 | `ErrorCodeMapper` (legacy → standardized) | **Manual** | 2 (test + impl) |
| 7 | ArchUnit verification | **Verify** | 0 |

All domain classes in `shared/error/domain/` MUST have ZERO Spring/JPA imports. Infrastructure classes in `infrastructure/primary/` use `@RestControllerAdvice`.
