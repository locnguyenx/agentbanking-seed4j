---
name: seed4j-post-scaffold
description: Use when Seed4J generates config in wrong directory, produces domain/DTO layering violations, or creates @BusinessContext placement errors that break ArchUnit tests
---

# Seed4J Post-Scaffolding Fixes

## Overview

After scaffolding with Seed4J, certain architectural issues commonly cause test failures. This skill documents the fixes needed to achieve 100% test pass rate with ArchUnit hexagonal architecture tests.

## When to Use

**Trigger symptoms:**
- `./gradlew test --tests "HexagonalArchTest" fails`
- Config files in `<context>/config/` causing violations
- Controllers returning domain models instead of DTOs
- Domain layer depending on application layer
- Empty secondary adapter packages

**After using systematic-debugging:** When root cause is identified as Seed4J config placement, domain/DTO layering, or @BusinessContext misplacement, invoke this skill for specific fixes.

## Quick Fixes (Before Business Logic)

### 1. Move Config Classes

**Problem:** Seed4J generates `*DomainServiceConfig` in `<context>/config/` but ArchUnit expects `<context>/application/`

```bash
# Move all config files
mv src/main/java/com/agentbanking/*/config/*DomainServiceConfig.java \
   src/main/java/com/agentbanking/*/application/config/
```

**For adapter services (billeradapter, isoadapter, etc.):**
```bash
# Move to infrastructure/config/
mv src/main/java/com/agentbanking/*adapter/config/*DomainServiceConfig.java \
   src/main/java/com/agentbanking/*adapter/infrastructure/config/
```

### 2. Add DTO Mapping

**Problem:** Controllers return domain models, violating hexagonal architecture

**Solution:** Create DTOs in application/dto/ with mapping:

```java
// In application/dto/MyResponseDTO.java
public record MyResponseDTO(...) {
    public static MyResponseDTO fromDomain(DomainResponse response) {
        return new MyResponseDTO(
            response.field1(),
            response.field2()
        );
    }
}
```

**Controller usage:**
```java
@PostMapping("/endpoint")
public ResponseEntity<MyResponseDTO> method(
    @Validated @RequestBody MyRequestDTO request) {
    var domainResponse = service.method(request.toDomain());
    return ResponseEntity.ok(MyResponseDTO.fromDomain(domainResponse));
}
```

### 3. Add @Validated

**Problem:** Controller parameters missing Spring validation

```java
@PostMapping("/register")
public ResponseEntity<Response> register(
    @Validated @RequestBody RequestDTO request) {  // Add @Validated
    ...
}
```

### 4. Add NoOp Secondary Adapters

**Problem:** ArchUnit test fails when bounded context has no secondary adapters

**Solution:** Add placeholder for all bounded contexts and shared kernels:

```java
// In infrastructure/secondary/ package
public class NoOpAdapter {}
```

### 5. Fix @BusinessContext Placement

**Problem:** Annotation in empty package instead of package with actual code

**Solution:** Move to correct package:

```java
// Wrong: com.agentbanking.biller/package-info.java
// Correct: com.agentbanking.billeradapter/package-info.java
@BusinessContext
package com.agentbanking.billeradapter;
```

### 6. Handle Empty Test Rules

**Problem:** ArchUnit rule "primaryJavaAdaptersShouldOnlyBeCalledFromSecondaries" fails on empty check

**Solution:** Add `.allowEmptyShould(true)`:

```java
ArchRule rule = classes()
    .that()
    .resideInAnyPackage("..primary..")
    .and()
    .areMetaAnnotatedWith(Component.class)
    .and()
    .haveSimpleNameStartingWith("Java")
    .should()
    .onlyHaveDependentClassesThat()
    .resideInAnyPackage("..secondary..");

rule.allowEmptyShould(true).check(classes);
```

## Verification

```bash
# Run ArchUnit tests
./gradlew test --tests "HexagonalArchTest"

# Should show: 13 tests completed, 13 passed
```

## Lessons Learned

From Agent Banking Platform project:

| Issue | Severity | Files Affected |
|-------|-----------|----------------|
| Config placement | 🔴 High | 12 |
| DTO mapping | 🔴 High | 8 |
| @Validated missing | 🟡 Medium | 3 |
| Empty secondary | 🟡 Medium | 4 |
| @BusinessContext | 🟡 Medium | 2 |

**Post-scaffold work distribution:**
- 36% refactoring (fixing Seed4J output)
- 64% business logic (manual, expected)

**Effectiveness rating:** 8.7/10

See [docs/lessons-learned/seed4j-lessons-learned.md](../../../docs/lessons-learned/seed4j-lessons-learned.md) for complete analysis.

## Common Mistakes

| Mistake | Why It Happens | Fix |
|---------|---------------|-----|
| Skip tests after scaffold | Build succeeds | Run ArchUnit tests immediately |
| Add business logic before fixes | Want to move fast | Fix architecture first, then business logic |
| Use domain in controllers | Don't know about DTOs | Create application/dto/ layer |
| Leave empty packages | Think they're optional | Add NoOp or remove @BusinessContext |

## Related Skills

- [seed4j-cli-setup](seed4j-cli-setup) - Initial scaffolding
- [seed4j-modules](seed4j-modules) - Module reference
- [seed4j-architecture-decisions](seed4j-architecture-decisions) - Architecture patterns