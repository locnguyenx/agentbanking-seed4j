import java.util.Properties
// seed4j-needle-gradle-imports

plugins {
  java
  alias(libs.plugins.spring.boot)
  jacoco
  alias(libs.plugins.sonarqube)
  // seed4j-needle-gradle-plugins
}

val springProfilesActive by extra("")
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

jacoco {
  toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
  dependsOn("test", "integrationTest")
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
  executionData.setFrom(fileTree(layout.buildDirectory).include("**/jacoco/test.exec", "**/jacoco/integrationTest.exec"))
}

val sonarProperties = Properties()
File("sonar-project.properties").inputStream().use { inputStream ->
    sonarProperties.load(inputStream)
}

sonarqube {
    properties {
      sonarProperties
        .map { it -> it.key as String to (it.value as String).split(",").map { it.trim() } }
        .forEach { (key, values) -> property(key, values) }
      property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
      property("sonar.junit.reportPaths", "build/test-results/test,build/test-results/integrationTest")
    }
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
if (profiles.isEmpty() || profiles.contains("local")) {
  apply(plugin = "profile-local")
}
// seed4j-needle-profile-activation

dependencies {
  implementation(platform(libs.spring.boot.dependencies))
  implementation(libs.spring.boot.starter)
  implementation(libs.spring.boot.configuration.processor)
  implementation(libs.commons.lang3)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.webmvc)
  implementation(libs.hikariCP)
  implementation(libs.spring.boot.starter.data.jpa)
  implementation("org.springframework.boot:spring-boot-autoconfigure")
  implementation(libs.spring.boot.starter.flyway)
  implementation(libs.flyway.database.postgresql)
  implementation(libs.kafka.clients)
  implementation(libs.testcontainers.kafka)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  implementation(libs.springdoc.openapi.starter.webmvc.api)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.jjwt.api)
  // seed4j-needle-gradle-implementation-dependencies
  // seed4j-needle-gradle-compile-dependencies
  runtimeOnly(libs.postgresql)
  runtimeOnly(libs.jjwt.impl)
  runtimeOnly(libs.jjwt.jackson)
  // seed4j-needle-gradle-runtime-dependencies

  testImplementation(libs.archunit.junit5.api)
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.reflections)
  testImplementation(libs.spring.boot.starter.webmvc.test)
  testImplementation(libs.testcontainers.testcontainers.postgresql)
  testImplementation(libs.spring.boot.starter.security.test)
  // seed4j-needle-gradle-test-dependencies
}


tasks.build {
  dependsOn("processResources")
}

tasks.processResources {
  filesMatching("**/*.yml") {
    filter { it.replace("@spring.profiles.active@", springProfilesActive) }
  }
  filesMatching("**/*.properties") {
    filter { it.replace("@spring.profiles.active@", springProfilesActive) }
  }
}

// seed4j-needle-gradle-free-configuration-blocks

tasks.test {
  filter {
    includeTestsMatching("**Test*")
    excludeTestsMatching("**IT*")
    excludeTestsMatching("**CucumberTest*")
  }
  useJUnitPlatform()
  finalizedBy("jacocoTestReport")
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
