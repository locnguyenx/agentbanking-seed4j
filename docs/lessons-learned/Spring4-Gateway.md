## Goal
Fix disabled integration tests in a Spring Boot 4 project by establishing a working Spring Boot/Spring Cloud version combination that supports full 4-layer test strategy (Unit, Component, Integration, E2E).

## Instructions
- Maintain Spring Boot 4.0.x compatibility as requested
- Use Spring Cloud version that supports Spring Boot 4.0.x (identified as 2025.1.x/Oakwood)
- Ensure all required Spring Cloud Stream and Spring Data Redis dependencies are included
- Fix build errors related to missing packages
- Once build succeeds, implement and run the 4-layer test strategy
- Don't skip problematic tests - refactor them using current Spring version
- E2E tests must use real services and complete HTTP request/response flow
- Use WebMVC (NOT WebFlux) for Spring Cloud Gateway with Virtual Threads for scalability

## Discoveries
1. Version Incompatibility: Spring Cloud 2024.0.1 is designed for Spring Boot 3.4.x, not 4.x, causing ClassNotFoundException for WebMvcAutoConfiguration and other classes that moved packages in Spring Boot 4.
2. Spring Cloud 2025.1 Compatibility: According to Spring Cloud documentation, release train 2025.1.x (Oakwood) supports Spring Boot 4.0.x, making it the correct choice for Spring Boot 4.0.1.
3. Missing Dependencies: When using Spring Boot 4.0.1 with Spring Cloud 2025.1.x, specific module dependencies are required:
   - Spring Cloud Stream: spring-cloud-starter-stream-kafka (for StreamBridge)
   - Spring Data Redis: spring-boot-starter-data-redis (for RedisTemplate, RedisConnectionFactory, etc.)
   - Spring Cloud Gateway: spring-cloud-starter-gateway-server-webmvc (NOT the deprecated spring-cloud-starter-gateway)
4. Gateway Artifact Changes: In Spring Cloud Gateway 4.3.x+:
   - Old (deprecated): spring-cloud-starter-gateway 
   - New WebMVC: spring-cloud-starter-gateway-server-webmvc (version 5.0.1 for latest)
   - New WebFlux: spring-cloud-starter-gateway-server-webflux
   - Property prefix changed: spring.cloud.gateway.mvc.* → spring.cloud.gateway.server.webmvc.*
5. Virtual Threads Revolution: With Java 21+ and Spring Boot 4, Virtual Threads provide WebFlux-level scalability WITHOUT requiring reactive stack. This eliminates the need to convert to WebFlux/R2DBC. Configuration: spring.threads.virtual.enabled=true
6. BOM Version vs Artifact Version Mismatch: Spring Cloud BOM version (2025.1.0) does NOT equal artifact versions. BOM maps to different versions (e.g., gateway 5.0.0 in BOM but only 4.3.4/5.0.1 actually exists in Maven).
7. Integration Test Issues: Pre-existing testcontainers/Hibernate schema validation issues unrelated to Gateway configuration.
Accomplished
- ✅ Identified root cause: Spring Cloud versions incompatible with Spring Boot 4
- ✅ Researched and confirmed Spring Cloud 2025.1.x supports Spring Boot 4.0.x
- ✅ Updated Gateway to WebMVC variant: spring-cloud-starter-gateway-server-webmvc:5.0.1
- ✅ Enabled Virtual Threads in application.yml for scalability
- ✅ Removed custom Java GatewayConfig routes (commented out) - using YAML only
- ✅ Build compiles successfully with Spring Boot 4.0.1 + Spring Cloud 2025.1.0
- ✅ Unit tests pass (602 tests)
- ✅ Updated AGENTS.md with latest technology stack and enforcement rules

## Relevant files / directories
Configuration files modified:
- D:\Working\myprojects\agentbanking-seed4j\gradle\libs.versions.toml - Version catalog managing Spring Boot, Spring Cloud, and other library versions
- D:\Working\myprojects\agentbanking-seed4j\build.gradle.kts - Main build script with Spring Cloud BOM import
Source files modified:
- D:\Working\myprojects\agentbanking-seed4j\src\main\java\com\agentbanking\gateway\config\GatewayConfig.java - Removed custom routes (commented out)
- D:\Working\myprojects\agentbanking-seed4j\src\main\resources\config\application.yml - Added Virtual Threads configuration
- D:\Working\myprojects\agentbanking-seed4j\src\test\resources\config\application-test.yml - Added datasource configurations