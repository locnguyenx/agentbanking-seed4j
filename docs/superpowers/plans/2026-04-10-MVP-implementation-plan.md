# Agent Banking Platform - MVP Implementation Plan

> **Status:** READY FOR EXECUTION
> **Mode:** Full TDD + Subagent-Driven Development
> **Scope:** Phases 1-4 (Backend + Backoffice UI)
> **Strategy:** Dependencies-ordered with parallel execution

---

## Execution Summary

| Phase | Scope | Tasks | Parallel Groups |
|-------|-------|------|--------------|
| 1 | Foundation | 4 | Sequential |
| 2 | Core Backend | 9 | A, B, C |
| 3 | Tier 4 Adapters | 5 | Sequential → Parallel |
| 4 | Backoffice UI | 13 | D, E, F |

**Total: 31 tasks**
**Parallel Groups: 6 (A-F)**

---

## Phase 1: Foundation

*Sequential - each builds on previous*

| # | Task | Sub-Plan | TDD Test | Status |
|---|---|---|---|
| 1.1 | Fix End User Guide endpoint paths | Verify all API paths match actual endpoints | ⬜ |
| 1.2 | Redis Idempotency Config | `plans/11-idempotency-redis.md` | Test Redis cache hit/miss | ⬜ |
| 1.3 | Temporal Saga Infrastructure | `plans/03-temporal-infrastructure.md` | Test workflow/activity stubs | ⬜ |
| 1.4 | Flyway Migrations | `plans/04-database-migrations.md` | Test schema creation | ⬜ |

---

## Phase 2: Core Backend Services

*Sequential with parallel opportunities within*

### Group A (Parallel Execution)
| # | Task | Sub-Plan | TDD Test | Status |
|---|---|---|---|
| 2.1 | Rules & Fee Engine | `plans/05-rules-fee-engine.md` | Test fee calculation | ⬜ |
| 2.2 | Ledger & Float | `plans/06-ledger-float.md` | Test double-entry | ⬜ |
| 2.3 | Commission Service | `plans/07-commission.md` | Test commission calc | ⬜ |

### Group B (Parallel Execution)
| # | Task | Sub-Plan | TDD Test | Status |
|---|---|---|---|
| 2.4 | Transaction Orchestrator | `plans/08-transaction-orchestrator.md` | Test saga orchestration | ⬜ |
| 2.5 | Cash Withdrawal | `plans/09-cash-withdrawal.md` | Test withdrawal flow | ⬜ |
| 2.6 | Cash Deposit | `plans/10-cash-deposit.md` | Test deposit flow | ⬜ |

### Group C (Parallel Execution)
| # | Task | Sub-Plan | TDD Test | Status |
|---|---|---|---|
| 2.7 | Onboarding | `plans/12-onboarding.md` | Test KYC workflow | ⬜ |
| 2.8 | Notification | `plans/13-notification.md` | Test SMS dispatch | ⬜ |
| 2.9 | Settlement | `plans/14-settlement.md` | Test EOD settlement | ⬜ |

---

## Phase 3: Tier 4 Adapters

*Sequential → Parallel*

| # | Task | Sub-Plan | TDD Test | Status |
|---|---|---|---|
| 3.1 | API Gateway | `plans/15-api-gateway.md` | Test JWT + rate limiting | ⬜ |
| 3.2 | ISO Adapter | `plans/16-iso-adapter.md` | Test ISO 8583 translation | ⬜ |
| 3.3 | CBS Adapter | `plans/17-cbs-adapter.md` | Test CBS connector | ⬜ |
| 3.4 | HSM Adapter | `plans/18-hsm-adapter.md` | Test PIN block operations | ⬜ |
| 3.5 | Biller Adapter | `plans/19-biller-adapter.md` | Test bill payment | ⬜ |

---

## Phase 4: Backoffice UI

*React + TypeScript + Vite*

### Group D (Parallel Execution)
| # | Task | Component | TDD Test | Status |
|---|---|---|---|
| 4.1 | Project Setup | vite + tsconfig | Verify build | ⬜ |
| 4.2 | Auth Integration | JWT login | Test auth flow | ⬜ |
| 4.3 | RBAC | VIEWER/OPERATOR/ADMIN | Test role enforcement | ⬜ |

### Group E (Parallel Execution)
| # | Task | Component | TDD Test | Status |
|---|---|---|---|
| 4.4 | Dashboard | KPIs + charts | Test data display | ⬜ |
| 4.5 | Agent Management | CRUD table | Test CRUD operations | ⬜ |
| 4.6 | Transaction Monitor | Real-time list | Test filters | ⬜ |
| 4.7 | Settlement Reports | CSV export | Test export | ⬜ |
| 4.8 | e-KYC Review Queue | Review form | Test approval flow | ⬜ |

### Group F (Parallel Execution)
| # | Task | Component | TDD Test | Status |
|---|---|---|---|
| 4.9 | Configuration Editor | Fee/limit forms | Test save | ⬜ |
| 4.10 | Audit Logs | Log viewer | Test search | ⬜ |
| 4.11 | Compliance Dashboard | EFM alerts | Test alerts display | ⬜ |
| 4.12 | Analytics | Charts + reports | Test data aggregation | ⬜ |
| 4.13 | Discrepancy Resolution | Maker-Checker | Test workflow | ⬜ |

---

## Execution Waves

### Wave 1: Foundation (Sequential)
```
1.1 → 1.2 → 1.3 → 1.4
```

### Wave 2: Core Backend (Semi-Parallel)
```
Group A: 2.1, 2.2, 2.3 (parallel)
   ↓
Group B: 2.4, 2.5, 2.6 (parallel)
   ↓
Group C: 2.7, 2.8, 2.9 (parallel)
```

### Wave 3: Tier 4 (Sequential → Parallel)
```
3.1 → (3.2, 3.3, 3.4, 3.5 in parallel)
```

### Wave 4: Backoffice UI (Semi-Parallel)
```
Core UI: 4.1, 4.2, 4.3 (parallel)
   ↓
Business: 4.4, 4.5, 4.6, 4.7, 4.8 (parallel)
   ↓
Advanced: 4.9, 4.10, 4.11, 4.12 (parallel)
   ↓
Final: 4.13
```

---

## Subagent-Driven Development Workflow

### Per Task:
1. Write test (TDD RED)
2. Write implementation (TDD GREEN)
3. Run tests: `./gradlew test` or UI test suite
4. Dispatch code-reviewer subagent
5. Fix Critical/Important issues
6. Mark complete ✅

### Dispatch Template:
```
BASE_SHA=$(git rev-parse HEAD)
HEAD_SHA=$(git rev-parse HEAD)

[Dispatch subagent]
  WHAT_WAS_IMPLEMENTED: <task description>
  PLAN_OR_REQUIREMENTS: <sub-plan reference>
  BASE_SHA: <sha>
  HEAD_SHA: <sha>
  DESCRIPTION: <brief summary>
```

---

## Code Review Checkpoints

| Phase | Checkpoint | Verification |
|-------|------------|--------------|
| 1.4 | After Flyway migrations | `./gradlew test --tests "*Migration*"` pass |
| 2.3 | After Commission | All commission unit tests pass |
| 2.6 | After Cash Deposit | Full transaction flow test pass |
| 3.1 | After API Gateway | Gateway routing tested |
| 4.3 | After RBAC | Role enforcement verified |
| 4.13 | After Discrepancy | Maker-Checker workflow complete |

---

## Current Status

### Completed (History):
- All 4 Seed4J skills created/refactored
- End User Guide created
- Developer User Guide created
- Master Implementation Plan reviewed
- This implementation plan written

### To Execute:
- Phase 1, Task 1.1: End User Guide endpoint correction

---

## Implementation Notes

### Hexagonal Architecture Enforcement
Every bounded context MUST follow the hexagonal pattern:
- Domain layer has ZERO framework imports
- Application layer depends only on domain
- Infrastructure.primary doesn't depend on infrastructure.secondary

### Law V Compliance
All domain services MUST be registered as Spring beans via `@Bean` in config classes, NOT via `@Service`.

### Backoffice Auth
- Separate JWT tokens (not shared with POS agents)
- Roles: VIEWER, OPERATOR, ADMIN
- Role-based page/component access

---

*Plan created: 2026-04-10*
*For execution via subagent-driven development*