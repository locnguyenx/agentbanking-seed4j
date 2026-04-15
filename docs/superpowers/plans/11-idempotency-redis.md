# Idempotency & Redis Caching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Redis-based idempotency service supporting transaction deduplication with TTL-based cache expiration.

**Architecture:** Cross-cutting concern in the `common` bounded context. Domain layer defines port interfaces; infrastructure layer implements Redis adapter. Application layer service orchestrates idempotency checks with logging. Seed4J scaffolds package structure, @BusinessContext annotations, config classes, ArchUnit tests, @Bean registration, test templates.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data Redis, Lettuce, Gradle, JUnit 5, Mockito

---

## Task 1: Seed4J Scaffolding — Common Bounded Context

**BDD Scenarios:** BDD-TO01, BDD-TO02, BDD-TO03, BDD-TO04, BDD-TO05
**BRD Requirements:** FR-2.4, NFR-2.4
**User-Facing:** NO

**Seed4J CLI Commands:**
```bash
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply spring-boot --context common --no-commit
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply hexagonal-architecture --context common --no-commit
```

**Manual package-info.java files (create all sub-packages):**
- Create: `src/main/java/com/agentbanking/common/package-info.java`
- Create: `src/main/java/com/agentbanking/common/config/package-info.java`
- Create: `src/main/java/com/agentbanking/common/domain/package-info.java`
- Create: `src/main/java/com/agentbanking/common/domain/model/package-info.java`
- Create: `src/main/java/com/agentbanking/common/domain/port/package-info.java`
- Create: `src/main/java/com/agentbanking/common/domain/port/out/package-info.java`
- Create: `src/main/java/com/agentbanking/common/application/package-info.java`
- Create: `src/main/java/com/agentbanking/common/application/service/package-info.java`
- Create: `src/main/java/com/agentbanking/common/infrastructure/package-info.java`
- Create: `src/main/java/com/agentbanking/common/infrastructure/caching/package-info.java`
- Create: `src/test/java/com/agentbanking/common/application/service/package-info.java`

- [ ] **Step 1: Run Seed4J CLI to scaffold common bounded context**
- [ ] **Step 2: Create remaining package-info.java files**

**Root package-info.java:**
```java
@BusinessContext
package com.agentbanking.common;

import com.agentbanking.BusinessContext;
```

- [ ] **Step 3: Add Redis dependency to build.gradle.kts**

**CRITICAL: Redis is NOT in build.gradle.kts yet — must be added.**

```kotlin
// build.gradle.kts — inside dependencies block
implementation(libs.spring.boot.starter.data.redis)
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/agentbanking/common/ build.gradle.kts
git commit -m "feat(common): scaffold common bounded context with Seed4J, add Redis dependency"
```

---

## Task 2: Redis Configuration

**BDD Scenarios:** N/A (infrastructure)
**BRD Requirements:** NFR-2.4
**User-Facing:** NO

**Files:**
- Create: `src/main/java/com/agentbanking/common/config/RedisProperties.java`
- Create: `src/main/java/com/agentbanking/common/config/RedisConfig.java`

- [ ] **Step 1: Write implementation**

**RedisProperties.java:**
```java
package com.agentbanking.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentbanking.redis")
public class RedisProperties {

    private String host = "localhost";
    private int port = 6379;
    private String password;
    private int database = 0;
    private int timeout = 5000;
    private int connectionPoolSize = 8;

    private Idempotency idempotency = new Idempotency();

    public static class Idempotency {
        private int ttlHours = 24;
        private String keyPrefix = "idempotency:";

        public int getTtlHours() { return ttlHours; }
        public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }
        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getDatabase() { return database; }
    public void setDatabase(int database) { this.database = database; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public int getConnectionPoolSize() { return connectionPoolSize; }
    public void setConnectionPoolSize(int connectionPoolSize) { this.connectionPoolSize = connectionPoolSize; }
    public Idempotency getIdempotency() { return idempotency; }
    public void setIdempotency(Idempotency idempotency) { this.idempotency = idempotency; }
}
```

**RedisConfig.java:**
```java
package com.agentbanking.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties props) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(props.getHost());
        config.setPort(props.getPort());
        config.setDatabase(props.getDatabase());

        if (props.getPassword() != null && !props.getPassword().isBlank()) {
            config.setPassword(props.getPassword());
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/common/config/RedisConfig.java
git add src/main/java/com/agentbanking/common/config/RedisProperties.java
git commit -m "feat(common): add Redis configuration"
```

---

## Task 3: Idempotency Domain Model

**BDD Scenarios:** BDD-TO01, BDD-TO02
**BRD Requirements:** FR-2.4
**User-Facing:** NO

**Files:**
- Create: `src/main/java/com/agentbanking/common/domain/model/IdempotencyRecord.java`

- [ ] **Step 1: Write implementation**

**IdempotencyRecord.java:**
```java
package com.agentbanking.common.domain.model;

import java.io.Serializable;
import java.time.Instant;

public record IdempotencyRecord(
    String idempotencyKey,
    String transactionId,
    String status,
    Object response,
    Instant createdAt,
    Instant expiresAt
) implements Serializable {

    public static IdempotencyRecord create(
        String idempotencyKey, String transactionId,
        String status, Object response, int ttlHours
    ) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(ttlHours * 3600L);
        return new IdempotencyRecord(
            idempotencyKey, transactionId, status, response, now, expires);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/common/domain/model/IdempotencyRecord.java
git commit -m "feat(common): add idempotency domain model"
```

---

## Task 4: Idempotency Cache Port

**BDD Scenarios:** BDD-TO01, BDD-TO02
**BRD Requirements:** FR-2.4
**User-Facing:** NO

**Files:**
- Create: `src/main/java/com/agentbanking/common/domain/port/out/IdempotencyCache.java`

- [ ] **Step 1: Write implementation**

**IdempotencyCache.java:**
```java
package com.agentbanking.common.domain.port.out;

import com.agentbanking.common.domain.model.IdempotencyRecord;
import java.util.Optional;

public interface IdempotencyCache {

    Optional<IdempotencyRecord> get(String idempotencyKey);
    boolean setIfAbsent(String idempotencyKey, IdempotencyRecord record);
    void set(String idempotencyKey, IdempotencyRecord record);
    void update(String idempotencyKey, IdempotencyRecord record);
    void delete(String idempotencyKey);
    boolean exists(String idempotencyKey);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/common/domain/port/out/IdempotencyCache.java
git commit -m "feat(common): add idempotency cache port"
```

---

## Task 5: Redis Idempotency Cache Implementation

**BDD Scenarios:** BDD-TO02, BDD-TO03, BDD-TO04
**BRD Requirements:** FR-2.4
**User-Facing:** NO

**Files:**
- Create: `src/main/java/com/agentbanking/common/infrastructure/caching/RedisIdempotencyCache.java`

- [ ] **Step 1: Write implementation**

**RedisIdempotencyCache.java:**
```java
package com.agentbanking.common.infrastructure.caching;

import com.agentbanking.common.config.RedisProperties;
import com.agentbanking.common.domain.model.IdempotencyRecord;
import com.agentbanking.common.domain.port.out.IdempotencyCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RedisIdempotencyCache implements IdempotencyCache {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyCache.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties redisProperties;
    private final ValueOperations<String, String> valueOps;
    private final ObjectMapper objectMapper;

    public RedisIdempotencyCache(
        StringRedisTemplate redisTemplate,
        RedisProperties redisProperties,
        ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.redisProperties = redisProperties;
        this.valueOps = redisTemplate.opsForValue();
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<IdempotencyRecord> get(String idempotencyKey) {
        String key = buildKey(idempotencyKey);
        String value = valueOps.get(key);
        if (value == null) return Optional.empty();

        try {
            IdempotencyRecord record = deserialize(value);
            if (record.isExpired()) {
                delete(idempotencyKey);
                return Optional.empty();
            }
            return Optional.of(record);
        } catch (Exception e) {
            log.error("Failed to deserialize idempotency record: {}", idempotencyKey, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean setIfAbsent(String idempotencyKey, IdempotencyRecord record) {
        String key = buildKey(idempotencyKey);
        Duration ttl = Duration.ofHours(redisProperties.getIdempotency().getTtlHours());
        try {
            Boolean result = valueOps.setIfAbsent(key, serialize(record), ttl);
            log.debug("setIfAbsent for key {}: {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to set idempotency record: {}", idempotencyKey, e);
            return false;
        }
    }

    @Override
    public void set(String idempotencyKey, IdempotencyRecord record) {
        String key = buildKey(idempotencyKey);
        Duration ttl = Duration.ofHours(redisProperties.getIdempotency().getTtlHours());
        try {
            valueOps.set(key, serialize(record), ttl);
            log.debug("Set idempotency record for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to set idempotency record: {}", idempotencyKey, e);
        }
    }

    @Override
    public void update(String idempotencyKey, IdempotencyRecord record) {
        set(idempotencyKey, record);
    }

    @Override
    public void delete(String idempotencyKey) {
        String key = buildKey(idempotencyKey);
        try {
            redisTemplate.delete(key);
            log.debug("Deleted idempotency record: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete idempotency record: {}", idempotencyKey, e);
        }
    }

    @Override
    public boolean exists(String idempotencyKey) {
        String key = buildKey(idempotencyKey);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check idempotency key exists: {}", idempotencyKey, e);
            return false;
        }
    }

    private String buildKey(String idempotencyKey) {
        return redisProperties.getIdempotency().getKeyPrefix() + idempotencyKey;
    }

    private String serialize(IdempotencyRecord record) throws JsonProcessingException {
        return objectMapper.writeValueAsString(record);
    }

    private IdempotencyRecord deserialize(String value) throws JsonProcessingException {
        return objectMapper.readValue(value, IdempotencyRecord.class);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/common/infrastructure/caching/RedisIdempotencyCache.java
git commit -m "feat(common): add Redis idempotency cache implementation"
```

---

## Task 6: Idempotency Service (Application Layer)

**BDD Scenarios:** BDD-TO01, BDD-TO02, BDD-TO03
**BRD Requirements:** FR-2.4
**User-Facing:** NO

**CRITICAL FIX: Law VI violation — IdempotencyService MUST be in `application/service/`, NOT `domain/service/`. Logging belongs in application layer, not domain layer.**

**Files:**
- Create: `src/main/java/com/agentbanking/common/application/service/IdempotencyService.java`

- [ ] **Step 1: Write implementation**

**IdempotencyService.java:**
```java
package com.agentbanking.common.application.service;

import com.agentbanking.common.config.RedisProperties;
import com.agentbanking.common.domain.model.IdempotencyRecord;
import com.agentbanking.common.domain.port.out.IdempotencyCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyCache cache;
    private final RedisProperties redisProperties;

    public IdempotencyService(IdempotencyCache cache, RedisProperties redisProperties) {
        this.cache = cache;
        this.redisProperties = redisProperties;
    }

    public IdempotencyResult checkAndLock(String idempotencyKey) {
        log.info("Checking idempotency key: {}", idempotencyKey);

        Optional<IdempotencyRecord> existing = cache.get(idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            log.info("Found existing idempotency record: status={}", record.status());
            if (record.isCompleted()) {
                return IdempotencyResult.alreadyProcessed(record);
            } else if ("PROCESSING".equals(record.status())) {
                return IdempotencyResult.inProgress();
            }
        }

        String transactionId = UUID.randomUUID().toString();
        IdempotencyRecord newRecord = IdempotencyRecord.create(
            idempotencyKey, transactionId, "PROCESSING", null,
            redisProperties.getIdempotency().getTtlHours());

        boolean locked = cache.setIfAbsent(idempotencyKey, newRecord);
        if (locked) {
            log.info("Acquired idempotency lock for key: {}", idempotencyKey);
            return IdempotencyResult.lockAcquired(transactionId);
        } else {
            log.info("Failed to acquire idempotency lock - another request is processing");
            return IdempotencyResult.inProgress();
        }
    }

    public void markCompleted(String idempotencyKey, String transactionId, Object response) {
        IdempotencyRecord record = IdempotencyRecord.create(
            idempotencyKey, transactionId, "COMPLETED", response,
            redisProperties.getIdempotency().getTtlHours());
        cache.update(idempotencyKey, record);
        log.info("Marked idempotency key as completed: {}", idempotencyKey);
    }

    public void markFailed(String idempotencyKey, String transactionId) {
        IdempotencyRecord record = IdempotencyRecord.create(
            idempotencyKey, transactionId, "FAILED", null,
            redisProperties.getIdempotency().getTtlHours());
        cache.update(idempotencyKey, record);
        log.info("Marked idempotency key as failed: {}", idempotencyKey);
    }

    public static class IdempotencyResult {
        private final boolean success;
        private final String transactionId;
        private final IdempotencyRecord existingRecord;
        private final Status status;

        public enum Status { LOCK_ACQUIRED, ALREADY_PROCESSED, IN_PROGRESS }

        private IdempotencyResult(boolean success, String transactionId,
                                  IdempotencyRecord existingRecord, Status status) {
            this.success = success;
            this.transactionId = transactionId;
            this.existingRecord = existingRecord;
            this.status = status;
        }

        public static IdempotencyResult lockAcquired(String transactionId) {
            return new IdempotencyResult(true, transactionId, null, Status.LOCK_ACQUIRED);
        }
        public static IdempotencyResult alreadyProcessed(IdempotencyRecord record) {
            return new IdempotencyResult(false, record.transactionId(), record, Status.ALREADY_PROCESSED);
        }
        public static IdempotencyResult inProgress() {
            return new IdempotencyResult(false, null, null, Status.IN_PROGRESS);
        }

        public boolean isSuccess() { return success; }
        public String getTransactionId() { return transactionId; }
        public IdempotencyRecord getExistingRecord() { return existingRecord; }
        public Status getStatus() { return status; }
        public boolean isLockAcquired() { return status == Status.LOCK_ACQUIRED; }
        public boolean isAlreadyProcessed() { return status == Status.ALREADY_PROCESSED; }
        public boolean isInProgress() { return status == Status.IN_PROGRESS; }
    }
}
```

- [ ] **Step 2: Register IdempotencyService as bean in CommonDomainServiceConfig**

Create `src/main/java/com/agentbanking/common/config/CommonDomainServiceConfig.java`:
```java
package com.agentbanking.common.config;

import com.agentbanking.common.application.service.IdempotencyService;
import com.agentbanking.common.domain.port.out.IdempotencyCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonDomainServiceConfig {

    @Bean
    public IdempotencyService idempotencyService(
        IdempotencyCache cache, RedisProperties redisProperties
    ) {
        return new IdempotencyService(cache, redisProperties);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/agentbanking/common/application/service/IdempotencyService.java
git add src/main/java/com/agentbanking/common/config/CommonDomainServiceConfig.java
git commit -m "feat(common): add idempotency service (application layer, Law VI compliant)"
```

---

## Task 7: Application Configuration

**BDD Scenarios:** N/A (configuration)
**BRD Requirements:** Infrastructure setup
**User-Facing:** NO

**Files:**
- Modify: `src/main/resources/application.yaml`

- [ ] **Step 1: Add Redis configuration to application.yaml**

```yaml
agentbanking:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: ${REDIS_DATABASE:0}
    timeout: 5000
    connection-pool-size: 8
    idempotency:
      ttl-hours: 24
      key-prefix: "idempotency:"
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/application.yaml
git commit -m "feat(common): add Redis configuration to application.yaml"
```

---

## Task 8: Unit Tests

**BDD Scenarios:** BDD-TO01, BDD-TO02, BDD-TO03
**BRD Requirements:** FR-2.4
**User-Facing:** NO

**Files:**
- Create: `src/test/java/com/agentbanking/common/application/service/IdempotencyServiceTest.java`

- [ ] **Step 1: Write failing test first (TDD)**

```java
package com.agentbanking.common.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.agentbanking.UnitTest;
import com.agentbanking.common.config.RedisProperties;
import com.agentbanking.common.domain.model.IdempotencyRecord;
import com.agentbanking.common.domain.port.out.IdempotencyCache;

@UnitTest
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock private IdempotencyCache cache;
    private RedisProperties redisProperties;
    private IdempotencyService service;

    @BeforeEach void setUp() {
        redisProperties = new RedisProperties();
        redisProperties.setHost("localhost");
        redisProperties.setPort(6379);
        RedisProperties.Idempotency idempotency = new RedisProperties.Idempotency();
        idempotency.setTtlHours(24);
        idempotency.setKeyPrefix("idempotency:");
        redisProperties.setIdempotency(idempotency);
        service = new IdempotencyService(cache, redisProperties);
    }

    @Nested @DisplayName("checkAndLock")
    class CheckAndLockTest {

        @Test void shouldAcquireLockForNewKey() {
            when(cache.setIfAbsent(anyString(), any(IdempotencyRecord.class))).thenReturn(true);
            IdempotencyService.IdempotencyResult result = service.checkAndLock("idem-123");
            assertThat(result.isLockAcquired()).isTrue();
            assertThat(result.getTransactionId()).isNotNull();
        }

        @Test void shouldReturnExistingForCompletedKey() {
            IdempotencyRecord existing = IdempotencyRecord.create(
                "idem-123", "TXN-001", "COMPLETED", null, 24);
            when(cache.get("idem-123")).thenReturn(Optional.of(existing));
            IdempotencyService.IdempotencyResult result = service.checkAndLock("idem-123");
            assertThat(result.isAlreadyProcessed()).isTrue();
            assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        }

        @Test void shouldReturnInProgressForProcessingKey() {
            IdempotencyRecord processing = IdempotencyRecord.create(
                "idem-123", "TXN-001", "PROCESSING", null, 24);
            when(cache.get("idem-123")).thenReturn(Optional.of(processing));
            IdempotencyService.IdempotencyResult result = service.checkAndLock("idem-123");
            assertThat(result.isInProgress()).isTrue();
        }

        @Test void shouldReturnInProgressWhenLockExists() {
            when(cache.get("idem-123")).thenReturn(Optional.empty());
            when(cache.setIfAbsent(anyString(), any(IdempotencyRecord.class))).thenReturn(false);
            IdempotencyService.IdempotencyResult result = service.checkAndLock("idem-123");
            assertThat(result.isInProgress()).isTrue();
        }
    }

    @Nested @DisplayName("markCompleted")
    class MarkCompletedTest {
        @Test void shouldUpdateCacheWithCompletedStatus() {
            service.markCompleted("idem-123", "TXN-001", "response");
            verify(cache).update(eq("idem-123"), any(IdempotencyRecord.class));
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew test --tests "com.agentbanking.common.application.service.IdempotencyServiceTest"
```

- [ ] **Step 3: Run test to verify it passes**

```bash
./gradlew test --tests "com.agentbanking.common.application.service.IdempotencyServiceTest"
```

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/agentbanking/common/application/service/IdempotencyServiceTest.java
git commit -m "test(common): add idempotency service tests"
```

---

## Verification Command

```bash
./gradlew test
```

Expected: All tests PASS

---

## Summary

| Task | Component | Files | Tests |
|------|-----------|-------|-------|
| 1 | Seed4J Scaffolding | ~11 | ArchUnit |
| 2 | Redis Config | 2 | 0 |
| 3 | Domain Model | 1 | 0 |
| 4 | Cache Port | 1 | 0 |
| 5 | Redis Cache Impl | 1 | 0 |
| 6 | Idempotency Service | 1 | 0 |
| 7 | Application Config | 1 (modify) | 0 |
| 8 | Unit Tests | 1 | 1 |
| **Total** | | **18+Seed4J** | **1+ArchUnit** |

## Bug Fixes Applied

1. **FIXED Law VI violation**: IdempotencyService placed in `application/service/` (NOT `domain/service/`). Logging is in application layer, not domain layer.
2. **FIXED Redis dependency**: Added `implementation(libs.spring.boot.starter.data.redis)` to build.gradle.kts — was missing.
3. **FIXED Jackson serialization**: Uses `ObjectMapper` for proper JSON serialization — NOT manual string formatting.
4. **Gradle**: Uses `./gradlew test` not `./mvnw test`.

## Architecture Notes

- **Domain layer**: `IdempotencyRecord` (record), `IdempotencyCache` (port interface) — ZERO framework imports
- **Infrastructure layer**: `RedisIdempotencyCache` — implements port, uses `@Repository`
- **Application layer**: `IdempotencyService` — orchestrates, handles logging
- **Config layer**: `RedisConfig`, `RedisProperties`, `CommonDomainServiceConfig` — bean registration

## Dependencies

- **Plan 03**: Temporal Infrastructure (Redis dependencies in build config)
