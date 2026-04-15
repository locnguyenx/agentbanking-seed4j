---
name: seed4j-architecture-decisions
description: Use when deciding between microservices, modular monolith, or hybrid architecture for Seed4J projects
---

# Seed4J Architecture Decisions

## Overview

Seed4J supports multiple architectural patterns. Choose based on team size, deployment complexity, and business domain isolation needs.

## CLI Scaffolding Patterns

### Pattern 1: Modular Monolith (Single Project)

```bash
# Single .seed4j/ at project root
seed4j apply init --project-name "AgentBanking" --base-name AgentBanking --no-commit
seed4j apply gradle-java --package-name com.agentbanking --no-commit
# ... other modules
```

**Structure:**
```
project-root/
  .seed4j/           # Single location
  src/main/java/com/agentbanking/
    rules/           # Business context (package)
      domain/
        model/
        port/in/
        port/out/
        service/
      application/
      infrastructure/
        primary/     # Controllers
        secondary/  # JPA, Kafka adapters
    ledger/
    onboarding/
    ...
```

**Key Seed4J patterns:**
- Single `.seed4j/` at project root
- `wire/` directory for cross-cutting concerns
- `@BusinessContext` annotation for packages
- `@SharedKernel` annotation for shared code

### Pattern 2: Microservices (Multi-Project)

```bash
# Each service has its own .seed4j/
mkdir services/rules-service && cd services/rules-service
seed4j apply init --project-name "Rules" --base-name Rules --no-commit
seed4j apply gradle-java --package-name com.agentbanking.rules --no-commit

mkdir services/ledger-service && cd services/ledger-service
seed4j apply init --project-name "Ledger" --base-name Ledger --no-commit
seed4j apply gradle-java --package-name com.agentbanking.ledger --no-commit
```

**Structure:**
```
project-root/
  services/
    rules-service/
      .seed4j/
      build.gradle.kts
      src/main/java/...
    ledger-service/
      .seed4j/
      build.gradle.kts
      src/main/java/...
  settings.gradle.kts
```

### Pattern 3: Hybrid (Database-per-Service in Monolith)

```bash
# Scaffold as monolith, then configure multiple databases
seed4j apply init --project-name "MyProject" --base-name MyProject --no-commit
seed4j apply datasource-postgresql --no-commit
# ...
```

Then configure multiple datasources in `application.yml`:

```yaml
spring:
  datasource:
    rules:
      url: jdbc:postgresql://localhost:5432/rules_db
    ledger:
      url: jdbc:postgresql://localhost:5432/ledger_db
    commission:
      url: jdbc:postgresql://localhost:5432/commission_db
```

## Quick Decision Matrix

| Factor | Monolith | Microservices | Hybrid |
|--------|----------|---------------|--------|
| Team size | 1-10 | 20+ | 5-15 |
| Deployment | Single JAR | Multiple JARs | Single JAR |
| Database | Shared | Per-service | Per-service |
| Complexity | Low | High | Medium |
| Start time | Seconds | Minutes | Seconds |

## When to Use Each

### Modular Monolith (Recommended for Most)

- Team size < 10
- Business domains tightly coupled
- Rapid iteration important
- Simple ops requirements

**CLI command:**
```bash
seed4j apply init --project-name "Project" --base-name Project --node-package-manager npm --no-commit
seed4j apply gradle-java --package-name com.example --no-commit
```

### Microservices

- Team size > 20
- Strict security/compliance isolation
- Different scaling needs per service

**CLI command:**
```bash
# Each service scaffolded independently
cd service-a && seed4j apply init ...
cd service-b && seed4j apply init ...
```

### Hybrid

- Need database isolation
- Plan to migrate to microservices later
- Team size 5-15

**Setup:**
```bash
# Scaffold as monolith
seed4j apply init ...

# Configure multiple datasources
# Create DatabaseConfig for each context
```

## Multi-Datasource Setup (Hybrid)

```java
// config/CommissionDatabaseConfig.java
@Configuration
@EnableJpaRepositories(
  basePackages = "com.agentbanking.commission.infrastructure.secondary",
  entityManagerFactoryRef = "commissionEntityManagerFactory"
)
public class CommissionDatabaseConfig {
  @Bean(name = "commissionDataSource")
  public DataSource commissionDataSource() {
    var ds = new HikariDataSource();
    ds.setJdbcUrl("jdbc:postgresql://localhost:5432/commission_db");
    // ... config
    return ds;
  }
}
```

**application.yml:**
```yaml
spring:
  datasource:
    commission:
      url: jdbc:postgresql://${COMMISSION_DB_HOST:localhost}:${COMMISSION_DB_PORT:5432}/commission_db
      username: ${COMMISSION_DB_USERNAME:agentbanking}
      password: ${COMMISSION_DB_PASSWORD:agentbanking}
```

**Create database:**
```bash
docker exec -i postgres-container psql -U agentbanking -c "CREATE DATABASE commission_db;"
```

## Common Mistakes

| Mistake | Solution |
|---------|----------|
| Multiple .seed4j directories | Use single `.seed4j/` at root |
| Maven instead of Gradle | Use `seed4j apply gradle-java` |
| Public wire classes | Keep package-private |
| Database not created | Manually create before bootRun |
| Missing @Primary | Add to one EntityManagerFactory |

## See Also

- [seed4j-cli-setup](seed4j-cli-setup) - CLI installation and project creation
- [seed4j-modules](seed4j-modules) - Module reference for `seed4j apply`
- [seed4j-post-scaffold](seed4j-post-scaffold) - Post-scaffolding fixes and refactoring