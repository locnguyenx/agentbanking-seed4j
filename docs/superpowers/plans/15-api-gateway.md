# 15. API Gateway Sub-Plan

> **Bounding Context:** com.agentbanking.gateway
> **Wave:** 5.1
> **Depends On:** 01 (error registry), 02 (domain model), 11 (idempotency)
> **BDD Scenarios:** BDD-G01, BDD-G02, BDD-G01-EC-01 through BDD-G01-EC-05
> **BRD Requirements:** US-G01, US-G02, FR-12.1, FR-12.2, FR-12.3

**Goal:** Implement Spring Cloud Gateway with JWT validation, per-agent rate limiting via Redis, and resilient routing to backend services.

**Architecture:** Wave 5 — Gateway (reactive). Tier 2 entry point for all POS terminal requests. Uses Spring Cloud Gateway (WebFlux-based) with JWT authentication filter, Redis-backed rate limiting, and circuit breaker routing.

**Tech Stack:** Java 21, Spring Cloud Gateway (WebFlux), Spring Data Redis Reactive, JJWT, Resilience4j, JUnit 5, Mockito, Gradle

---

## Task 1: Seed4J Scaffolding

Run Seed4J CLI to scaffold the gateway service:

```bash
# Apply gateway module (includes spring-boot-webflux-empty + spring-cloud)
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply gateway \
  --context gateway \
  --package com.agentbanking.gateway \
  --no-commit
```

This creates:
- Reactive package structure: `config/`, `filter/`
- `@BusinessContext` annotation on package-info
- Gradle build configuration with gateway + webflux dependencies
- ArchUnit test stub
- Test base classes and `@UnitTest` annotation

- [ ] **Step 1: Run Seed4J scaffolding**
- [ ] **Step 2: Verify generated structure**
- [ ] **Step 3: Commit scaffolding**

```bash
git add . && git commit -m "feat(gateway): scaffold with Seed4J gateway module"
```

---

## Task 2: Gateway Properties (Write Manually)

**FIX: Use config properties for service URLs, NOT hardcoded.**

**Files:**
- Create: `src/main/java/com/agentbanking/gateway/config/GatewayProperties.java`

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentbanking.gateway")
public class GatewayProperties {

  private String keycloakUrl = "http://localhost:8080/realms/agentbanking";
  private String jwksUri;
  private RateLimit rateLimit = new RateLimit();
  private Routes routes = new Routes();

  public String getJwksUri() {
    return jwksUri != null ? jwksUri : keycloakUrl + "/protocol/openid-connect/certs";
  }
  public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }

  public String getKeycloakUrl() { return keycloakUrl; }
  public void setKeycloakUrl(String keycloakUrl) { this.keycloakUrl = keycloakUrl; }
  public RateLimit getRateLimit() { return rateLimit; }
  public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
  public Routes getRoutes() { return routes; }
  public void setRoutes(Routes routes) { this.routes = routes; }

  public static class RateLimit {
    private int requestsPerMinute = 60;
    private int transactionsPerHour = 5;
    private String redisKeyPrefix = "ratelimit:";
    public int getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(int v) { this.requestsPerMinute = v; }
    public int getTransactionsPerHour() { return transactionsPerHour; }
    public void setTransactionsPerHour(int v) { this.transactionsPerHour = v; }
    public String getRedisKeyPrefix() { return redisKeyPrefix; }
    public void setRedisKeyPrefix(String v) { this.redisKeyPrefix = v; }
  }

  public static class Routes {
    private String orchestratorUrl = "http://orchestrator:8080";
    private String onboardingUrl = "http://onboarding:8080";
    private String rulesUrl = "http://rules:8080";
    public String getOrchestratorUrl() { return orchestratorUrl; }
    public void setOrchestratorUrl(String v) { this.orchestratorUrl = v; }
    public String getOnboardingUrl() { return onboardingUrl; }
    public void setOnboardingUrl(String v) { this.onboardingUrl = v; }
    public String getRulesUrl() { return rulesUrl; }
    public void setRulesUrl(String v) { this.rulesUrl = v; }
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/gateway/config/GatewayProperties.java
git commit -m "feat(gateway): add configurable properties for service URLs"
```

---

## Task 3: JWT Authentication Filter (Write Manually)

**Files:**
- Create: `src/main/java/com/agentbanking/gateway/filter/JwtAuthenticationFilter.java`

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.gateway.filter;

import com.agentbanking.gateway.config.GatewayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";

  private final GatewayProperties properties;

  public JwtAuthenticationFilter(GatewayProperties properties) {
    this.properties = properties;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();

    if (request.getPath().value().startsWith("/health") ||
        request.getPath().value().startsWith("/docs/")) {
      return chain.filter(exchange);
    }

    String authHeader = request.getHeaders().getFirst("Authorization");

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      return unauthorized(exchange, "ERR_AUTH_002");
    }

    String token = authHeader.substring(BEARER_PREFIX.length());

    try {
      Claims claims = validateToken(token);
      String agentId = claims.get("agent_id", String.class);
      String terminalId = claims.get("terminal_id", String.class);

      ServerHttpRequest modifiedRequest = request.mutate()
        .header("X-Agent-Id", agentId != null ? agentId : "")
        .header("X-Terminal-Id", terminalId != null ? terminalId : "")
        .build();

      return chain.filter(exchange.mutate().request(modifiedRequest).build());
    } catch (Exception e) {
      log.error("JWT validation failed: {}", e.getMessage());
      return unauthorized(exchange, "ERR_AUTH_001");
    }
  }

  private Claims validateToken(String token) {
    return Jwts.parser()
      .verifyWith(getSigningKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }

  private SecretKey getSigningKey() {
    String secret = System.getenv("JWT_SECRET");
    byte[] keyBytes = secret != null
      ? secret.getBytes(StandardCharsets.UTF_8)
      : Base64.getDecoder().decode("dGhpcy1pcy1hLXZlcmlmeS1zZWNyZXQta2V5LWZvci1nYXRld2F5");
    return Keys.hmacShaKeyFor(keyBytes);
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String errorCode) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().add("Content-Type", "application/json");
    String body = String.format(
      "{\"status\":\"FAILED\",\"error\":{\"code\":\"%s\",\"message\":\"Authentication failed\"}}",
      errorCode
    );
    return exchange.getResponse().writeWith(Mono.just(
      exchange.getResponse().bufferFactory().wrap(body.getBytes())
    ));
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/gateway/filter/JwtAuthenticationFilter.java
git commit -m "feat(gateway): add JWT authentication filter"
```

---

## Task 4: Rate Limit Filter (Write Manually)

**FIX: Use Spring Data Redis Reactive (not hardcoded Redis calls).**

**Files:**
- Create: `src/main/java/com/agentbanking/gateway/filter/RateLimitFilter.java`
- Create: `src/test/java/com/agentbanking/gateway/filter/RateLimitFilterTest.java`

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.gateway.filter;

import com.agentbanking.gateway.config.GatewayProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RateLimitFilter implements GlobalFilter {

  private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

  private final ReactiveStringRedisTemplate redisTemplate;
  private final GatewayProperties properties;

  public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate, GatewayProperties properties) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();

    if (!path.startsWith("/api/v1/")) {
      return chain.filter(exchange);
    }

    String terminalId = exchange.getRequest().getHeaders().getFirst("X-Terminal-Id");
    if (terminalId == null) {
      terminalId = "unknown";
    }

    String redisKey = properties.getRateLimit().getRedisKeyPrefix() + terminalId;

    return redisTemplate.opsForValue()
      .increment(redisKey)
      .flatMap(count -> {
        if (count == 1) {
          redisTemplate.expire(redisKey, Duration.ofHours(1)).subscribe();
        }

        int limit = properties.getRateLimit().getTransactionsPerHour();

        if (count > limit) {
          log.warn("Rate limit exceeded for terminal: {}", terminalId);
          exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
          exchange.getResponse().getHeaders().add("Content-Type", "application/json");
          String body = "{\"status\":\"FAILED\",\"error\":{\"code\":\"ERR_AUTH_004\",\"message\":\"Rate limit exceeded\"}}";
          return exchange.getResponse().writeWith(Mono.just(
            exchange.getResponse().bufferFactory().wrap(body.getBytes())
          ));
        }

        return chain.filter(exchange);
      });
  }
}
```

**RateLimitFilterTest.java:**
```java
package com.agentbanking.gateway.filter;

import com.agentbanking.gateway.config.GatewayProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

  @Mock
  private ReactiveStringRedisTemplate redisTemplate;

  @Mock
  private ReactiveValueOperations<String, String> valueOperations;

  private GatewayProperties properties;
  private RateLimitFilter filter;

  @BeforeEach
  void setUp() {
    properties = new GatewayProperties();
    properties.getRateLimit().setTransactionsPerHour(5);
    properties.getRateLimit().setRedisKeyPrefix("ratelimit:");
    filter = new RateLimitFilter(redisTemplate, properties);
  }

  @Test
  @DisplayName("should allow request under rate limit")
  void shouldAllowUnderLimit() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(anyString())).thenReturn(Mono.just(3L));

    MockServerWebExchange exchange = MockServerWebExchange.from(
      MockServerHttpRequest.post("/api/v1/withdrawal")
        .header("X-Terminal-Id", "TERM-001")
    );

    StepVerifier.create(filter.filter(exchange, e -> Mono.empty()))
      .verifyComplete();

    assert exchange.getResponse().getStatusCode() == null;
  }

  @Test
  @DisplayName("should reject request over rate limit")
  void shouldRejectOverLimit() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(anyString())).thenReturn(Mono.just(6L));

    MockServerWebExchange exchange = MockServerWebExchange.from(
      MockServerHttpRequest.post("/api/v1/withdrawal")
        .header("X-Terminal-Id", "TERM-001")
    );

    StepVerifier.create(filter.filter(exchange, e -> Mono.empty()))
      .verifyComplete();

    assert exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
  }
}
```

- [ ] **Step 2: Run test**

```bash
./gradlew test --tests "RateLimitFilterTest"
```
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/agentbanking/gateway/filter/RateLimitFilter.java
git add src/test/java/com/agentbanking/gateway/filter/RateLimitFilterTest.java
git commit -m "feat(gateway): add Redis-based rate limit filter with tests"
```

---

## Task 5: Route Configuration (Write Manually)

**FIX: Use config properties for service URLs, NOT hardcoded.**

**Files:**
- Create: `src/main/java/com/agentbanking/gateway/config/RouteConfig.java`

- [ ] **Step 1: Write implementation**

```java
package com.agentbanking.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

  private final GatewayProperties properties;

  public RouteConfig(GatewayProperties properties) {
    this.properties = properties;
  }

  @Bean
  public RouteLocator customRoutes(RouteLocatorBuilder builder) {
    GatewayProperties.Routes routes = properties.getRoutes();

    return builder.routes()
      .route("transaction-orchestrator", r -> r
        .path("/api/v1/withdrawal/**", "/api/v1/deposit/**", "/api/v1/payment/**")
        .filters(f -> f
          .circuitBreaker(c -> c
            .setName("orchestrator")
            .setFallbackUri("forward:/fallback/orchestrator"))
          .retry(retry -> retry.setRetries(3).setMethods("POST")))
        .uri(routes.getOrchestratorUrl()))
      .route("onboarding", r -> r
        .path("/api/v1/agents/**", "/api/v1/terminals/**")
        .filters(f -> f
          .circuitBreaker(c -> c
            .setName("onboarding")
            .setFallbackUri("forward:/fallback/onboarding")))
        .uri(routes.getOnboardingUrl()))
      .route("rules", r -> r
        .path("/api/v1/rules/**")
        .uri(routes.getRulesUrl()))
      .build();
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agentbanking/gateway/config/RouteConfig.java
git commit -m "feat(gateway): add route configuration with configurable service URLs"
```

---

## Task 6: Application Configuration

**Files:**
- Create: `src/main/resources/application.yaml`

- [ ] **Step 1: Write implementation**

```yaml
spring:
  application:
    name: gateway
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: GET,POST,PUT,DELETE,OPTIONS
            allowedHeaders: "*"
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

agentbanking:
  gateway:
    keycloak-url: ${KEYCLOAK_URL:http://localhost:8080/realms/agentbanking}
    rate-limit:
      requests-per-minute: 60
      transactions-per-hour: 5
      redis-key-prefix: "ratelimit:"
    routes:
      orchestrator-url: ${ORCHESTRATOR_URL:http://orchestrator:8080}
      onboarding-url: ${ONBOARDING_URL:http://onboarding:8080}
      rules-url: ${RULES_URL:http://rules:8080}

resilience4j:
  circuitbreaker:
    instances:
      orchestrator:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 5s
        failureRateThreshold: 50
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/application.yaml
git commit -m "feat(gateway): add application configuration with service URL properties"
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
| 1 | Scaffolding | **Seed4J CLI** (gateway module) | ArchUnit |
| 2 | Gateway Properties | Manual (configurable URLs) | 0 |
| 3 | JWT Authentication Filter | Manual | 0 |
| 4 | Rate Limit Filter | Manual (ReactiveStringRedisTemplate) | 1 |
| 5 | Route Configuration | Manual (uses config properties) | 0 |
| 6 | Application Config | Manual | 0 |

**Key fixes from original plan:**
- Service URLs configurable via `application.yaml` properties (not hardcoded `http://orchestrator:8080`)
- Uses `ReactiveStringRedisTemplate` (Spring Data Redis Reactive) for rate limiting
- `./gradlew test` (not `./mvnw`)
- Seed4J scaffolding replaces manual module structure creation
- `GatewayProperties.Routes` inner class for typed URL configuration
