---
name: seed4j-cli-setup
description: Use when scaffolding a Java/Spring Boot project with Seed4J CLI, before applying any modules
---

# Seed4J CLI Setup

## Overview

Seed4J CLI generates hexagonal architecture Spring Boot projects. CLI runs locally and requires Java 25.

## Prerequisites

| Dependency | Required Version | Check Command |
|------------|-----------------|---------------|
| Java | 25 | `java -version` |
| Maven | 3.9+ | `mvn --version` (for building CLI) |
| Node.js | 22+ | `node -v` |
| Git | Any | `git --version` |

## Installation Steps

### 1. Download Java 25

```bash
curl -L -o /tmp/jdk-25.zip "https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse"
unzip -q /tmp/jdk-25.zip -d /tmp/
export JAVA_HOME="/tmp/jdk-25.0.2+10"
export PATH="$JAVA_HOME/bin:$PATH"
```

### 2. Install Maven

```bash
curl -L -o /tmp/maven.zip "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
unzip -q /tmp/maven.zip -d /tmp/
export M2_HOME="/tmp/apache-maven-3.9.9"
export PATH="$M2_HOME/bin:$PATH"
```

### 3. Build Seed4J CLI from Source

```bash
cd /tmp
git clone https://github.com/seed4j/seed4j-cli.git
cd seed4j-cli
mvn clean package -DskipTests
```

**Output:** `/tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar`

### 4. Verify

```bash
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar --version
```

## CLI Commands

| Command | Description |
|---------|-------------|
| `seed4j --version` | Show CLI and Seed4J versions |
| `seed4j list` | List all available modules |
| `seed4j apply <module> [options]` | Apply module to project |
| `seed4j apply <module> --help` | Show module parameters |

## Project Scaffolding Workflow

```bash
cd your-project

# 1. Initialize project
seed4j apply init --project-name "My Project" --base-name MyProject --node-package-manager npm --no-commit

# 2. Setup Gradle (NOT Maven)
seed4j apply gradle-java --package-name com.example --no-commit

# 3. Add modules
seed4j apply spring-boot --no-commit
seed4j apply java-base --no-commit
# ... more modules as needed
```

## Key Rules

- **Always use `--no-commit`** to avoid auto-commits during scaffolding
- **Apply `init` first** - parameters are reused by subsequent modules
- **Use system Maven** not `./mvnw` (wrapper has issues on Windows)
- **Set JAVA_HOME explicitly** before each command

## Common Issues

| Issue | Solution |
|-------|----------|
| Maven wrapper fails | Use `mvn` directly, not `./mvnw` |
| Module parameter missing | Run `apply <module> --help` |
| Build slow | First run downloads dependencies (2-3 min) |
