# Seed4J Guidelines for Monorepo Multi-Microservices

## 1. Seed4J CLI - Complete Configuration Options

Source: https://github.com/seed4j/seed4j-cli/blob/main/documentation/Commands.md

### 1.1 CLI Commands

```bash
seed4j --version                    # Show CLI + platform version
seed4j list                          # List all available modules
seed4j apply <module> [options]     # Apply a module to a project
seed4j apply <module> --help        # Show module-specific parameters
```

### 1.2 ALL Available CLI Parameters

| Parameter | Type | Required By | Description | Default |
|-----------|------|-------------|-------------|---------|
| `--project-path` | String | All modules | Target directory for module application | `.` (current directory) |
| `--[no-]commit` | Boolean | All modules | Whether to git commit changes | `true` |
| `--project-name` | String | `init` | Full project name (required for some modules) | `"Seed4J Sample Application"` |
| `--base-name` | String | `init`, `gateway` | The project's short name, used for naming files and classes (letters+numbers only) | `"seed4jSampleApplication"` |
| `--package-name` | String | `gradle-java`, `maven-java`, `gateway` | Base Java package (required for Java projects) | `"com.mycompany.myapp"` |
| `--node-package-manager` | String | `init` | Node package manager to use for Node.js projects (`npm` or `pnpm`) | `"npm"` |
| `--server-port` | Integer | `spring-boot` | HTTP server port | `8080` |
| `--end-of-line` | String | `init`, `prettier` | Line ending type (`lf` or `crlf`) | `"lf"` |
| `--indentation` | Integer | `init`, `prettier` | Number of spaces for indentation | `2` |
| `--spring-configuration-format` | String | `init`, `spring-boot` | Spring config format (`yaml` or `properties`) | `"yaml"` |

Options are module-specific. When a required option is missing, the CLI will show an error message indicating which option is required.

### 1.3 Parameter Reuse

Seed4J CLI automatically reuses parameters from previous module applications per `--project-path`. This means, when you apply multiple modules to the same directory:
- Parameters you've provided when applying one module will be remembered for subsequent module applications
- You don't need to specify the same parameters repeatedly for different modules
- Only new parameters or parameters you want to override need to be specified

```bash
# First module - provide all parameters
seed4j apply init --project-path ./rules-service \
  --project-name "Rules Service" \
  --base-name AgentBankingRules \
  --package-name com.agentbanking.rules \
  --node-package-manager npm

# Subsequent modules - only provide NEW parameters
seed4j apply gradle-java --project-path ./rules-service \
  --package-name com.agentbanking.rules  # Only NEW parameter needed

# Even simpler - if package-name was already set
seed4j apply java-base --project-path ./rules-service
```

**Key insight:** Parameters are stored in `.seed4j/modules/history.json` per project path. Each service directory has its OWN parameter history.

### 1.4 External Configuration
Seed4J CLI supports external configuration files to customize its behavior. The CLI automatically looks for a configuration file at
Location: `~/.config/seed4j-cli.yml`
If this file exists, it will be loaded automatically when the CLI starts.

```yaml
seed4j:
  # Hide modules from list/apply
  hidden-resources:
    slugs:
      - maven-java          # Hide specific modules
      - angular-core
    tags:
      - client              # Hide all client modules
      - setup

  # Extension mode for custom modules
  runtime:
    mode: standard          # or "extension"
```

---

## 2. Complete Module Catalog (160+ modules)

### 2.1 Module Properties

Each Seed4J module has these official properties:

| Property | Purpose | Example |
|----------|---------|---------|
| `slug` | Unique identifier | `"gateway"` |
| `apiDoc(category, description)` | Category + description | `"Spring Boot - Spring Cloud", "Add Spring Cloud Gateway"` |
| `organization` | Dependencies (modules or features) | `addDependency(SPRING_BOOT_WEBFLUX_EMPTY)` |
| `tags` | Filtering labels | `"server", "spring", "spring-boot", "cloud"` |
| `propertiesDefinition` | Required CLI parameters | `addBasePackage().addProjectBaseName()` |
| `factory` | Generation logic | `gateway::buildModule` |

### 2.2 Feature Slugs (Dependency Targets)

Seed4J modules can depend on **features** (abstract capabilities) rather than specific modules. Features are satisfied by ANY module in that feature group:

| Feature Slug | Satisfied By |
|-------------|-------------|
| `java-build-tool` | `maven-java` OR `gradle-java` |
| `spring-server` | `spring-boot-mvc-empty` OR `spring-boot-webflux-empty` |
| `spring-mvc-server` | `spring-boot-tomcat` |
| `authentication` | `spring-boot-jwt` OR `spring-boot-oauth2` |
| `springdoc` | `springdoc-mvc-openapi` OR `springdoc-webflux-openapi` |
| `client-core` | `angular-core` OR `vue-core` OR `react-core` OR `svelte-core` |
| `datasource` | ANY `datasource-*` module |
| `jpa-persistence` | ANY `jpa-*` module |
| `database-migration` | `flyway` OR `liquibase` |
| `spring-boot-cucumber` | `spring-boot-cucumber-mvc` OR `spring-boot-cucumber-webflux` |
| `cucumber-authentication` | `spring-boot-cucumber-jwt-authentication` OR `spring-boot-cucumber-oauth2-authentication` |
| `jcache` | `ehcache-java-config` OR `ehcache-xml-config` |

### 2.3 Complete Module List by Official Category

#### Init & Setup
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `init` | None | `projectName`, `baseName`, `nodePackageManager`, `endOfLine`, `indentation`, `springConfigurationFormat` | Project initialization |
| `prettier` | None | `endOfLine`, `indentation` | Code formatting |
| `infinitest-filters` | None | - | Infinitest continuous testing filters |
| `license-apache` | None | - | Apache 2.0 license file |
| `license-mit` | None | - | MIT license file |

#### Build Tools
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `maven-java` | `init` | `packageName` | Maven project with pom.xml |
| `maven-wrapper` | `maven-java` | - | Maven wrapper scripts |
| `gradle-java` | `init` | `packageName` | Gradle project with Kotlin DSL |
| `gradle-wrapper` | `gradle-java` | - | Gradle wrapper scripts |
| `frontend-maven-plugin` | `spring-server`, `spring-mvc-server`, `client-core`, `maven-java` | - | Frontend Maven plugin |
| `frontend-maven-plugin-cache` | `frontend-maven-plugin` | - | Frontend Maven plugin cache |
| `frontend-maven-plugin-merge-coverage` | `cypress-component-tests`, `client-core`, `spring-server`, `spring-mvc-server`, `maven-java` | - | Coverage merge |
| `node-gradle-plugin` | `spring-server`, `spring-mvc-server`, `client-core`, `gradle-java` | - | Node Gradle plugin |

#### Java Core
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `java-base` | `java-build-tool` | - | Java base classes (Assert, etc.) |
| `java-enums` | `java-base` | - | Enum utilities |
| `java-memoizers` | `java-base` | - | Memoization utilities |
| `java-archunit` | `spring-server` | - | ArchUnit architecture tests |
| `checkstyle` | `java-build-tool` | - | Checkstyle code style |
| `modernizer` | `java-build-tool` | - | Modernizer Maven plugin |
| `jacoco` | `java-build-tool` | - | JaCoCo code coverage |
| `jacoco-with-min-coverage-check` | `java-build-tool` | - | JaCoCo with minimum coverage |
| `protobuf` | `java-base` | - | Protocol Buffers |
| `protobuf-backwards-compatibility-check` | `protobuf`, `maven-java` | - | Protobuf compatibility |
| `jmolecules` | `java-build-tool` | - | jMolecules architecture annotations |
| `jqassistant` | `java-build-tool` | - | jQAssistant code analysis |
| `jqassistant-jmolecules` | `jqassistant`, `jmolecules` | - | jQAssistant jMolecules |
| `jqassistant-spring` | `jqassistant`, `spring-boot` | - | jQAssistant Spring |
| `git-information` | `spring-boot-actuator` | - | Git commit info in build |
| `jib` | `java-build-tool` | - | Jib container image building |

#### Spring Boot Core
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `spring-boot` | `java-build-tool`, `java-base` | `serverPort` | Spring Boot core |
| `spring-boot-actuator` | `spring-server` | - | Spring Boot Actuator |
| `spring-boot-async` | `spring-boot` | - | Async support |
| `spring-boot-cache` | `spring-boot` | - | Spring Cache |
| `spring-boot-devtools` | `spring-boot` | - | Spring Boot DevTools |
| `spring-boot-local-profile` | `spring-boot` | - | Local Spring profile |
| `spring-boot-docker-compose` | `maven-java`, `spring-boot` | - | Spring Boot Docker Compose |

#### Spring Boot - Server Stack
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `spring-boot-mvc-empty` | `spring-boot` | - | Spring MVC (empty, no sample) |
| `spring-boot-webflux-empty` | `spring-boot` | - | WebFlux (empty, no sample) |
| `spring-boot-webflux-netty` | `spring-boot-webflux-empty` | - | WebFlux with Netty |
| `spring-boot-tomcat` | `spring-boot-mvc-empty`, `logs-spy` | - | Tomcat server |
| `spring-boot-thymeleaf` | `spring-server` | - | Thymeleaf template engine |

#### Spring Boot - Spring Cloud
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `spring-cloud` | `spring-boot-actuator` | - | Spring Cloud BOM |
| `gateway` | `spring-boot-webflux-empty`, `spring-cloud` | `basePackage`, `baseName` | Spring Cloud Gateway |
| `consul` | `spring-boot-actuator` | - | Consul service discovery |
| `eureka-client` | `spring-cloud` | - | Eureka client |

#### Spring Boot - Database
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `datasource-postgresql` | `spring-boot` | - | PostgreSQL datasource |
| `datasource-mysql` | `spring-boot` | - | MySQL datasource |
| `datasource-mariadb` | `spring-boot` | - | MariaDB datasource |
| `datasource-mssql` | `spring-boot` | - | SQL Server datasource |
| `jpa-postgresql` | `datasource-postgresql` | - | JPA with PostgreSQL |
| `jpa-mysql` | `datasource-mysql` | - | JPA with MySQL |
| `jpa-mariadb` | `datasource-mariadb` | - | JPA with MariaDB |
| `jpa-mssql` | `datasource-mssql` | - | JPA with SQL Server |
| `hibernate-2nd-level-cache` | `jcache`, `jpa-persistence` | - | Hibernate 2nd level cache |
| `jooq-postgresql` | `datasource-postgresql` | - | jOOQ with PostgreSQL |
| `jooq-mysql` | `datasource-mysql` | - | jOOQ with MySQL |
| `jooq-mariadb` | `datasource-mariadb` | - | jOOQ with MariaDB |
| `jooq-mssql` | `datasource-mssql` | - | jOOQ with SQL Server |

#### Spring Boot - NoSQL
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `mongodb` | `spring-boot` | - | MongoDB support |
| `cassandra` | `spring-boot` | - | Cassandra support |
| `neo4j` | `spring-boot` | - | Neo4j graph database |
| `redis` | `spring-boot` | - | Redis support |

#### Spring Boot - Migration
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `liquibase` | `datasource` | - | Liquibase migration |
| `liquibase-async` | `liquibase`, `logs-spy` | - | Liquibase async |
| `liquibase-linter` | `liquibase`, `maven-java` | - | Liquibase linter |
| `flyway` | `datasource` | - | Flyway migration |
| `flyway-postgresql` | `flyway`, `datasource-postgresql` | - | Flyway PostgreSQL |
| `flyway-mysql` | `flyway`, `datasource-mysql` | - | Flyway MySQL |
| `flyway-mariadb` | `flyway`, `datasource-mariadb` | - | Flyway MariaDB |
| `flyway-mssql` | `flyway`, `datasource-mssql` | - | Flyway SQL Server |
| `mongock` | `mongodb` | - | Mongock MongoDB migration |
| `neo4j-migrations` | `neo4j` | - | Neo4j migrations |
| `cassandra-migration` | `cassandra` | - | Cassandra migration |

#### Spring Boot - Cache
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `caffeine-cache` | `spring-boot-cache` | - | Caffeine in-memory cache |
| `ehcache-java-config` | `spring-boot-cache` | - | Ehcache Java config |
| `ehcache-xml-config` | `spring-boot-cache` | - | Ehcache XML config |

#### Spring Boot - Security
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `spring-boot-jwt` | `java-base`, `spring-mvc-server` | - | JWT authentication |
| `spring-boot-jwt-basic-auth` | `spring-boot-jwt`, `springdoc-jwt` | - | JWT basic auth |
| `spring-boot-oauth2` | `java-base`, `spring-mvc-server`, `java-memoizers` | - | OAuth2 authentication |
| `spring-boot-oauth2-account` | `spring-boot-oauth2` | - | OAuth2 Account service |
| `spring-boot-oauth2-auth0` | `spring-boot-oauth2` | - | OAuth2 Auth0 |
| `spring-boot-oauth2-okta` | `spring-boot-oauth2` | - | OAuth2 Okta |
| `kipe-authorization` | `authentication` | - | Kipe authorization |
| `kipe-expression` | `authentication` | - | Kipe expression |

#### Spring Boot - Messaging
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `spring-boot-kafka` | `spring-boot` | - | Apache Kafka |
| `spring-boot-kafka-akhq` | `spring-boot-kafka` | - | Kafka AHQ UI |
| `spring-boot-kafka-sample-producer-consumer` | `spring-boot-kafka` | - | Kafka sample |
| `spring-boot-pulsar` | `spring-boot` | - | Apache Pulsar |

#### Spring Boot - Testing
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `logs-spy` | `spring-boot` | - | LogSpy test utility |
| `spring-boot-cucumber-mvc` | `spring-mvc-server` | - | Cucumber BDD MVC |
| `spring-boot-cucumber-webflux` | `spring-boot-webflux-netty` | - | Cucumber BDD WebFlux |
| `spring-boot-cucumber-jpa-reset` | `spring-boot-cucumber`, `jpa-persistence` | - | Cucumber JPA reset |
| `spring-boot-cucumber-jwt-authentication` | `spring-boot-cucumber`, `spring-boot-jwt` | - | Cucumber JWT auth |
| `spring-boot-cucumber-oauth2-authentication` | `spring-boot-cucumber`, `spring-boot-oauth2` | - | Cucumber OAuth2 auth |
| `approval-tests` | `java-build-tool` | - | ApprovalTests |
| `jqwik` | `java-build-tool` | - | jqwik property testing |
| `arch-unit-ts` | `client-core` | - | ArchUnit TypeScript |

#### SpringDoc (OpenAPI)
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `springdoc-mvc-openapi` | `spring-mvc-server` | - | SpringDoc MVC OpenAPI |
| `springdoc-webflux-openapi` | `spring-boot-webflux-netty` | - | SpringDoc WebFlux OpenAPI |
| `springdoc-jwt` | `springdoc`, `spring-boot-jwt` | - | SpringDoc JWT |
| `springdoc-oauth2` | `springdoc`, `spring-boot-oauth2` | - | SpringDoc OAuth2 |
| `springdoc-oauth2-auth0` | `springdoc`, `spring-boot-oauth2-auth0` | - | SpringDoc Auth0 |
| `springdoc-oauth2-okta` | `springdoc`, `spring-boot-oauth2-okta` | - | SpringDoc Okta |
| `openapi-contract` | `spring-mvc-server`, `maven-java` | - | OpenAPI contract |
| `openapi-backwards-compatibility-check` | `openapi-contract` | - | OpenAPI compatibility |

#### Spring Boot - AI
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `langchain4j` | `spring-boot` | - | LangChain4j AI |
| `spring-boot-langchain4j-sample` | `spring-mvc-server`, `langchain4j` | - | LangChain4j sample |

#### Spring Boot - Other
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `logstash` | `spring-boot` | - | Logstash JSON logging |
| `internationalized-errors` | `java-enums`, `spring-boot-mvc-empty` | - | i18n error messages |
| `pagination-domain` | `java-base` | - | Pagination domain |
| `jpa-pagination` | `pagination-domain`, `jpa-persistence` | - | JPA pagination |
| `rest-pagination` | `pagination-domain`, `springdoc` | - | REST pagination |

#### Frontend
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `typescript` | `init`, `prettier` | - | TypeScript base |
| `optional-typescript` | `typescript` | - | Optional type |
| `ts-loader` | `client-core` | - | TypeScript loader |
| `ts-pagination-domain` | `client-core` | - | TS pagination domain |
| `ts-rest-pagination` | `ts-pagination-domain` | - | TS REST pagination |
| `angular-core` | `init`, `prettier` | - | Angular core |
| `angular-health` | `angular-core`, `spring-boot-actuator` | - | Angular health |
| `angular-jwt` | `angular-core` | - | Angular JWT |
| `angular-oauth2-keycloak` | `angular-core` | - | Angular OAuth2 Keycloak |
| `angular-i18n` | `angular-core` | - | Angular i18n |
| `angular-tailwind` | `angular-core` | - | Angular Tailwind |
| `vue-core` | `typescript`, `prettier` | - | Vue core |
| `vue-router` | `vue-core` | - | Vue Router |
| `vue-pinia` | `vue-core` | - | Vue Pinia |
| `vue-jwt` | `vue-core` | - | Vue JWT |
| `vue-oauth2-keycloak` | `vue-core` | - | Vue OAuth2 Keycloak |
| `vue-i18next` | `vue-core` | - | Vue i18next |
| `react-core` | `typescript`, `prettier` | - | React core |
| `react-i18next` | `react-core` | - | React i18next |
| `react-jwt` | `react-core` | - | React JWT |
| `svelte-core` | `init`, `prettier` | - | Svelte core |

#### Frontend Testing
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `cypress-e2e` | `client-core` | - | Cypress E2E |
| `cypress-component-tests` | `client-core` | - | Cypress component |
| `cypress-merge-coverage` | `cypress-component-tests` | - | Cypress coverage |
| `playwright-e2e` | `client-core` | - | Playwright E2E |
| `playwright-component-tests` | `client-core` | - | Playwright component |

#### Frontend UI
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `tikui` | `client-core` | - | Tikui design system |
| `thymeleaf-template` | `spring-boot-thymeleaf` | - | Thymeleaf templates |
| `thymeleaf-template-htmx-webjars` | `htmx-webjars`, `thymeleaf-template` | - | Thymeleaf + HTMX |
| `thymeleaf-template-alpinejs-webjars` | `alpinejs-webjars`, `thymeleaf-template` | - | Thymeleaf + Alpine.js |
| `thymeleaf-template-tailwindcss` | `thymeleaf-template` | - | Thymeleaf + Tailwind |
| `webjars-locator` | `spring-boot-thymeleaf` | - | WebJars locator |
| `htmx-webjars` | `webjars-locator` | - | HTMX WebJars |
| `alpinejs-webjars` | `webjars-locator` | - | Alpine.js WebJars |

#### CI/CD
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `github-actions-maven` | `java-build-tool` | - | GitHub Actions Maven |
| `github-actions-gradle` | `java-build-tool` | - | GitHub Actions Gradle |
| `gitlab-ci-maven` | `java-build-tool` | - | GitLab CI Maven |
| `gitlab-ci-gradle` | `java-build-tool` | - | GitLab CI Gradle |
| `renovate` | `java-build-tool` | - | Renovate dependency updates |
| `sonarqube-java-backend` | `java-build-tool`, `code-coverage-java` | - | SonarQube Java |
| `sonarqube-java-backend-and-frontend` | `java-build-tool`, `code-coverage-java` | - | SonarQube full stack |
| `sonarqube-typescript` | `typescript` | - | SonarQube TypeScript |

#### Dev Environment
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `github-codespaces` | None | - | GitHub Codespaces |
| `gitpod` | None | - | Gitpod config |
| `dockerfile-maven` | `maven-wrapper` | - | Dockerfile Maven |
| `dockerfile-gradle` | `gradle-wrapper` | - | Dockerfile Gradle |

#### Documentation
| Module | Dependencies | Parameters | Description |
|--------|-------------|------------|-------------|
| `application-service-hexagonal-architecture-documentation` | None | - | Hexagonal architecture docs |
| `front-hexagonal-architecture` | None | - | Frontend hexagonal architecture docs |

#### Samples (Rank D - Demo Only)
| Module | Dependencies | Description |
|--------|-------------|-------------|
| `sample-feature` | `cucumber-authentication`, `springdoc`, `java-base`, `kipe-expression`, `kipe-authorization` | Sample feature |
| `sample-jpa-persistence` | `sample-schema`, `spring-boot-cucumber-jpa-reset` | Sample JPA |
| `sample-cassandra-persistence` | `sample-feature`, `cassandra-migration` | Sample Cassandra |
| `sample-mongodb-persistence` | `sample-feature`, `mongock` | Sample MongoDB |
| `sample-postgresql-flyway-changelog` | `flyway-postgresql`, `sample-feature`, `jpa-postgresql` | Sample Flyway PG |
| `sample-not-postgresql-flyway-changelog` | `flyway`, `sample-feature` | Sample Flyway non-PG |
| `sample-liquibase-changelog` | `liquibase`, `sample-feature` | Sample Liquibase |

#### Extension
| Module | Dependencies | Description |
|--------|-------------|-------------|
| `seed4j-extension` | `spring-boot` | Seed4J extension support |

---

## 3. Module Dependency Graph (Topological Order)

Based on actual source code analysis of all `*ModuleConfiguration.java` files:

```
Tier 0:  init
Tier 1:  prettier
Tier 2:  maven-java, gradle-java, java-base, checkstyle, jacoco, java-enums, java-memoizers
Tier 3:  spring-boot
Tier 4:  spring-boot-mvc-empty, spring-boot-webflux-empty, spring-boot-actuator, logs-spy, spring-boot-cache
Tier 5:  spring-boot-tomcat, spring-boot-webflux-netty, java-archunit, springdoc-mvc-openapi
Tier 6:  datasource-postgresql, jpa-postgresql, spring-boot-kafka
Tier 7:  flyway, flyway-postgresql
Tier 8:  spring-cloud, gateway, springdoc-webflux-openapi
Tier 9:  sonarqube-*, github-actions-*, renovate, dockerfile-*
```

### Critical Dependency Rules

| Rule | Explanation |
|------|-------------|
| `gateway` requires `spring-boot-webflux-empty` + `spring-cloud` | Gateway is WebFlux-only, NOT Tomcat |
| `spring-boot-tomcat` requires `spring-boot-mvc-empty` + `logs-spy` | Tomcat is MVC-only |
| `spring-boot-webflux-openapi` requires `spring-boot-webflux-netty` | WebFlux OpenAPI needs Netty |
| `flyway-*` requires `flyway` + matching `datasource-*` | Database-specific Flyway needs both |
| `jpa-*` requires matching `datasource-*` | JPA needs its datasource |
| `java-archunit` requires `spring-server` | Needs either MVC or WebFlux |
| `springdoc-mvc-openapi` requires `spring-mvc-server` | MVC OpenAPI needs Tomcat |

---

## 4. Monorepo Multi-Microservices Strategy

### 4.1 Core Principle

**Seed4J is a single-project generator.** Each `--project-path` = one deployable application with its own `.seed4j/` directory.

### 4.2 Monorepo Architecture

```
agentbanking-seed4j/
├── settings.gradle.kts                    # Root Gradle (MANUAL)
├── build.gradle.kts                       # Root build (MANUAL - lightweight)
├── gradle/libs.versions.toml              # Centralized versions (MANUAL)
├── docker-compose.yml                     # Infrastructure (MANUAL)
│
├── api-gateway-service/                   # Seed4J project #1
│   ├── .seed4j/modules/history.json       # Module history
│   ├── build.gradle.kts                   # Generated
│   └── src/main/java/com/agentbanking/gateway/
│
├── rules-service/                         # Seed4J project #2
│   ├── .seed4j/modules/history.json
│   ├── build.gradle.kts
│   └── src/main/java/com/agentbanking/rules/
│
├── ledger-service/                        # Seed4J project #3
├── onboarding-service/                    # Seed4J project #4
├── switch-adapter-service/                # Seed4J project #5
├── biller-service/                        # Seed4J project #6
│
└── common/                                # Shared library (MANUAL)
    ├── build.gradle.kts
    └── src/main/java/com/agentbanking/common/
```

### 4.3 Module Application Per Service Type

**API Gateway (WebFlux stack):**
```bash
seed4j apply init --project-path ./api-gateway-service \
  --project-name "API Gateway" --base-name ApiGateway --package-name com.agentbanking.gateway

seed4j apply gradle-java --project-path ./api-gateway-service --package-name com.agentbanking.gateway
seed4j apply gradle-wrapper --project-path ./api-gateway-service
seed4j apply java-base --project-path ./api-gateway-service
seed4j apply prettier --project-path ./api-gateway-service
seed4j apply spring-boot --project-path ./api-gateway-service --server-port 8080
seed4j apply spring-boot-webflux-empty --project-path ./api-gateway-service
seed4j apply spring-cloud --project-path ./api-gateway-service
seed4j apply gateway --project-path ./api-gateway-service
seed4j apply spring-boot-actuator --project-path ./api-gateway-service
seed4j apply java-archunit --project-path ./api-gateway-service
seed4j apply logs-spy --project-path ./api-gateway-service
seed4j apply springdoc-webflux-openapi --project-path ./api-gateway-service
```

**Domain Service (Tomcat stack):**
```bash
seed4j apply init --project-path ./rules-service \
  --project-name "Rules Service" --base-name AgentBankingRules --package-name com.agentbanking.rules

seed4j apply gradle-java --project-path ./rules-service --package-name com.agentbanking.rules
seed4j apply gradle-wrapper --project-path ./rules-service
seed4j apply java-base --project-path ./rules-service
seed4j apply prettier --project-path ./rules-service
seed4j apply java-memoizers --project-path ./rules-service
seed4j apply java-enums --project-path ./rules-service
seed4j apply spring-boot --project-path ./rules-service --server-port 8081
seed4j apply spring-boot-tomcat --project-path ./rules-service
seed4j apply spring-boot-actuator --project-path ./rules-service
seed4j apply application-service-hexagonal-architecture-documentation --project-path ./rules-service
seed4j apply java-archunit --project-path ./rules-service
seed4j apply datasource-postgresql --project-path ./rules-service
seed4j apply jpa-postgresql --project-path ./rules-service
seed4j apply flyway --project-path ./rules-service
seed4j apply flyway-postgresql --project-path ./rules-service
seed4j apply spring-boot-kafka --project-path ./rules-service
seed4j apply springdoc-mvc-openapi --project-path ./rules-service
seed4j apply logs-spy --project-path ./rules-service
```

### 4.4 What Goes Where

| Component | Tool | Why |
|-----------|------|-----|
| Root `settings.gradle.kts` | Manual | Seed4J doesn't generate multi-module Gradle |
| Root `build.gradle.kts` | Manual | Lightweight parent only |
| Each service | Seed4J | Full scaffolding with `--project-path` |
| Common module | Manual | Simple Java library, no Spring Boot |
| Docker Compose | Manual | Infrastructure orchestration |
| Documentation | Manual | Project docs |

### 4.5 What Seed4J Does NOT Support

| Feature | Status | Workaround |
|---------|--------|------------|
| Monorepo awareness | Not supported | Use `--project-path` per service |
| Multi-module Gradle generation | Not supported | Manual `settings.gradle.kts` |
| Shared library generation | Not supported | Manual `common/` module |
| Cross-service dependencies | Not supported | Manual dependency management |
| Parent POM generation | Not supported | Manual parent `pom.xml` |
| Bulk module application | Not supported | Apply modules one at a time |
| Module templates | Not supported | Each module has fixed generation |

---

## 5. Best Practices

### 5.1 Module Application Order

1. **Foundation first:** `init` → `prettier` → `gradle-java` → `java-base`
2. **Server stack:** `spring-boot` → `spring-boot-tomcat` OR `spring-boot-webflux-empty`
3. **Infrastructure:** `datasource-*` → `jpa-*` → `flyway-*` → `spring-boot-kafka`
4. **Cross-cutting:** `spring-boot-actuator` → `java-archunit` → `logs-spy` → `springdoc-*`

### 5.2 Parameter Management

- Always specify `--project-path` explicitly in monorepo
- Parameters are reused within the same project path
- Use `--no-commit` during scaffolding, commit manually after verification
- Set `--server-port` on `spring-boot` module, not later

### 5.3 Server Stack Exclusivity

| Stack | Modules | Use For |
|-------|---------|---------|
| **MVC (Tomcat)** | `spring-boot-mvc-empty` → `spring-boot-tomcat` | Domain services |
| **WebFlux** | `spring-boot-webflux-empty` → `gateway` | API Gateway |
| **NEVER MIX** | Tomcat + Gateway in same project | Will cause conflicts |

### 5.4 OpenAPI Module Selection

| Stack | OpenAPI Module |
|-------|---------------|
| MVC (Tomcat) | `springdoc-mvc-openapi` |
| WebFlux | `springdoc-webflux-openapi` |

### 5.5 Database Module Selection

| Database | Datasource Module | JPA Module | Flyway Module |
|----------|------------------|------------|---------------|
| PostgreSQL | `datasource-postgresql` | `jpa-postgresql` | `flyway-postgresql` |
| MySQL | `datasource-mysql` | `jpa-mysql` | `flyway-mysql` |
| MariaDB | `datasource-mariadb` | `jpa-mariadb` | `flyway-mariadb` |
| SQL Server | `datasource-mssql` | `jpa-mssql` | `flyway-mssql` |

---

## 6. Seed4J CLI Installation

### 6.1 Build from Source

```bash
cd /tmp
git clone https://github.com/seed4j/seed4j-cli
cd seed4j-cli
./mvnw clean package

# Install to system path
echo "java -jar \"/usr/local/bin/seed4j.jar\" \"\$@\"" | sudo tee /usr/local/bin/seed4j > /dev/null
sudo chmod +x /usr/local/bin/seed4j
JAR_SOURCE=$(ls target/seed4j-cli-*.jar | head -n 1)
sudo mv "$JAR_SOURCE" /usr/local/bin/seed4j.jar
```

### 6.2 Verify Installation

```bash
seed4j --version
# Expected: Seed4J CLI v0.0.1-SNAPSHOT, Seed4J version: 2.2.0

seed4j list
# Expected: List of 160+ modules
```

### 6.3 Using Local Build (Alternative)

If not installed to system path:
```bash
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply init --project-path ./my-service ...
```
