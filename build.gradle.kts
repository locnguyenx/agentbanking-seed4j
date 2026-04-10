import java.util.Properties
// seed4j-needle-gradle-imports

plugins {
  java
  alias(libs.plugins.spring.boot.get())
  jacoco
  alias(libs.plugins.sonarqube.get())
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
  implementation(platform(libs.spring.boot.dependencies.get()))
  implementation(libs.spring.boot.starter.get())
  implementation(libs.spring.boot.configuration.processor.get())
  implementation(libs.commons.lang3.get())
  implementation(libs.spring.boot.starter.actuator.get())
  implementation(libs.spring.boot.starter.validation.get())
  implementation(libs.spring.boot.starter.webmvc.get())
  implementation(libs.hikariCP.get())
  implementation(libs.spring.boot.starter.data.jpa.get())
  implementation("org.springframework.boot:spring-boot-autoconfigure")
  implementation(libs.spring.boot.starter.flyway.get())
  implementation(libs.flyway.database.postgresql.get())
  implementation(libs.kafka.clients.get())
  implementation(libs.testcontainers.kafka.get())
  implementation(libs.springdoc.openapi.starter.webmvc.ui.get())
  implementation(libs.springdoc.openapi.starter.webmvc.api.get())
  implementation(libs.spring.boot.starter.security.get())
  implementation(libs.jjwt.api.get())
  // seed4j-needle-gradle-implementation-dependencies
  // seed4j-needle-gradle-compile-dependencies
  runtimeOnly(libs.postgresql.get())
  runtimeOnly(libs.jjwt.impl.get())
  runtimeOnly(libs.jjwt.jackson.get())
  // seed4j-needle-gradle-runtime-dependencies

  testImplementation(libs.archunit.junit5.api.get())
  testImplementation(libs.spring.boot.starter.test.get())
  testImplementation(libs.reflections.get())
  testImplementation(libs.spring.boot.starter.webmvc.test.get())
  testImplementation(libs.testcontainers.testcontainers.postgresql.get())
  testImplementation(libs.spring.boot.starter.security.test.get())
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
