plugins {
  java
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

group = "com.agentbanking.common"
version = "0.0.1-SNAPSHOT"

repositories {
  mavenCentral()
}
