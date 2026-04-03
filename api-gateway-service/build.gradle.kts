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
  mainClass = "com.agentbanking.gateway.ApiGatewayApp"
}

// seed4j-needle-gradle-plugins-configurations

repositories {
  mavenCentral()
  // seed4j-needle-gradle-repositories
}

group = "com.agentbanking.gateway"
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
  implementation(platform(libs.spring.cloud.dependencies))
  implementation(libs.spring.cloud.starter.bootstrap)
  implementation(libs.spring.cloud.starter.gateway.server.webflux)
  implementation(libs.spring.boot.starter.actuator)
  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.2.0")
  implementation("org.springdoc:springdoc-openapi-starter-webflux-api:2.2.0")
  // seed4j-needle-gradle-implementation-dependencies
  // seed4j-needle-gradle-compile-dependencies
  // seed4j-needle-gradle-runtime-dependencies
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.archunit.junit5.api)
  testImplementation(libs.spring.boot.starter.webflux)
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")

  // seed4j-needle-gradle-test-dependencies
}

// seed4j-needle-gradle-free-configuration-blocks

tasks.test {
  filter {
    includeTestsMatching("**Test*")
    excludeTestsMatching("**IT*")
    excludeTestsMatching("**CucumberTest*")
    excludeTestsMatching("**GatewayResourceIT*")
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
