plugins {
  java
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

// Root build.gradle.kts - lightweight parent only
// Each service manages its own dependencies via Seed4J
// Common module is a simple Java library

group = "com.agentbanking"
version = "0.0.1-SNAPSHOT"

repositories {
  mavenCentral()
}
