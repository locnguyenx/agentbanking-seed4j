// seed4j-needle-gradle-imports

plugins {
  java
  alias(libs.plugins.spring.boot)
  // seed4j-needle-gradle-plugins
}

// seed4j-needle-gradle-properties

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

defaultTasks("bootRun")

springBoot {
  mainClass = "com.agentbanking.AgentBankingApp"
}

// seed4j-needle-gradle-plugins-configurations

repositories {
  mavenCentral()
  // seed4j-needle-gradle-repositories
}

group = "com.agentbanking"
version = "0.0.1-SNAPSHOT"

val profiles = (project.findProperty("profiles") as String? ?: "")
  .split(",")
  .map { it.trim() }
  .filter { it.isNotEmpty() }
// seed4j-needle-profile-activation

dependencies {
  implementation(platform(libs.spring.boot.dependencies))
  implementation(libs.spring.boot.starter)
  implementation(libs.spring.boot.configuration.processor)
  implementation(libs.commons.lang3)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.webmvc)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.hikariCP)
  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.spring.boot.starter.flyway)
  implementation(libs.flyway.database.postgresql)
  implementation(libs.kafka.clients)
  implementation(libs.testcontainers.kafka)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.spring.boot.starter.restclient)
  implementation(libs.spring.boot.starter.oauth2.client)
  implementation(libs.spring.boot.starter.oauth2.resource.server)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  implementation(libs.springdoc.openapi.starter.webmvc.api)
  implementation(platform(libs.spring.cloud.dependencies))
  implementation(libs.spring.cloud.starter.bootstrap)
  implementation(libs.spring.cloud.starter.gateway.server.webflux)

  // Axon Framework for CQRS/Event Sourcing
  implementation("org.axonframework:axon-spring-boot-starter:4.11.1")
  implementation("org.axonframework:axon-server-connector:4.11.1")

  // seed4j-needle-gradle-implementation-dependencies
  // seed4j-needle-gradle-compile-dependencies
  runtimeOnly(libs.postgresql)
  // seed4j-needle-gradle-runtime-dependencies
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.reflections)
  testImplementation(libs.spring.boot.starter.webmvc.test)
  testImplementation(libs.archunit.junit5.api)
  testImplementation(libs.testcontainers.testcontainers.postgresql)
  testImplementation(libs.spring.boot.starter.security.test)

  // Axon testing
  testImplementation("org.axonframework:axon-test:4.11.1")

  // seed4j-needle-gradle-test-dependencies
}

// seed4j-needle-gradle-free-configuration-blocks

tasks.test {
  filter {
    includeTestsMatching("**Test*")
    excludeTestsMatching("**IT*")
    excludeTestsMatching("**CucumberTest*")
  }
  useJUnitPlatform()
  // seed4j-needle-gradle-tasks-test
}

val test by testing.suites.existing(JvmTestSuite::class)
tasks.register<Test>("integrationTest") {
  description = "Runs integration tests."
  group = "verification"
  shouldRunAfter("test")

  testClassesDirs = files(test.map { it.sources.output.classesDirs })
  classpath = files(test.map { it.sources.runtimeClasspath })

  filter {
    includeTestsMatching("**IT*")
    includeTestsMatching("**CucumberTest*")
  }
  useJUnitPlatform()
}
