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

#### Complete Module Catalog (168 modules)

| Module | Dependencies | Description |
|--------|-------------|-------------|
| init | - | Init project |
| prettier | module:init | Format project with prettier |
| infinitest-filters | - | Add filter for infinitest, the continuous test runner |
| license-apache | - | Add APACHE license file |
| license-mit | - | Add MIT license file |
| maven-java | module:init | Init Maven project with pom.xml |
| maven-wrapper | module:maven-java | Add maven wrapper |
| gradle-java | module:init | Init Gradle project with kotlin DSL |
| gradle-wrapper | module:gradle-java | Add gradle wrapper |
| frontend-maven-plugin | feature:spring-server, feature:spring-mvc-server, feature:client-core, module:maven-java | Add Frontend Maven Plugin |
| frontend-maven-plugin-cache | module:frontend-maven-plugin | Add cache - by computing resources checksum - to avoid rebuilding frontend on successive maven builds |
| frontend-maven-plugin-merge-coverage | module:cypress-component-tests, feature:client-core, feature:spring-server, feature:spring-mvc-server, module:maven-java | Merge Cypress and vitest code coverage |
| node-gradle-plugin | feature:spring-server, feature:spring-mvc-server, feature:client-core, module:gradle-java | Add node-gradle plugin for building frontend with Gradle |
| java-base | feature:java-build-tool | Add Base classes and Error domain to project |
| java-enums | module:java-base | Add simple enums mapper |
| java-memoizers | module:java-base | Add simple memoizers factory |
| java-archunit | feature:spring-server | Add Hexagonal Arch Unit Tests to project |
| checkstyle | feature:java-build-tool | Add Checkstyle configuration to enforce code style rules |
| modernizer | feature:java-build-tool | Add Modernizer build plugin for detecting uses of legacy APIs which modern Java versions supersede. These modern APIs are often more performant, safer, and idiomatic than the legacy equivalents. |
| jacoco | feature:java-build-tool | Add JaCoCo for code coverage reporting |
| jacoco-with-min-coverage-check | feature:java-build-tool | Add JaCoCo for code coverage reporting and 100% coverage check |
| protobuf | module:java-base | Add protobuf support |
| protobuf-backwards-compatibility-check | module:protobuf, module:maven-java | Add protobuf backwards compatibility check |
| jmolecules | feature:java-build-tool | Add support for jMolecules documentation annotations based on DDD patterns such as @BoundedContext, @ValueObject, @Entity, @AggregateRoot... |
| jqassistant | feature:java-build-tool | Setup jQAssistant for documentation and analysis of the project |
| jqassistant-jmolecules | module:jqassistant, module:jmolecules | Add jMolecules support for jQAssistant |
| jqassistant-spring | module:jqassistant, module:spring-boot | Add Spring support for jQAssistant |
| git-information | module:spring-boot-actuator | Injecting Git Information into Spring |
| jib | feature:java-build-tool | Add Docker image building with Jib |
| spring-boot | feature:java-build-tool, module:java-base | Init Spring Boot project with dependencies, App, and properties |
| spring-boot-actuator | feature:spring-server | Add Spring Boot Actuator to the project |
| spring-boot-async | module:spring-boot | Add asynchronous execution and scheduling configuration |
| spring-boot-cache | module:spring-boot | Add simple cache |
| spring-boot-devtools | module:spring-boot | Add Spring Boot devtools. |
| spring-boot-local-profile | module:spring-boot | Use Spring local profile by default for development. |
| spring-boot-docker-compose | module:maven-java, module:spring-boot | Configure Spring Boot and docker compose integration, to make local development easier |
| spring-boot-mvc-empty | module:spring-boot | Empty module: do not use alone. You should add another module in Spring MVC Server |
| spring-boot-webflux-empty | module:spring-boot | Empty module: do not use alone. You should add module Spring Boot Webflux Netty |
| spring-boot-webflux-netty | module:spring-boot-webflux-empty | Add Spring Boot Webflux Netty |
| spring-boot-tomcat | module:spring-boot-mvc-empty, module:logs-spy | Add Spring Boot MVC with Tomcat |
| spring-boot-thymeleaf | feature:spring-server | Add Spring Boot Thymeleaf to the project |
| spring-cloud | module:spring-boot-actuator | Add Spring Cloud Config Client |
| gateway | module:spring-boot-webflux-empty, module:spring-cloud | Add Spring Cloud Gateway |
| consul | module:spring-boot-actuator | Add Spring Cloud Consul config and discovery |
| eureka-client | module:spring-cloud | Add Spring Cloud Eureka Client |
| datasource-postgresql | module:spring-boot | Add PostgreSQL datasource to Spring project |
| datasource-mysql | module:spring-boot | Add MySQL datasource to Spring project |
| datasource-mariadb | module:spring-boot | Add MariaDB datasource to Spring project |
| datasource-mssql | module:spring-boot | Add MsSQL datasource to Spring project |
| jpa-postgresql | module:datasource-postgresql | Add JPA with PostgreSQL to project |
| jpa-mysql | module:datasource-mysql | Add JPA with MySQL to project |
| jpa-mariadb | module:datasource-mariadb | Add JPA with MariaDB to project |
| jpa-mssql | module:datasource-mssql | Add JPA with MsSQL to project |
| hibernate-2nd-level-cache | feature:jcache, feature:jpa-persistence | Add Hibernate second level cache configuration to project |
| jooq-postgresql | module:datasource-postgresql | Add Jooq with PostgreSQL to project |
| jooq-mysql | module:datasource-mysql | Add Jooq with MySQL to project |
| jooq-mariadb | module:datasource-mariadb | Add Jooq with MariaDB to project |
| jooq-mssql | module:datasource-mssql | Add Jooq with MsSQL to project |
| mongodb | module:spring-boot | Add MongoDB drivers and dependencies, with testcontainers |
| cassandra | module:spring-boot | Add Cassandra drivers and dependencies |
| neo4j | module:spring-boot | Add Neo4j drivers and dependencies, with testcontainers |
| redis | module:spring-boot | Add Redis drivers and dependencies, with testcontainers |
| liquibase | feature:datasource | Add Liquibase |
| liquibase-async | module:liquibase, module:logs-spy | Support updating the database asynchronously with Liquibase |
| liquibase-linter | module:liquibase, module:maven-java | Configure a linter for the Liquibase migration scripts |
| flyway | feature:datasource | Add Flyway |
| flyway-mariadb | module:flyway, module:datasource-mariadb | Add Flyway MariaDB |
| flyway-mssql | module:flyway, module:datasource-mssql | Add Flyway PostgreSQL |
| flyway-mysql | module:flyway, module:datasource-mysql | Add Flyway MySQL |
| flyway-postgresql | module:flyway, module:datasource-postgresql | Add Flyway PostgreSQL |
| mongock | module:mongodb | Add Mongock |
| neo4j-migrations | module:neo4j | Add neo4j migrations |
| cassandra-migration | module:cassandra | Add Cassandra Migration tools |
| caffeine-cache | module:spring-boot-cache | Add caffeine cache |
| ehcache-java-config | module:spring-boot-cache | Add Ehcache with Java configuration |
| ehcache-xml-config | module:spring-boot-cache | Add Ehcache with XML configuration |
| spring-boot-jwt | module:java-base, feature:spring-mvc-server | Add Spring Security JWT |
| spring-boot-jwt-basic-auth | module:spring-boot-jwt, module:springdoc-jwt | Add Basic Auth for Spring Security JWT |
| spring-boot-oauth2 | module:java-base, feature:spring-mvc-server, module:java-memoizers | Add a Spring Security: OAuth 2.0 / OIDC Authentication (stateful, works with Keycloak and Okta) |
| spring-boot-oauth2-account | module:spring-boot-oauth2 | Add a account context for OAuth 2.0 / OIDC Authentication |
| spring-boot-oauth2-auth0 | module:spring-boot-oauth2 | Add a Spring Security: OAuth 2.0 / OIDC Authentication / Auth0 Provider (stateful, works with Keycloak and Auth0) |
| spring-boot-oauth2-okta | module:spring-boot-oauth2 | Add a Spring Security: OAuth 2.0 / OIDC Authentication / Okta Provider (stateful, works with Keycloak and Okta) |
| kipe-authorization | feature:authentication | Ease authorization matrices definition |
| kipe-expression | feature:authentication | Create a new security expression for spring security: can('action', #element) |
| spring-boot-kafka | module:spring-boot | Add Kafka dependencies, with testcontainers |
| spring-boot-kafka-akhq | module:spring-boot-kafka | Add AKHQ |
| spring-boot-kafka-sample-producer-consumer | module:spring-boot-kafka | Add sample Kafka producer and consumer |
| spring-boot-pulsar | module:spring-boot | Add Pulsar dependencies, with testcontainers |
| logs-spy | module:spring-boot | Add LogsSpy JUnit5 extension to project |
| spring-boot-cucumber-mvc | feature:spring-mvc-server | Add Cucumber integration for Spring MVC to project |
| spring-boot-cucumber-webflux | module:spring-boot-webflux-netty | Add Cucumber integration for Webflux to project |
| spring-boot-cucumber-jpa-reset | feature:spring-boot-cucumber, feature:jpa-persistence | Add jpa reset for cucumber |
| spring-boot-cucumber-jwt-authentication | feature:spring-boot-cucumber, module:spring-boot-jwt | Add JWT authentication steps for cucumber |
| spring-boot-cucumber-oauth2-authentication | feature:spring-boot-cucumber, module:spring-boot-oauth2 | Add OAuth2 authentication steps for cucumber |
| approval-tests | feature:java-build-tool | Add ApprovalTests library for Approval testing |
| jqwik | feature:java-build-tool | Add jqwik library for Property Based Testing |
| arch-unit-ts | feature:client-core | Add Arch unit ts |
| springdoc-mvc-openapi | feature:spring-mvc-server | Add springdoc-openapi for spring MVC |
| springdoc-webflux-openapi | module:spring-boot-webflux-netty | Add springdoc-openapi for webflux |
| springdoc-jwt | feature:springdoc, module:spring-boot-jwt | Add JWT authentication for springdoc |
| springdoc-oauth2 | feature:springdoc, module:spring-boot-oauth2 | Add OAuth2 authentication for springdoc |
| springdoc-oauth2-auth0 | feature:springdoc, module:spring-boot-oauth2-auth0 | Add Auth0 authentication for springdoc |
| springdoc-oauth2-okta | feature:springdoc, module:spring-boot-oauth2-okta | Add Okta authentication for springdoc |
| openapi-contract | feature:spring-mvc-server, module:maven-java | Generates OpenAPI contract at build time using openapi-maven-plugin |
| openapi-backwards-compatibility-check | module:openapi-contract | Check backwards incompatible changes to OpenAPI contract during build |
| langchain4j | module:spring-boot | Add LangChain4j |
| spring-boot-langchain4j-sample | feature:spring-mvc-server, module:langchain4j | Add LangChain4j sample |
| logstash | module:spring-boot | Add Logstash TCP appender |
| internationalized-errors | module:java-enums, module:spring-boot-mvc-empty | Add internationalization for application errors |
| pagination-domain | module:java-base | Add domain model for pagination management |
| jpa-pagination | module:pagination-domain, feature:jpa-persistence | Add utility class for JPA pagination |
| rest-pagination | module:pagination-domain, feature:springdoc | Add rest models for pagination handling |
| typescript | module:init, module:prettier | Init Typescript project |
| optional-typescript | module:typescript | Add Optional class domain to project |
| ts-loader | feature:client-core | Helper class to represent loading states |
| ts-pagination-domain | feature:client-core | Add webapp domain for pagination |
| ts-rest-pagination | module:ts-pagination-domain | Add rest pagination to the frontend webapp |
| angular-core | module:init, module:prettier | Add Angular + Angular CLI |
| angular-health | module:angular-core, module:spring-boot-actuator | Angular Health |
| angular-i18n | module:angular-core | Add Angular internationalization |
| angular-jwt | module:angular-core | Add Angular with authentication JWT |
| angular-oauth2-keycloak | module:angular-core | Add OAuth2 authentication |
| angular-tailwind | module:angular-core | Add Tailwind CSS to an Angular project |
| vue-core | module:typescript, module:prettier | Add Vue+Vite |
| vue-router | module:vue-core | Add Vue Router |
| vue-pinia | module:vue-core | Add pinia for state management |
| vue-jwt | module:vue-core | Add JWT authentication to Vue |
| vue-oauth2-keycloak | module:vue-core | Add OAuth2 Keycloak authentication to Vue |
| vue-i18next | module:vue-core | Add vue internationalization |
| react-core | module:typescript, module:prettier | Add React+Vite with minimal CSS |
| react-i18next | module:react-core | Add react internationalization |
| react-jwt | module:react-core | Add JWT Login React |
| svelte-core | module:init, module:prettier | Add Svelte |
| cypress-e2e | feature:client-core | Setup E2E tests using Cypress |
| cypress-component-tests | feature:client-core | Setup frontend component tests using Cypress |
| cypress-merge-coverage | module:cypress-component-tests | Merge coverage from unit test vitest and component test cypress. Not working with Angular |
| playwright-e2e | feature:client-core | Configure E2E tests using Playwright |
| playwright-component-tests | feature:client-core | Configure frontend component tests using Playwright |
| tikui | feature:client-core | Add Tikui, a pattern library to build your styles |
| thymeleaf-template | module:spring-boot-thymeleaf | Add thymeleaf skeleton layout files to the project |
| thymeleaf-template-htmx-webjars | module:htmx-webjars, module:thymeleaf-template | Add htmx webjars scripts to thymeleaf layout |
| thymeleaf-template-alpinejs-webjars | module:alpinejs-webjars, module:thymeleaf-template | Add alpine webjars scripts to thymeleaf layout |
| thymeleaf-template-tailwindcss | module:thymeleaf-template | Add tailwindcss to the thymeleaf template |
| webjars-locator | module:spring-boot-thymeleaf | Add webjars locator to the project |
| htmx-webjars | module:webjars-locator | Add HTMX webjar to the project |
| alpinejs-webjars | module:webjars-locator | Add alpine.js webjar to the project |
| github-actions-maven | module:maven-java | Add GitHub Actions for Maven Build |
| github-actions-gradle | module:gradle-wrapper | Add GitHub Actions for Gradle Build |
| gitlab-ci-maven | module:maven-java | Add GitLab CI for Maven Build |
| gitlab-ci-gradle | module:gradle-java | Add GitLab CI for Gradle Build |
| renovate | - | Add Renovate for automatic dependency updates |
| sonarqube-java-backend | feature:java-build-tool, feature:code-coverage-java | Add Sonar configuration for Java Backend to inspect code quality |
| sonarqube-java-backend-and-frontend | feature:java-build-tool, feature:code-coverage-java | Add Sonar configuration for Java Backend and Frontend to inspect code quality |
| sonarqube-typescript | module:typescript | Add Sonar to project |
| github-codespaces | - | Init GitHub Codespaces configuration files |
| gitpod | - | Init Gitpod configuration files |
| dockerfile-maven | module:maven-wrapper | Add Dockerfile with maven commands |
| dockerfile-gradle | module:gradle-wrapper | Add Dockerfile with gradle commands |
| application-service-hexagonal-architecture-documentation | - | Add documentation for hexagonal architecture |
| front-hexagonal-architecture | - | Add front hexagonal architecture documentation |
| sample-feature | feature:cucumber-authentication, feature:springdoc, module:java-base, module:kipe-expression, module:kipe-authorization | Add sample context with some APIs |
| sample-jpa-persistence | feature:sample-schema, module:spring-boot-cucumber-jpa-reset | Add JPA persistence for sample feature |
| sample-cassandra-persistence | module:sample-feature, module:cassandra-migration | Add Cassandra persistence for sample feature |
| sample-mongodb-persistence | module:sample-feature, module:mongock | Add MongoDB persistence for sample feature |
| sample-postgresql-flyway-changelog | module:flyway-postgresql, module:sample-feature, module:jpa-postgresql | Add PostgreSQL flyway changelog for sample feature |
| sample-not-postgresql-flyway-changelog | module:flyway, module:sample-feature | Add not PostgreSQL flyway changelog for sample feature |
| sample-liquibase-changelog | module:liquibase, module:sample-feature | Add liquibase changelog for sample feature |
| seed4j-extension | module:spring-boot | Create a Seed4J extension to build custom modules |

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
