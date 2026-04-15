# 04. Database Migrations

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Document and verify all 9 existing Flyway migration files (V1–V9) covering the full database schema for the Agent Banking Platform.

**Architecture:** Database-per-service pattern. All migrations target a single PostgreSQL database with Flyway managing version history. Tables are organized by bounded context with proper foreign keys, indexes, and audit fields.

**Tech Stack:** PostgreSQL, Flyway, Gradle

**References:**
- Design: `docs/superpowers/specs/agent-banking-platform-design.md` (Section 4: Data Architecture)

---

## Task 1: Verify Seed4J Flyway Scaffolding

Seed4J has already scaffolded Flyway infrastructure:
- `spring-boot-starter-flyway` in `build.gradle.kts`
- `flyway-database-postgresql` dependency
- Migration directory: `src/main/resources/db/migration/`

**Verify:**
```bash
ls src/main/resources/db/migration/
# Should see V1__ through V9__ migration files
```

- [ ] Flyway dependencies present in `build.gradle.kts`
- [ ] `src/main/resources/db/migration/` directory exists
- [ ] All 9 migration files present (V1–V9)

---

## Task 2: Review Existing Migrations

All 9 migration files exist and are complete. This task documents what each does.

### V1: Common — `V1__common_init.sql`

**Tables:** `audit_log`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| entity_type | VARCHAR(100) | NOT NULL |
| entity_id | VARCHAR(100) | NOT NULL |
| action | VARCHAR(50) | NOT NULL |
| actor_id | VARCHAR(100) | |
| actor_type | VARCHAR(50) | |
| payload | JSONB | |
| trace_id | VARCHAR(100) | |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW() |

**Indexes:** `idx_audit_entity` (entity_type, entity_id), `idx_audit_trace` (trace_id), `idx_audit_created` (created_at)

### V2: Onboarding — `V2__onboarding_init.sql`

**Tables:** `agent`, `agent_user`

| Table | PK | Key Columns |
|-------|----|-------------|
| agent | VARCHAR(50) | business_registration_number (UNIQUE), name, type, status (DEFAULT 'PENDING_APPROVAL'), max_float_limit |
| agent_user | VARCHAR(50) | agent_id (FK→agent), username (UNIQUE), role, status (DEFAULT 'ACTIVE') |

**Indexes:** `idx_agent_status`, `idx_agent_type`, `idx_agent_user_agent`

### V3: Float — `V3__float_init.sql`

**Tables:** `agent_float`, `float_transaction`

| Table | PK | Key Columns |
|-------|----|-------------|
| agent_float | agent_id (FK→agent) | balance, reserved_balance, currency (DEFAULT 'MYR'), version (optimistic locking) |
| float_transaction | UUID | agent_id (FK→agent), transaction_id, type, amount, balance_before, balance_after |

**Indexes:** `idx_float_txn_agent`, `idx_float_txn_transaction`, `idx_float_txn_created`

### V4: Rules — `V4__rules_init.sql`

**Tables:** `fee_config`, `velocity_rule`

| Table | PK | Key Columns |
|-------|----|-------------|
| fee_config | UUID | transaction_type, agent_tier, fee_type, customer_fee_value, agent_commission_value, bank_share_value, daily_limit_amount, daily_limit_count, effective_from, effective_to |
| velocity_rule | UUID | scope, max_transactions_per_day, max_amount_per_day, is_active (DEFAULT TRUE) |

**Indexes:** `idx_fee_config_type_tier` (transaction_type, agent_tier), `idx_fee_config_effective` (effective_from, effective_to)

### V5: Transaction — `V5__transaction_init.sql`

**Tables:** `transaction`, `idempotency_record`

| Table | PK | Key Columns |
|-------|----|-------------|
| transaction | VARCHAR(50) | type, amount, agent_id, customer_account_id, idempotency_key (UNIQUE), saga_execution_id, status, customer_fee, agent_commission, bank_share, error_code, initiated_at, completed_at |
| idempotency_record | idempotency_key | response_payload (JSONB), expires_at |

**Indexes:** `idx_txn_agent`, `idx_txn_status`, `idx_txn_idempotency`, `idx_txn_saga`, `idx_txn_initiated`, `idx_idem_expires`

### V6: Ledger — `V6__ledger_init.sql`

**Tables:** `account`, `ledger_entry`

| Table | PK | Key Columns |
|-------|----|-------------|
| account | VARCHAR(50) | account_type, owner_id, owner_type, currency (DEFAULT 'MYR'), balance, status (DEFAULT 'ACTIVE') |
| ledger_entry | UUID | account_id (FK→account), entry_type, amount, balance_before, balance_after, transaction_id, metadata (JSONB) |

**Indexes:** `idx_account_type`, `idx_account_owner` (owner_id, owner_type), `idx_entry_account`, `idx_entry_txn`, `idx_entry_created`

### V7: Commission — `V7__commission_init.sql`

**Tables:** `commission_entry`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| transaction_id | VARCHAR(50) | NOT NULL |
| agent_id | VARCHAR(50) | NOT NULL |
| commission_type | VARCHAR(50) | NOT NULL |
| transaction_amount | DECIMAL(19,4) | NOT NULL |
| commission_amount | DECIMAL(10,4) | NOT NULL |
| rate_applied | DECIMAL(10,6) | NOT NULL |
| status | VARCHAR(50) | DEFAULT 'PENDING' |
| settled_at | TIMESTAMP WITH TIME ZONE | |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW() |

**Indexes:** `idx_commission_agent`, `idx_commission_txn`, `idx_commission_status`

### V8: Notification — `V8__notification_init.sql`

**Tables:** `notification`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| type | VARCHAR(30) | NOT NULL |
| recipient | VARCHAR(50) | |
| channel | VARCHAR(20) | NOT NULL |
| payload | JSONB | NOT NULL |
| status | VARCHAR(20) | DEFAULT 'PENDING' |
| sent_at | TIMESTAMP WITH TIME ZONE | |
| error_message | TEXT | |
| retry_count | INTEGER | DEFAULT 0 |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW() |

**Indexes:** `idx_notification_status`, `idx_notification_recipient`, `idx_notification_created`

### V9: Settlement — `V9__settlement_init.sql`

**Tables:** `settlement_batch`, `reconciliation_record`, `discrepancy_case`

| Table | PK | Key Columns |
|-------|----|-------------|
| settlement_batch | UUID | agent_id, business_date, total_withdrawals, total_deposits, total_commissions, net_settlement, direction, status (DEFAULT 'PENDING'), cbs_file_generated |
| reconciliation_record | UUID | settlement_batch_id (FK→settlement_batch), transaction_id, discrepancy_type, internal_status, paynet_status, internal_amount, paynet_amount |
| discrepancy_case | UUID | case_id (UNIQUE), discrepancy_type, reconciliation_record_id (FK→reconciliation_record), status (DEFAULT 'PENDING_MAKER'), maker_id, maker_action, reason_code, checker_id, checker_comment |

**Indexes:** `idx_settlement_agent`, `idx_settlement_date`, `idx_settlement_status`, `idx_discrepancy_status`, `idx_discrepancy_case_id`

- [ ] All 9 migration files reviewed and verified against schema above

---

## Task 3: Verify Migration Integrity

### Check migration ordering

```bash
ls -1 src/main/resources/db/migration/V*.sql | sort
```

Expected order: V1 → V2 → V3 → V4 → V5 → V6 → V7 → V8 → V9

### Check foreign key consistency

| FK | References | Migration |
|----|-----------|-----------|
| agent_user.agent_id → agent.id | V2 | Same migration (V2) |
| agent_float.agent_id → agent.id | V3 | Cross-migration (V2→V3) |
| float_transaction.agent_id → agent.id | V3 | Same migration (V3) |
| ledger_entry.account_id → account.id | V6 | Same migration (V6) |
| reconciliation_record.settlement_batch_id → settlement_batch.id | V9 | Same migration (V9) |
| discrepancy_case.reconciliation_record_id → reconciliation_record.id | V9 | Same migration (V9) |

### Check index naming

All indexes use `idx_<entity>_<column>` convention. Verify no duplicates.

- [ ] Migration ordering is sequential (V1–V9)
- [ ] All foreign keys reference tables created in same or earlier migration
- [ ] No duplicate index names
- [ ] All tables have `created_at` audit field
- [ ] Currency defaults to MYR where applicable
- [ ] Timestamps use `TIMESTAMP WITH TIME ZONE`

---

## Task 4: Verify Flyway Runs Clean

### Local verification

```bash
# Start PostgreSQL (Docker)
docker run --rm -d --name pg-test -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=agentbanking -p 5432:5432 postgres:16

# Run Flyway migrate
./gradlew flywayMigrate -Dspring.datasource.url=jdbc:postgresql://localhost:5432/agentbanking -Dspring.datasource.username=postgres -Dspring.datasource.password=postgres

# Verify tables created
docker exec pg-test psql -U postgres -d agentbanking -c "\dt"

# Cleanup
docker stop pg-test
```

Expected: All 14 tables created across 9 migrations

- [ ] `./gradlew flywayMigrate` succeeds
- [ ] All tables present in database
- [ ] `./gradlew flywayValidate` passes

---

## Task 5: New Migration Workflow (Reference)

If a new migration is needed in the future, use this process:

### Seed4J CLI (for stub)

```bash
java -jar /tmp/seed4j-cli/target/seed4j-cli-0.0.1-SNAPSHOT.jar apply flyway-migration --name V10_context_name
```

### Manual DDL

Write the SQL manually following these conventions:
- Table names: `snake_case`, singular
- Column names: `snake_case`
- Primary keys: `id` (UUID or VARCHAR(50) for saga compatibility)
- Foreign keys: `<referenced_table>_id`
- Audit fields: `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
- Currency: VARCHAR(3) DEFAULT 'MYR'
- Indexes: `idx_<table>_<column>` convention
- JSONB for flexible payloads

---

## Summary

| Migration | File | Context | Tables | Status |
|-----------|------|---------|--------|--------|
| V1 | `V1__common_init.sql` | Common | audit_log | EXISTS |
| V2 | `V2__onboarding_init.sql` | Onboarding | agent, agent_user | EXISTS |
| V3 | `V3__float_init.sql` | Float | agent_float, float_transaction | EXISTS |
| V4 | `V4__rules_init.sql` | Rules | fee_config, velocity_rule | EXISTS |
| V5 | `V5__transaction_init.sql` | Transaction | transaction, idempotency_record | EXISTS |
| V6 | `V6__ledger_init.sql` | Ledger | account, ledger_entry | EXISTS |
| V7 | `V7__commission_init.sql` | Commission | commission_entry | EXISTS |
| V8 | `V8__notification_init.sql` | Notification | notification | EXISTS |
| V9 | `V9__settlement_init.sql` | Settlement | settlement_batch, reconciliation_record, discrepancy_case | EXISTS |

**Total:** 9 migrations, 14 tables, all pre-existing. No new SQL files to create.
