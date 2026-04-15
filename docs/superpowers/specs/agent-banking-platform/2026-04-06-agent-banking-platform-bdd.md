# Agent Banking Platform — BDD Specification v2.0

**Version:** 2.0  
**Date:** 2026-04-06  
**Project:** Agent Banking Platform  
**Architecture:** Modular Monolith with Temporal Saga Orchestration  
**Previous Version:** 1.1 (2026-03-26)  
**BRD Reference:** `2026-04-06-agent-banking-platform-brd.md`  
**Design Reference:** `2026-04-06-agent-banking-platform-design.md`

---

## Table of Contents

1. [Transaction Orchestrator (Temporal Saga)](#1-transaction-orchestrator-temporal-saga)
2. [Rules & Fee Engine](#2-rules--fee-engine)
3. [Ledger & Float Service](#3-ledger--float-service)
4. [Cash Withdrawal](#4-cash-withdrawal)
5. [Cash Deposit](#5-cash-deposit)
6. [e-KYC & Onboarding](#6-e-kyc--onboarding)
7. [Bill Payments](#7-bill-payments)
8. [Prepaid Top-up](#8-prepaid-top-up)
9. [DuitNow & JomPAY](#9-duitnow--jompay)
10. [e-Wallet & eSSP](#10-e-wallet--essp)
11. [Agent Onboarding](#11-agent-onboarding)
12. [Reversals & Disputes](#12-reversals--disputes)
13. [Merchant Services](#13-merchant-services)
14. [EOD Settlement & Reconciliation](#14-eod-settlement--reconciliation)
15. [Discrepancy Resolution](#15-discrepancy-resolution)
16. [API Gateway & Backoffice](#16-api-gateway--backoffice)
17. [STP Processing](#17-stp-processing)
18. [Translation Layer — Tier 4](#18-translation-layer--tier-4)
19. [Traceability Matrix](#19-traceability-matrix)
20. [Error Code Reference](#20-error-code-reference)
21. [Summary](#21-summary)

---

## Scenario Classification

- **[HP]** = Happy Path (optimal user flow)
- **[EC]** = Edge Case (boundary, invalid, or extreme scenarios)

---

## 1. Transaction Orchestrator (Temporal Saga)

### Narrative

All financial transactions flow through the **Transaction Orchestrator**, which uses **Temporal** as the Saga orchestration engine. Each transaction is executed as a Temporal Workflow with the following key characteristics:

- **Saga Execution ID**: Every transaction is assigned a unique `sagaExecutionId` (Temporal Workflow ID) which is persisted and can be resumed after crashes
- **Activities**: Each step in a transaction (validate, lock float, debit, credit, notify) is a Temporal Activity with its own retry policy
- **Compensation Handlers**: If any step fails, Temporal automatically executes compensation handlers in reverse order (e.g., reversal of float debit)
- **Store & Forward**: Reversals triggered by network timeouts are persisted as Temporal durable executions — they survive system restarts and retry automatically
- **Idempotency**: Every request must include `X-Idempotency-Key`; duplicate requests return cached responses (TTL: 24h in Redis)

**Typical Saga Flow (e.g., Cash Withdrawal):**
1. `validateTransaction` Activity — fee lookup, limit check, velocity check
2. `lockAgentFloat` Activity — PESSIMISTIC_WRITE on AgentFloat
3. `debitAgentFloat` Activity — debits float, creates journal entry
4. `sendToSwitch` Activity — ISO message to PayNet via ISO Adapter
5. `creditCustomer` Activity — credits customer account via CBS Connector
6. `calculateCommission` Activity — splits fee into commission components
7. `sendNotifications` Activity — SMS via Kafka to Notification Service

### BDD-TO01 [HP] — US-TO01, US-TO02
```gherkin
Scenario: Successful cash withdrawal via Temporal Saga
  Given the Transaction Orchestrator receives a CASH_WITHDRAWAL request with:
    | field              | value           |
    | amount            | 500.00          |
    | agentId           | AGT-01          |
    | customerCardMasked | 411111******1111 |
    | idempotencyKey   | IDEM-123        |
  And the saga execution ID is "saga-001"
  When the Temporal Workflow "CashWithdrawalWorkflow" executes
  Then Activity "validateTransaction" should be called
  And Activity "lockAgentFloat" should be called with PESSIMISTIC_WRITE
  And Activity "sendToSwitch" should be called (ISO 8583 MTI 0100)
  And Activity "creditCustomer" should be called via CBS Connector
  And Activity "debitAgentFloat" should be called
  And Activity "calculateCommission" should be called
  And Activity "sendNotifications" should be called
  And the Transaction should be created with status COMPLETED
  And sagaExecutionId "saga-001" should be recorded in Transaction
```

### BDD-TO01-EC-01 [EC] — US-TO01
```gherkin
Scenario: Saga resumes from last completed step after Orchestrator crash
  Given a CASH_WITHDRAWAL Saga "saga-001" was in progress
  And Activity "validateTransaction" completed
  And Activity "lockAgentFloat" completed
  And the Orchestrator process crashed
  When the Orchestrator restarts
  Then Temporal should resume "saga-001" from the last completed Activity
  And Activity "sendToSwitch" should be called (skip already-completed steps)
  And the Transaction should eventually complete with status COMPLETED
  And no duplicate Activities should be executed (idempotency)
```

### BDD-TO01-EC-02 [EC] — US-TO01
```gherkin
Scenario: Saga executes compensation handlers on Activity failure
  Given a CASH_WITHDRAWAL Saga "saga-002" is in progress
  And Activity "sendToSwitch" returns failure (network timeout)
  When the Temporal Workflow catches the ActivityFailure
  Then Temporal should execute compensation in reverse order:
    | step               | compensation            |
    | lockAgentFloat     | unlockAgentFloat        |
    | validateTransaction | (no compensation needed) |
  And the Transaction should be created with status REVERSAL_INITIATED
  And AgentFloat.balance should be restored to original value
```

### BDD-TO01-EC-03 [EC] — US-TO01, US-TO02
```gherkin
Scenario: Store & Forward reversal persists after system restart
  Given a CASH_WITHDRAWAL Saga "saga-003" triggered reversal
  And the Reversal message is in Temporal's internal queue
  And the Temporal service restarts
  When the Temporal service recovers
  Then the Reversal message should still be in the queue
  And Temporal should retry the reversal delivery
  And the Transaction.status should eventually become REVERSED
  And AgentFloat.balance should be restored
```

---

## 2. Rules & Fee Engine

### BDD-R01 [HP] — US-R01, FR-1.1, FR-1.4
```gherkin
Scenario: Configure fee structure for Micro agent cash withdrawal
  Given a FeeConfig exists:
    | field                | value           |
    | transactionType      | CASH_WITHDRAWAL |
    | agentTier            | MICRO           |
    | feeType              | FIXED           |
    | customerFeeValue     | 1.00            |
    | agentCommissionValue | 0.20            |
    | bankShareValue       | 0.80            |
    | dailyLimitAmount     | 5000.00         |
    | dailyLimitCount      | 10              |
  And an Agent exists:
    | field     | value  |
    | agentId   | AGT-01 |
    | tier      | MICRO  |
    | status    | ACTIVE |
  When Agent "AGT-01" processes a CASH_WITHDRAWAL of RM 500.00
  Then the Rules Service Activity should return:
    | field            | value   |
    | customerFee      | 1.00    |
    | agentCommission  | 0.20    |
    | bankShare        | 0.80    |
  And the Transaction should have status PENDING
```

### BDD-R01-PCT [HP] — US-R01, FR-1.1
```gherkin
Scenario: Percentage-based fee for Premier agent cash withdrawal
  Given a FeeConfig exists:
    | field                | value           |
    | transactionType      | CASH_WITHDRAWAL |
    | agentTier            | PREMIER         |
    | feeType              | PERCENTAGE      |
    | customerFeeValue     | 0.005           |
    | agentCommissionValue | 0.002           |
    | bankShareValue       | 0.003           |
  When Agent "AGT-02" (PREMIER) processes a CASH_WITHDRAWAL of RM 10000.00
  Then the Transaction should have:
    | field            | value  |
    | customerFee      | 50.00  |
    | agentCommission  | 20.00  |
    | bankShare        | 30.00  |
```

### BDD-R01-EC-01 [EC] — US-R01, FR-1.1
```gherkin
Scenario: No fee configuration exists for transaction type and tier
  Given no FeeConfig exists for transactionType "CASH_WITHDRAWAL" and agentTier "MICRO"
  When Agent "AGT-01" (MICRO) processes a CASH_WITHDRAWAL of RM 500.00
  Then the system should return error code "ERR_VAL_001"
  And the Transaction.status should be FAILED
```

### BDD-R01-EC-02 [EC] — US-R01, FR-1.1
```gherkin
Scenario: Fee configuration is expired (effectiveTo is in the past)
  Given a FeeConfig exists with effectiveTo "2026-01-01" for CASH_WITHDRAWAL MICRO
  And today is "2026-04-06"
  When Agent "AGT-01" (MICRO) processes a CASH_WITHDRAWAL
  Then the system should return error code "ERR_VAL_002"
  And the Transaction.status should be FAILED
```

### BDD-R01-EC-03 [EC] — US-R01, FR-1.4
```gherkin
Scenario: Fee component values do not sum correctly
  Given a FeeConfig exists:
    | field                | value |
    | customerFeeValue     | 1.00  |
    | agentCommissionValue | 0.50  |
    | bankShareValue       | 0.30  |
  And the sum (1.00) != (0.50 + 0.30)
  When the FeeConfig is saved
  Then the system should reject the configuration
  And return error code "ERR_VAL_003"
```

### BDD-R02 [HP] — US-R02, FR-1.2
```gherkin
Scenario: Daily transaction limit check passes
  Given a FeeConfig with dailyLimitAmount "10000.00" for STANDARD CASH_WITHDRAWAL
  And Agent "AGT-03" (STANDARD) has completed RM 5000.00 in CASH_WITHDRAWAL today
  When Agent "AGT-03" attempts a CASH_WITHDRAWAL of RM 3000.00
  Then the limit check should pass
  And the Transaction.status should be PENDING
```

### BDD-R02-EC-01 [EC] — US-R02, FR-1.2
```gherkin
Scenario: Daily transaction limit exceeded — amount boundary
  Given a FeeConfig with dailyLimitAmount "5000.00" for MICRO CASH_WITHDRAWAL
  And Agent "AGT-01" (MICRO) has completed RM 4500.00 in CASH_WITHDRAWAL today
  When Agent "AGT-01" attempts a CASH_WITHDRAWAL of RM 1000.00
  Then the limit check should fail
  And the system should return error code "ERR_BIZ_201"
  And the Transaction.status should be FAILED
```

### BDD-R02-EC-02 [EC] — US-R02, FR-1.2
```gherkin
Scenario: Daily transaction count limit exceeded
  Given a FeeConfig with dailyLimitCount "10" for MICRO CASH_WITHDRAWAL
  And Agent "AGT-01" (MICRO) has completed 10 CASH_WITHDRAWAL transactions today
  When Agent "AGT-01" attempts another CASH_WITHDRAWAL
  Then the limit check should fail
  And the system should return error code "ERR_BIZ_202"
```

### BDD-R02-EC-03 [EC] — US-R02, FR-1.2
```gherkin
Scenario: Transaction amount is zero
  Given a FeeConfig exists for CASH_WITHDRAWAL MICRO
  When Agent "AGT-01" (MICRO) attempts a CASH_WITHDRAWAL of RM 0.00
  Then the system should return error code "ERR_VAL_004"
  And the Transaction.status should be FAILED
```

### BDD-R02-EC-04 [EC] — US-R02, FR-1.2
```gherkin
Scenario: Transaction amount is negative
  Given a FeeConfig exists for CASH_WITHDRAWAL MICRO
  When Agent "AGT-01" (MICRO) attempts a CASH_WITHDRAWAL of RM -100.00
  Then the system should return error code "ERR_VAL_004"
  And the Transaction.status should be FAILED
```

### BDD-R04 [HP] — US-R04, FR-1.4
```gherkin
Scenario: Percentage-based fee calculation with rounding
  Given a FeeConfig exists:
    | field                | value           |
    | transactionType      | CASH_WITHDRAWAL |
    | agentTier            | STANDARD        |
    | feeType              | PERCENTAGE      |
    | customerFeeValue     | 0.005           |
    | agentCommissionValue | 0.002           |
    | bankShareValue       | 0.003           |
  When Agent "AGT-03" (STANDARD) processes a CASH_WITHDRAWAL of RM 333.33
  Then the Transaction should have:
    | field            | value |
    | customerFee      | 1.67  |
    | agentCommission  | 0.67  |
    | bankShare        | 1.00  |
  And rounding should use HALF_UP to 2 decimal places
```

### BDD-R03 [HP] — US-R03, FR-1.3
```gherkin
Scenario: Velocity check passes
  Given a VelocityRule exists:
    | field                 | value |
    | maxTransactionsPerDay | 5     |
    | maxAmountPerDay       | 25000 |
    | scope                 | GLOBAL |
    | isActive              | true  |
  And customer MyKad "123456789012" has 2 transactions today totaling RM 5000
  When a transaction is initiated for MyKad "123456789012"
  Then the velocity check should pass
```

### BDD-R03-EC-01 [EC] — US-R03, FR-1.3
```gherkin
Scenario: Velocity check fails — transaction count exceeded
  Given a VelocityRule with maxTransactionsPerDay "5" and isActive "true"
  And customer MyKad "123456789012" has 5 transactions today
  When a transaction is initiated for MyKad "123456789012"
  Then the velocity check should fail
  And the system should return error code "ERR_BIZ_203"
  And the Transaction.status should be FAILED
```

### BDD-R03-EC-02 [EC] — US-R03, FR-1.3
```gherkin
Scenario: Velocity check fails — amount exceeded
  Given a VelocityRule with maxAmountPerDay "25000" and isActive "true"
  And customer MyKad "123456789012" has transactions totaling RM 24000 today
  When a transaction of RM 2000 is initiated for MyKad "123456789012"
  Then the velocity check should fail
  And the system should return error code "ERR_BIZ_204"
```

### BDD-R03-EC-03 [EC] — US-R03, FR-1.3
```gherkin
Scenario: Velocity rule is inactive — check is skipped
  Given a VelocityRule with maxTransactionsPerDay "5" and isActive "false"
  And customer MyKad "123456789012" has 10 transactions today
  When a transaction is initiated for MyKad "123456789012"
  Then the velocity check should pass (rule not enforced)
```

### BDD-R03-EC-04 [EC] — US-R03, FR-1.3
```gherkin
Scenario: No velocity rule configured — default behavior
  Given no active VelocityRule exists
  When a transaction is initiated for any MyKad
  Then the velocity check should pass (no rule to enforce)
```

---

## 3. Ledger & Float Service

### BDD-L01 [HP] — US-L01, FR-2.1
```gherkin
Scenario: Agent checks wallet balance
  Given an AgentFloat exists:
    | field           | value     |
    | agentId         | AGT-01    |
    | balance         | 10000.00  |
    | reservedBalance | 0.00      |
    | currency        | MYR       |
  When Agent "AGT-01" requests balance inquiry
  Then the response should contain:
    | field            | value     |
    | balance          | 10000.00  |
    | reservedBalance  | 0.00      |
    | availableBalance | 10000.00  |
```

### BDD-L01-EC-01 [EC] — US-L01, FR-2.1
```gherkin
Scenario: Agent float not found
  Given no AgentFloat exists for agentId "AGT-99"
  When Agent "AGT-99" requests balance inquiry
  Then the system should return error code "ERR_BIZ_205"
```

### BDD-L01-EC-02 [EC] — US-L01, FR-2.1
```gherkin
Scenario: Agent is deactivated — balance inquiry blocked
  Given an Agent exists with agentId "AGT-01" and status "DEACTIVATED"
  When Agent "AGT-01" requests balance inquiry
  Then the system should return error code "ERR_BIZ_206"
```

### BDD-L02 [HP] — US-L02, FR-2.2
```gherkin
Scenario: Transaction creates double-entry journal
  Given Agent "AGT-01" processes a CASH_WITHDRAWAL of RM 500.00
  When the transaction is completed
  Then two JournalEntry records should be created:
    | entryType | accountCode | amount | description                    |
    | DEBIT     | AGT_FLOAT   | 500.00 | Debit agent float              |
    | CREDIT    | BANK_SETTLE | 500.00 | Credit bank settlement account |
  And both entries should reference the same transactionId
```

### BDD-L03 [HP] — US-L03, FR-2.1, FR-2.3
```gherkin
Scenario: Real-time settlement updates agent float
  Given AgentFloat for "AGT-01" has balance "10000.00"
  When a CASH_WITHDRAWAL of RM 500.00 completes
  Then AgentFloat.balance should be "9500.00"
  And AgentFloat.updatedAt should be the current timestamp
  And no EOD batch processing is required
```

### BDD-L03-EC-01 [EC] — US-L03, FR-2.1, FR-2.3
```gherkin
Scenario: Insufficient agent float for withdrawal
  Given AgentFloat for "AGT-01" has balance "200.00"
  When a CASH_WITHDRAWAL of RM 500.00 is attempted
  Then the system should return error code "ERR_BIZ_207"
  And AgentFloat.balance should remain "200.00"
  And the Transaction.status should be FAILED
```

### BDD-L03-EC-02 [EC] — US-L03, FR-2.3
```gherkin
Scenario: Concurrent withdrawal race condition
  Given AgentFloat for "AGT-01" has balance "600.00"
  And two concurrent CASH_WITHDRAWAL requests of RM 500.00 each
  When both requests are processed simultaneously
  Then only one request should succeed (PESSIMISTIC_WRITE lock)
  And the other should return error code "ERR_BIZ_207"
  And AgentFloat.balance should be "100.00" after both complete
```

### BDD-L04 [HP] — US-L04, FR-5.1
```gherkin
Scenario: Customer balance inquiry via card + PIN
  Given a customer presents card "411111******1111" with valid PIN
  When the POS terminal requests balance inquiry
  Then the system should return the account balance
  And the card number should be masked in all logs
```

### BDD-L04-EC-01 [EC] — US-L04, FR-5.1
```gherkin
Scenario: Balance inquiry with invalid PIN
  Given a customer presents card "411111******1111" with invalid PIN
  When the POS terminal requests balance inquiry
  Then the system should return error code "ERR_AUTH_001"
  And no balance information should be returned
```

### BDD-L04-EC-02 [EC] — US-L04, FR-2.4
```gherkin
Scenario: Duplicate balance inquiry with same idempotency key
  Given a balance inquiry was completed with idempotency key "IDEM-123"
  When another balance inquiry is received with idempotency key "IDEM-123"
  Then the system should return the cached response from the first request
  And no new Transaction should be created
```

### BDD-L02-DC [HP] — US-L02, FR-2.2 — Deposit Double-Entry
```gherkin
Scenario: Cash deposit creates correct double-entry journal
  Given Agent "AGT-01" processes a CASH_DEPOSIT of RM 1000.00
  When the transaction is completed
  Then two JournalEntry records should be created:
    | entryType | accountCode  | amount  | description                    |
    | DEBIT     | CUST_DEPOSIT | 1000.00 | Credit customer account        |
    | CREDIT    | AGT_FLOAT    | 1000.00 | Debit agent float              |
  And both entries should reference the same transactionId
```

### BDD-L02-MER [HP] — US-L02, FR-2.2 — Merchant Retail Sale
```gherkin
Scenario: Retail sale creates correct double-entry journal with MDR
  Given Agent "AGT-01" processes a RETAIL_SALE of RM 100.00 with MDR rate 1%
  When the transaction is completed
  Then two JournalEntry records should be created:
    | entryType | accountCode  | amount | description                          |
    | DEBIT     | CUST_ACCOUNT | 100.00 | Debit customer account               |
    | CREDIT    | AGT_FLOAT    | 99.00  | Credit agent float (net of MDR)      |
  And one JournalEntry for MDR:
    | entryType | accountCode  | amount | description                          |
    | CREDIT    | MDR_INCOME   | 1.00   | MDR fee retained by bank             |
```

### BDD-L02-PIN [HP] — US-L02, FR-2.2 — PIN Purchase Double-Entry
```gherkin
Scenario: PIN purchase creates correct double-entry journal
  Given Agent "AGT-01" processes a PIN_PURCHASE of RM 10.00
  When the transaction is completed
  Then two JournalEntry records should be created:
    | entryType | accountCode       | amount | description                    |
    | DEBIT     | AGT_FLOAT        | 10.00  | Debit agent float              |
    | CREDIT    | DIGITAL_INVENTORY | 10.00  | Credit digital inventory account |
```

---

## 4. Cash Withdrawal

### BDD-W01 [HP] — US-L05, FR-3.1, FR-3.3, FR-3.5
```gherkin
Scenario: Successful ATM card withdrawal via Temporal Saga
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "10000.00"
  And a FeeConfig for CASH_WITHDRAWAL STANDARD:
    | field            | value  |
    | customerFeeValue | 1.00   |
    | dailyLimitAmount | 10000  |
  And the customer presents a valid EMV card with correct PIN
  When the POS terminal requests CASH_WITHDRAWAL of RM 500.00
  Then the Transaction Orchestrator should start Temporal Workflow "CashWithdrawalWorkflow"
  And the Switch Adapter should send ISO 8583 MTI 0100 authorization to PayNet
  And on approval, the Transaction should be created:
    | field              | value           |
    | transactionType    | CASH_WITHDRAWAL |
    | amount             | 500.00          |
    | customerFee        | 1.00            |
    | agentCommission    | 0.20            |
    | bankShare          | 0.80            |
    | status             | COMPLETED       |
    | customerCardMasked | 411111******1111 |
  And AgentFloat.balance should be "9500.00"
  And two JournalEntry records should exist (debit float, credit settlement)
  And the SagaExecutionId should be recorded in the Transaction
```

### BDD-W01-EC-01 [EC] — US-L05, FR-3.1
```gherkin
Scenario: Withdrawal with invalid card PIN
  Given the customer presents an EMV card with incorrect PIN
  When the POS terminal requests CASH_WITHDRAWAL
  Then the Switch Adapter should receive ISO 8583 decline from PayNet
  And the Transaction.status should be FAILED
  And the Transaction.errorCode should be "ERR_AUTH_001"
  And AgentFloat should not be modified
```

### BDD-W01-EC-02 [EC] — US-L05, FR-3.4
```gherkin
Scenario: Terminal printer failure after switch approval — Temporal compensation triggers
  Given the Switch Adapter has received approval from PayNet
  And the Transaction.status is COMPLETED
  And AgentFloat was debited RM 500.00
  And the saga execution ID is "saga-w01-001"
  When the POS terminal reports printer failure
  Then the Transaction Orchestrator should signal Temporal for reversal
  And Temporal should execute the compensation handler
  And the Transaction.status should change to REVERSAL_INITIATED
  And AgentFloat.balance should be restored to original amount
  And the Transaction should eventually transition to REVERSED
```

### BDD-W01-EC-03 [EC] — US-L05, FR-3.4, FR-18.1
```gherkin
Scenario: Network drop after switch approval — Temporal Store & Forward reversal
  Given the Switch Adapter has received approval from PayNet
  And the network connection to the POS terminal drops
  And the saga execution ID is "saga-w01-002"
  When the Temporal Workflow detects the network failure
  Then Temporal should persist the reversal request durably
  And retry the MTI 0400 Reversal when network is restored
  And the Transaction.status should eventually change to REVERSED
  And the SagaStepLog should record all retry attempts
```

### BDD-W01-EC-04 [EC] — US-L05, FR-3.3
```gherkin
Scenario: Withdrawal amount exceeds daily limit
  Given the daily withdrawal limit for STANDARD is RM 10000
  And Agent "AGT-01" has processed RM 9500 in withdrawals today
  When a CASH_WITHDRAWAL of RM 1000 is attempted
  Then the system should return error code "ERR_BIZ_201"
  And the Transaction.status should be FAILED
```

### BDD-W01-EC-05 [EC] — US-L05, NFR-4.2
```gherkin
Scenario: Withdrawal outside geofence — location violation
  Given Agent "AGT-01" is registered at GPS (3.1390, 101.6869)
  And the POS terminal reports GPS (3.2000, 101.7000) — 7km away
  When a CASH_WITHDRAWAL is attempted
  Then the system should return error code "ERR_BIZ_208"
  And the Transaction.status should be FAILED
```

### BDD-W01-EC-06 [EC] — US-L05, NFR-4.2
```gherkin
Scenario: GPS unavailable on POS terminal
  Given the POS terminal cannot obtain GPS coordinates
  When a CASH_WITHDRAWAL is attempted
  Then the system should return error code "ERR_BIZ_209"
  And the Transaction.status should be FAILED
```

### BDD-W01-EC-07 [EC] — US-L05, FR-2.4
```gherkin
Scenario: Duplicate withdrawal with same idempotency key
  Given a CASH_WITHDRAWAL was completed with idempotency key "IDEM-789"
  When another withdrawal request arrives with idempotency key "IDEM-789"
  Then the system should return the original response
  And AgentFloat should not be debited again
  And no new Transaction should be created
```

### BDD-W01-EC-08 [EC] — US-L05, FR-3.4, FR-18.2
```gherkin
Scenario: Reversal fails after maximum Temporal retries
  Given a reversal (MTI 0400) has been retried 3 times by Temporal and still fails
  Then Temporal should mark the Workflow as FAILED
  And the system should flag the Transaction for manual investigation
  And create an AuditLog entry with action "FAIL"
  And alert the backoffice operations team
  And the SagaStepLog should contain all 3 failed attempts
```

### BDD-W01-SMS [HP] — US-L05, FR-3.1
```gherkin
Scenario: Withdrawal triggers SMS notification to customer
  Given a successful CASH_WITHDRAWAL of RM 500.00
  When the Temporal Saga completes
  Then the Notification Gateway should receive a Kafka message
  And the SMS content should include: "RM 500 withdrawn at Agent XYZ"
  And the SMS should provide non-repudiation proof
```

### BDD-W02 [HP] — US-L06, FR-3.2 — MyKad Withdrawal
```gherkin
Scenario: Successful MyKad-based withdrawal
  Given Agent "AGT-01" has AgentFloat balance "10000.00"
  And the customer presents MyKad "123456789012" with biometric match
  When the POS terminal requests CASH_WITHDRAWAL of RM 200.00
  Then the system should verify MyKad via JPN API
  And the biometric match should return MATCH
  And the Transaction should be created with status COMPLETED
  And AgentFloat.balance should be "9800.00"
```

### BDD-W02-EC-01 [EC] — US-L06, FR-3.2
```gherkin
Scenario: MyKad withdrawal with biometric mismatch
  Given the customer presents MyKad "123456789012"
  And the biometric match returns NO_MATCH
  When the POS terminal requests CASH_WITHDRAWAL
  Then the system should return error code "ERR_AUTH_002"
  And the Transaction.status should be FAILED
```

---

## 5. Cash Deposit

### BDD-D01 [HP] — US-L07, FR-2.5, FR-4.1, FR-4.3
```gherkin
Scenario: Successful cash deposit with account validation
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides destination account "1234567890"
  And ProxyEnquiry confirms the account is valid
  When the customer hands RM 1000.00 cash to the agent
  And the POS terminal confirms cash received
  Then the system should credit the destination account RM 1000.00
  And the Transaction should be created:
    | field            | value        |
    | transactionType  | CASH_DEPOSIT |
    | amount           | 1000.00      |
    | status           | COMPLETED    |
  And AgentFloat.balance should be "6000.00"
```

### BDD-D01-EC-01 [EC] — US-L07, FR-2.5
```gherkin
Scenario: Deposit to invalid account — ProxyEnquiry fails
  Given the customer provides destination account "9999999999"
  And ProxyEnquiry returns "ACCOUNT_NOT_FOUND"
  When the POS terminal attempts CASH_DEPOSIT
  Then the system should return error code "ERR_VAL_005"
  And the Transaction.status should be FAILED
  And no money should be moved
```

### BDD-D01-EC-02 [EC] — US-L07, FR-4.1
```gherkin
Scenario: Deposit amount is zero
  Given ProxyEnquiry confirms account "1234567890" is valid
  When a CASH_DEPOSIT of RM 0.00 is attempted
  Then the system should return error code "ERR_VAL_004"
  And the Transaction.status should be FAILED
```

### BDD-D01-EC-03 [EC] — US-L07, FR-4.1
```gherkin
Scenario: Deposit amount is negative
  Given ProxyEnquiry confirms account "1234567890" is valid
  When a CASH_DEPOSIT of RM -500.00 is attempted
  Then the system should return error code "ERR_VAL_004"
  And the Transaction.status should be FAILED
```

### BDD-D01-EC-04 [EC] — US-L07, FR-2.1
```gherkin
Scenario: Agent float cap exceeded after deposit
  Given Agent "AGT-01" (MICRO) has a float cap of RM 20000.00
  And AgentFloat.balance is "19500.00"
  When a CASH_DEPOSIT of RM 1000.00 is attempted
  Then the system should return error code "ERR_BIZ_210"
  And the Transaction.status should be FAILED
```

### BDD-D01-NIC [HP] — US-L07, FR-4.1
```gherkin
Scenario: Deposit with name inquiry confirmation (low value)
  Given the customer provides destination account "1234567890"
  And ProxyEnquiry returns account holder name "MOHD A***D BIN AL*"
  And the transaction amount is RM 500.00 (below high-value threshold)
  When the customer confirms the name is correct
  Then the CASH_DEPOSIT should proceed
  And the Transaction.status should be COMPLETED
```

### BDD-D01-BIO [HP] — US-L07, FR-4.1
```gherkin
Scenario: High-value deposit requires MyKad biometric match
  Given the customer provides destination account "1234567890"
  And the transaction amount is RM 5000.00 (above high-value threshold)
  When the customer attempts CASH_DEPOSIT
  Then the system should require MyKad biometric verification
  And the biometric match must return MATCH
  Then the Transaction should proceed with status COMPLETED
```

### BDD-D02 [HP] — US-L08, FR-4.2
```gherkin
Scenario: Successful card-based deposit
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer presents a valid EMV card with correct PIN
  And ProxyEnquiry confirms the destination account is valid
  When the POS terminal requests CASH_DEPOSIT of RM 500.00
  Then the system should debit the card and credit the destination account
  And AgentFloat.balance should be "5500.00"
  And the Transaction.status should be COMPLETED
```

### BDD-D02-EC-01 [EC] — US-L08, FR-4.2
```gherkin
Scenario: Card deposit with invalid PIN
  Given the customer presents an EMV card with incorrect PIN
  When the POS terminal requests CASH_DEPOSIT
  Then the system should return error code "ERR_AUTH_001"
  And the Transaction.status should be FAILED
  And AgentFloat should not be modified
```

---

## End of Sections 1-5
## 6. e-KYC & Onboarding

### BDD-O01 [HP] — US-O01, FR-6.1
```gherkin
Scenario: Successful MyKad verification via JPN
  Given an agent initiates e-KYC for MyKad "123456789012"
  And the JPN API returns:
    | field       | value         |
    | fullName    | AHMAD BIN ABU |
    | dateOfBirth | 1990-05-15    |
    | amlStatus   | CLEAN         |
  When the verification completes
  Then a KycVerification should be created:
    | field              | value           |
    | mykadNumber        | 123456789012    |
    | fullName           | AHMAD BIN ABU   |
    | dateOfBirth        | 1990-05-15      |
    | age                | 35              |
    | amlStatus          | CLEAN           |
    | verificationStatus | AUTO_APPROVED   |
```

### BDD-O02 [HP] — US-O02, FR-6.2, FR-6.3
```gherkin
Scenario: Successful biometric match-on-card with auto-approval
  Given MyKad "123456789012" has been verified via JPN (AML=CLEAN, age=35)
  And the biometric (thumbprint) match returns MATCH
  When the verification completes
  Then KycVerification.verificationStatus should be AUTO_APPROVED
  And the customer can proceed with account opening
```

### BDD-O01-EC-01 [EC] — US-O01, FR-6.1
```gherkin
Scenario: JPN API returns "MyKad not found"
  Given an agent initiates e-KYC for MyKad "000000000000"
  And the JPN API returns "NOT_FOUND"
  When the verification completes
  Then the system should return error code "ERR_MYKAD_NOT_FOUND"
  And KycVerification.verificationStatus should be REJECTED
  And KycVerification.rejectionReason should be "MyKad not found in JPN records"
```

### BDD-O01-EC-02 [EC] — US-O01, FR-6.1
```gherkin
Scenario: JPN API is unavailable
  Given the JPN API returns a timeout or 503 error
  When the verification is attempted
  Then the system should return error code "ERR_KYC_SERVICE_UNAVAILABLE"
  And the verification should be queued for retry
```

### BDD-O01-EC-03 [EC] — US-O01, FR-6.3
```gherkin
Scenario: Customer is under 18 — auto-reject
  Given the JPN API returns dateOfBirth "2012-05-15" (age=13)
  When the verification completes
  Then KycVerification.verificationStatus should be REJECTED
  And KycVerification.rejectionReason should be "Customer age below minimum (18)"
```

### BDD-O03 [HP] — US-O03, FR-6.3, FR-6.4
```gherkin
Scenario: KYC auto-approval decision matrix — all conditions must pass
  Given the JPN API returns:
    | field       | value         |
    | amlStatus   | CLEAN         |
    | dateOfBirth | 1990-05-15    |
  And the biometric match returns MATCH
  When the verification completes
  Then KycVerification.verificationStatus should be AUTO_APPROVED
```

### BDD-O03-EC-01 [EC] — US-O03, FR-6.3, FR-6.4
```gherkin
Scenario: AML=FLAGGED overrides biometric=MATCH — manual review
  Given the JPN API returns amlStatus "FLAGGED"
  And the biometric match returns MATCH
  And the customer age is 35
  When the verification completes
  Then KycVerification.verificationStatus should be MANUAL_REVIEW
  And the rejection reason should reference AML status
```

### BDD-O03-EC-02 [EC] — US-O03, FR-6.3
```gherkin
Scenario: Age < 18 overrides all other passing conditions — reject
  Given the JPN API returns:
    | field       | value      |
    | amlStatus   | CLEAN      |
    | dateOfBirth | 2012-05-15 |
  And the biometric match returns MATCH
  When the verification completes
  Then KycVerification.verificationStatus should be REJECTED
  And KycVerification.rejectionReason should be "Customer age below minimum (18)"
```

### BDD-O02-EC-01 [EC] — US-O02, FR-6.2, FR-6.4
```gherkin
Scenario: Biometric match fails — queue for manual review
  Given MyKad "123456789012" has been verified via JPN (AML=CLEAN, age=35)
  And the biometric match returns NO_MATCH
  When the verification completes
  Then KycVerification.verificationStatus should be MANUAL_REVIEW
  And KycVerification.biometricMatch should be NO_MATCH
  And the case should appear in backoffice manual review queue
```

### BDD-O02-EC-02 [EC] — US-O02, FR-6.4
```gherkin
Scenario: AML status is flagged — queue for manual review regardless of biometric
  Given the JPN API returns amlStatus "FLAGGED"
  And the biometric match returns MATCH
  When the verification completes
  Then KycVerification.verificationStatus should be MANUAL_REVIEW
  And KycVerification.amlStatus should be FLAGGED
```

### BDD-O02-EC-03 [EC] — US-O02, FR-6.2
```gherkin
Scenario: Biometric scanner unavailable on POS terminal
  Given the POS terminal biometric scanner returns an error
  When the verification is attempted
  Then the system should return error code "ERR_BIOMETRIC_SCANNER_UNAVAILABLE"
  And the verification should be queued for retry
```

### BDD-O02-EC-04 [EC] — US-O02, FR-6.1
```gherkin
Scenario: MyKad number format invalid
  Given an agent initiates e-KYC for MyKad "12345"
  When the verification is attempted
  Then the system should return error code "ERR_INVALID_MYKAD_FORMAT"
  And the verification should not proceed
```

### BDD-O04 [HP] — US-O04, FR-6.5 — Account Opening
```gherkin
Scenario: Account opening for new customer after KYC approval
  Given a KycVerification exists for MyKad "123456789012" with status AUTO_APPROVED
  When the agent requests account opening
  Then the system should create a new customer account
  And the account status should be ACTIVE
  And the Transaction.status should be COMPLETED
```

### BDD-O04-EC-01 [EC] — US-O04, FR-6.5
```gherkin
Scenario: Account opening fails — customer already has active account
  Given a customer with MyKad "123456789012" already has an active account
  When the agent requests account opening
  Then the system should return error code "ERR_DUPLICATE_ACCOUNT"
  And no new account should be created
```

### BDD-O05 [HP] — US-O05, FR-6.3 — Probationary Monitoring
```gherkin
Scenario: STP-approved account enters 30-day probationary period
  Given a KycVerification was AUTO_APPROVED via Conditional STP
  When the account is created
  Then the system should flag the account for "enhanced monitoring"
  And set a probationary expiry date of 30 days from approval
  And the Rules Service should apply stricter velocity limits during probation
```

---

## 7. Bill Payments

### BDD-B01 [HP] — US-B01, FR-7.1 — JomPAY Payment
```gherkin
Scenario: Successful JomPAY bill payment via cash
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides biller code "JOMPAY" and Ref-1 "TNB-12345678"
  And the Biller Service validates the account and returns outstanding RM 150.00
  And the customer hands RM 150.00 cash to the agent
  When the POS terminal confirms CASH_DEPOSIT for bill payment
  Then the system should route payment to JomPAY via PayNet
  And the Biller Service should receive payment confirmation
  And the Transaction should be created:
    | field            | value         |
    | transactionType  | BILL_PAYMENT  |
    | amount           | 150.00        |
    | status           | COMPLETED     |
  And AgentFloat.balance should be "4850.00"
```

### BDD-B01-EC-01 [EC] — US-B05, FR-7.5
```gherkin
Scenario: Bill payment — Ref-1 validation fails
  Given the customer provides biller code "JOMPAY" and Ref-1 "INVALID-REF"
  And the Biller Service returns "ACCOUNT_NOT_FOUND"
  When the POS terminal attempts bill payment
  Then the system should return error code "ERR_BILLER_REF_INVALID"
  And the Transaction.status should be FAILED
  And no money should be moved
```

### BDD-B01-EC-02 [EC] — US-B01, FR-7.1
```gherkin
Scenario: Bill payment — biller system timeout
  Given the customer provides valid biller code and Ref-1
  And the Biller Service timeout exceeds 30 seconds
  When the POS terminal attempts bill payment
  Then the system should return error code "ERR_BILLER_TIMEOUT"
  And the Transaction.status should be FAILED
  And AgentFloat should not be modified
```

### BDD-B01-EC-03 [EC] — US-B01, FR-7.1 — Insufficient Float
```gherkin
Scenario: Bill payment — insufficient agent float
  Given AgentFloat for "AGT-01" has balance "50.00"
  And the bill amount is RM 150.00
  When the POS terminal attempts bill payment
  Then the system should return error code "ERR_INSUFFICIENT_FLOAT"
  And the Transaction.status should be FAILED
```

### BDD-B02 [HP] — US-B02, FR-7.2 — ASTRO RPN Payment
```gherkin
Scenario: Successful ASTRO RPN bill payment via cash
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides biller code "ASTRO-RPN" and Ref-1 "ASTRO-98765"
  And the Biller Service validates the account and returns outstanding RM 89.00
  When the customer hands RM 89.00 cash and confirms payment
  Then the system should route payment to ASTRO
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "4911.00"
```

### BDD-B02-CARD [HP] — US-B02, FR-7.2 — Card Payment
```gherkin
Scenario: Successful ASTRO RPN bill payment via card
  Given the customer provides biller code "ASTRO-RPN" and Ref-1 "ASTRO-98765"
  And the customer presents a valid EMV card with correct PIN
  When the POS terminal confirms bill payment of RM 89.00
  Then the system should debit the card and route payment to ASTRO
  And the Transaction.status should be COMPLETED
  And AgentFloat should not be modified (card-funded transaction)
```

### BDD-B03 [HP] — US-B03, FR-7.3 — TM RPN Payment
```gherkin
Scenario: Successful TM RPN bill payment
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides biller code "TM-RPN" and Ref-1 "TM-55555"
  And the Biller Service validates and returns outstanding RM 120.00
  When the customer confirms payment with RM 120.00 cash
  Then the system should route payment to TM
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "4880.00"
```

### BDD-B04 [HP] — US-B04, FR-7.4 — EPF i-SARAAN Payment
```gherkin
Scenario: Successful EPF i-SARAAN payment via cash
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides EPF member number "EPF-112233"
  And the EPF system validates the member
  When the customer hands RM 500.00 cash and confirms contribution
  Then the system should route payment to EPF
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "4500.00"
```

### BDD-B04-EC-01 [EC] — US-B04, FR-7.4
```gherkin
Scenario: EPF payment — invalid member number
  Given the customer provides EPF member number "INVALID-EPF"
  And the EPF system returns "MEMBER_NOT_FOUND"
  When the POS terminal attempts EPF payment
  Then the system should return error code "ERR_EPF_MEMBER_INVALID"
  And the Transaction.status should be FAILED
```

---

## 8. Prepaid Top-up

### BDD-T01 [HP] — US-T01, FR-8.1 — CELCOM Top-up
```gherkin
Scenario: Successful CELCOM prepaid top-up via cash
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides phone number "0191234567"
  And the CELCOM aggregator validates the number
  When the customer hands RM 30.00 cash and confirms top-up
  Then the system should route top-up to CELCOM aggregator
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "4970.00"
```

### BDD-T01-EC-01 [EC] — US-T03, FR-8.3
```gherkin
Scenario: CELCOM top-up — invalid phone number
  Given the customer provides phone number "0199999999"
  And the CELCOM aggregator returns "INVALID_NUMBER"
  When the POS terminal attempts CELCOM top-up
  Then the system should return error code "ERR_INVALID_PHONE_NUMBER"
  And the Transaction.status should be FAILED
```

### BDD-T01-EC-02 [EC] — US-T01, FR-8.1
```gherkin
Scenario: CELCOM top-up — aggregator timeout
  Given the CELCOM aggregator does not respond within 30 seconds
  When the POS terminal attempts CELCOM top-up
  Then the system should return error code "ERR_AGGREGATOR_TIMEOUT"
  And the Transaction.status should be FAILED
  And AgentFloat should not be modified
```

### BDD-T01-CARD [HP] — US-T01, FR-8.1 — Card Top-up
```gherkin
Scenario: Successful CELCOM prepaid top-up via card
  Given the customer provides phone number "0191234567"
  And the customer presents a valid EMV card with correct PIN
  When the POS terminal confirms CELCOM top-up of RM 30.00
  Then the system should debit the card and route top-up to CELCOM
  And the Transaction.status should be COMPLETED
  And AgentFloat should not be modified (card-funded transaction)
```

### BDD-T02 [HP] — US-T02, FR-8.2 — M1 Top-up
```gherkin
Scenario: Successful M1 prepaid top-up via cash
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides phone number "0178765432"
  And the M1 aggregator validates the number
  When the customer hands RM 20.00 cash and confirms top-up
  Then the system should route top-up to M1 aggregator
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "4980.00"
```

### BDD-T02-EC-01 [EC] — US-T02, FR-8.3
```gherkin
Scenario: M1 top-up — invalid phone number
  Given the customer provides phone number "0170000000"
  And the M1 aggregator returns "INVALID_NUMBER"
  When the POS terminal attempts M1 top-up
  Then the system should return error code "ERR_INVALID_PHONE_NUMBER"
  And the Transaction.status should be FAILED
```

---

## 9. DuitNow & JomPAY

### BDD-DNOW-01 [HP] — US-D01, FR-9.1, FR-9.2 — DuitNow Transfer
```gherkin
Scenario: Successful DuitNow transfer via mobile number
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides DuitNow proxy "0123456789" (mobile number)
  And the customer hands RM 100.00 cash to the agent
  When the POS terminal requests DUITNOW_TRANSFER of RM 100.00
  Then the system should send ISO 20022 message to PayNet
  And PayNet should confirm the transfer in under 15 seconds
  And the Transaction should be created:
    | field            | value            |
    | transactionType  | DUITNOW_TRANSFER |
    | amount           | 100.00           |
    | status           | COMPLETED        |
  And AgentFloat.balance should be "4900.00"
```

### BDD-DNOW-01-EC-01 [EC] — US-D01, FR-9.1
```gherkin
Scenario: DuitNow transfer — recipient account closed
  Given the DuitNow proxy "0123456789" maps to a closed account
  When the POS terminal requests DUITNOW_TRANSFER
  Then the ISO Translation Engine should return error code "AC04"
  And the system should map to "ACCOUNT_INACTIVE"
  And the Transaction.status should be FAILED
  And AgentFloat should be restored (rollback)
```

### BDD-DNOW-01-EC-02 [EC] — US-D01, FR-9.3
```gherkin
Scenario: DuitNow transfer — network timeout at PayNet
  Given the PayNet switch does not respond within 20 seconds
  When the POS terminal requests DUITNOW_TRANSFER
  Then the system should trigger automatic reversal (Store & Forward)
  And AgentFloat should be restored
  And the Transaction.status should change to REVERSED
```

### BDD-DNOW-01-EC-03 [EC] — US-D01, FR-9.1
```gherkin
Scenario: DuitNow transfer — invalid proxy
  Given the customer provides DuitNow proxy "INVALID-PROXY"
  When the POS terminal requests DUITNOW_TRANSFER
  Then the system should return error code "ERR_INVALID_DUITNOW_PROXY"
  And the Transaction.status should be FAILED
```

### BDD-DNOW-01-NRIC [HP] — US-D01, FR-9.2 — NRIC Proxy
```gherkin
Scenario: DuitNow transfer via MyKad number
  Given the customer provides DuitNow proxy "123456789012" (NRIC)
  And the customer hands RM 50.00 cash to the agent
  When the POS terminal requests DUITNOW_TRANSFER
  Then the system should route via PayNet using NRIC proxy
  And the Transaction.status should be COMPLETED
```

### BDD-DNOW-01-BRN [HP] — US-D01, FR-9.2 — BRN Proxy
```gherkin
Scenario: DuitNow transfer via Business Registration Number
  Given the customer provides DuitNow proxy "BRN-123456789" (Business Reg)
  And the customer hands RM 200.00 cash to the agent
  When the POS terminal requests DUITNOW_TRANSFER
  Then the system should route via PayNet using BRN proxy
  And the Transaction.status should be COMPLETED
```

### BDD-DNOW-02 [HP] — US-D02, FR-7.1 — JomPAY ON-US
```gherkin
Scenario: Successful JomPAY ON-US payment (same bank)
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides JomPAY biller code "ON-US-BILLER" and Ref-1 "ACC-111"
  And the Biller Service validates the account
  When the customer hands RM 300.00 cash and confirms payment
  Then the system should process as ON-US (internal transfer)
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "4700.00"
```

### BDD-DNOW-03 [HP] — US-D02, FR-7.1 — JomPAY OFF-US
```gherkin
Scenario: Successful JomPAY OFF-US payment (cross-bank)
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides JomPAY biller code "OFF-US-TNB" and Ref-1 "TNB-555"
  And the Biller Service validates via PayNet JomPAY switch
  When the customer hands RM 250.00 cash and confirms payment
  Then the system should route via PayNet JomPAY
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "4750.00"
```

---

## 10. e-Wallet & eSSP

### BDD-WAL-01 [HP] — US-W01, FR-10.1 — Sarawak Pay Withdrawal
```gherkin
Scenario: Successful Sarawak Pay e-Wallet withdrawal via cash
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides Sarawak Pay ID "SP-123456"
  And the Sarawak Pay system validates the wallet
  When the customer hands RM 200.00 cash and confirms withdrawal
  Then the system should route withdrawal to Sarawak Pay
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "5200.00"
```

### BDD-WAL-01-EC-01 [EC] — US-W01, FR-10.1
```gherkin
Scenario: Sarawak Pay withdrawal — insufficient wallet balance
  Given the customer's Sarawak Pay wallet has only RM 50.00
  And the withdrawal amount is RM 200.00
  When the POS terminal attempts withdrawal
  Then the system should return error code "ERR_WALLET_INSUFFICIENT"
  And the Transaction.status should be FAILED
```

### BDD-WAL-01-CARD [HP] — US-W01, FR-10.1 — Card Withdrawal
```gherkin
Scenario: Successful Sarawak Pay e-Wallet withdrawal via card
  Given the customer presents a valid EMV card with correct PIN
  And the customer provides Sarawak Pay ID "SP-123456"
  When the POS terminal confirms withdrawal of RM 100.00
  Then the system should debit the card and route to Sarawak Pay
  And the Transaction.status should be COMPLETED
  And AgentFloat should not be modified (card-funded transaction)
```

### BDD-WAL-02 [HP] — US-W02, FR-10.2 — Sarawak Pay Top-up
```gherkin
Scenario: Successful Sarawak Pay e-Wallet top-up via cash
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides Sarawak Pay ID "SP-123456"
  And the Sarawak Pay system validates the wallet
  When the customer hands RM 50.00 cash and confirms top-up
  Then the system should route top-up to Sarawak Pay
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "4950.00"
```

### BDD-WAL-02-CARD [HP] — US-W02, FR-10.2 — Card Top-up
```gherkin
Scenario: Successful Sarawak Pay e-Wallet top-up via card
  Given the customer presents a valid EMV card with correct PIN
  And the customer provides Sarawak Pay ID "SP-123456"
  When the POS terminal confirms top-up of RM 50.00
  Then the system should debit the card and route to Sarawak Pay
  And the Transaction.status should be COMPLETED
```

### BDD-ESSP-01 [HP] — US-E01, FR-10.3 — eSSP Purchase
```gherkin
Scenario: Successful eSSP certificate purchase via cash
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer requests eSSP purchase of RM 100.00
  And the BSN system validates the purchase amount
  When the customer hands RM 100.00 cash
  Then the system should route purchase to BSN
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "4900.00"
  And a receipt with the eSSP certificate details should be generated
```

### BDD-ESSP-01-EC-01 [EC] — US-E01, FR-10.3
```gherkin
Scenario: eSSP purchase — BSN system unavailable
  Given the BSN system returns timeout or 503 error
  When the POS terminal attempts eSSP purchase
  Then the system should return error code "ERR_ESSP_SERVICE_UNAVAILABLE"
  And the Transaction.status should be FAILED
  And AgentFloat should not be modified
```

### BDD-ESSP-01-CARD [HP] — US-E01, FR-10.3 — Card Purchase
```gherkin
Scenario: Successful eSSP certificate purchase via card
  Given the customer presents a valid EMV card with correct PIN
  When the POS terminal confirms eSSP purchase of RM 100.00
  Then the system should debit the card and route to BSN
  And the Transaction.status should be COMPLETED
```

---
## 11. Agent Onboarding

### BDD-A01 [HP] — US-A01, FR-14.3 — Micro-Agent Self-Onboarding
```gherkin
Scenario: Successful Micro-Agent self-onboarding via Conditional STP
  Given a prospective agent submits self-onboarding via POS app
  And the OCR extracts MyKad name "AHMAD BIN ABU"
  And the SSM API returns business status "ACTIVE" and owner name "AHMAD BIN ABU"
  And the facial liveness check returns score 98% (above 95% threshold)
  And the AML watchlist screening returns "CLEAN"
  And the applicant is applying for MICRO tier with RM 5000 daily float limit
  When the Rules Engine evaluates all criteria
  Then the onboarding should be AUTO_APPROVED
  And an Agent record should be created with status ACTIVE
  And an AgentFloat should be initialized at RM 0.00
  And the time elapsed should be approximately 3 minutes
  And zero bank staff should be involved
```

### BDD-A01-EC-01 [EC] — US-A01, FR-14.3
```gherkin
Scenario: Micro-Agent self-onboarding — OCR name mismatch — falls to manual review
  Given the OCR extracts MyKad name "AHMAD BIN ABU"
  And the SSM API returns business owner name "ABU BIN AHMAD" (different)
  When the Rules Engine evaluates
  Then the onboarding should be routed to MANUAL_REVIEW
  And the case should appear in the backoffice review queue
```

### BDD-A01-EC-02 [EC] — US-A01, FR-14.3
```gherkin
Scenario: Micro-Agent self-onboarding — AML hit — falls to manual review
  Given the OCR and SSM checks pass
  And the AML watchlist screening returns "FLAGGED" (partial match)
  When the Rules Engine evaluates
  Then the onboarding should be routed to MANUAL_REVIEW
  And the AML flag reason should be recorded
```

### BDD-A01-EC-03 [EC] — US-A01, FR-14.3
```gherkin
Scenario: Micro-Agent self-onboarding — high-risk GPS zone — falls to manual review
  Given the applicant's shop GPS is in a designated high-risk zone
  And all other checks pass
  When the Rules Engine evaluates
  Then the onboarding should be routed to MANUAL_REVIEW
  And the geofence risk reason should be recorded
```

### BDD-A02 [HP] — US-A02, FR-14.4 — Standard/Premier Agent Onboarding
```gherkin
Scenario: Standard/Premier agent onboarding requires Maker-Checker
  Given a prospective agent submits application for STANDARD tier
  When the onboarding request is received
  Then the system should route to NON-STP Maker-Checker workflow
  And a field officer (Maker) must physically verify the shop premises
  And a compliance officer (Checker) must review and approve
  And the Agent status should remain PENDING until Checker approval
```

### BDD-A02-EC-01 [EC] — US-A02, FR-14.4, FR-17.6
```gherkin
Scenario: Standard agent onboarding — same user tries to be Maker and Checker
  Given user "OFFICER-01" has submitted the onboarding application as Maker
  When user "OFFICER-01" attempts to approve the same application as Checker
  Then the system should return error code "ERR_SELF_APPROVAL_PROHIBITED"
  And the application should remain PENDING_CHECKER
```

### BDD-A02-EC-02 [EC] — US-A02, FR-14.4
```gherkin
Scenario: Standard agent onboarding — Checker rejects
  Given the Maker has submitted the onboarding with physical verification evidence
  When the Checker reviews and selects REJECT with comment "Shop premises not verified"
  Then the Agent status should be REJECTED
  And the rejection reason should be recorded in AuditLog
  And no AgentFloat should be created
```

---

## 12. Reversals & Disputes

### BDD-V01 [HP] — US-V01, FR-18.1, FR-18.4 — Automatic Reversal via Temporal Saga
```gherkin
Scenario: Network timeout triggers automatic reversal via Temporal compensation handler (Store & Forward)
  Given Agent "AGT-01" has AgentFloat balance "10000.00"
  And a CASH_WITHDRAWAL of RM 500.00 is in progress via Temporal Saga
  And the Saga Workflow ID is "saga-abc123"
  And the Switch Adapter sends authorization to PayNet
  And the network times out after 25 seconds (no response received)
  When the Orchestrator detects the timeout
  Then the Temporal compensation handler should be triggered
  And the SagaStepLog should record "REVERSAL_INITIATED" with workflowId "saga-abc123"
  And the system should:
    1. Mark transaction_history status as REVERSAL_INITIATED
    2. Call Tier 3 ISO Engine with REVERSAL_REQUEST (MTI 0400)
    3. Call Ledger Service to Rollback the float lock
  And AgentFloat.balance should be restored to "10000.00"
  And the Transaction.status should change to REVERSED
```

### BDD-V01-EC-01 [EC] — US-V01, FR-18.2 — Store & Forward Retry
```gherkin
Scenario: Reversal message fails — Store & Forward retries via Temporal durable queue
  Given a reversal (MTI 0400) has been initiated via Temporal Saga
  And the SagaStepLog records "REVERSAL_REQUEST_SENT"
  And the network is down so the reversal cannot reach PayNet
  When the reversal fails to send
  Then the Temporal workflow should persist the reversal in the Store & Forward queue
  And retry the reversal every 60 seconds
  And log each attempt in the SagaStepLog with action "REVERSAL_RETRY"
  And eventually send successfully when network is restored
```

### BDD-V01-EC-02 [EC] — US-V01, FR-18.2 — Max Retries Exceeded
```gherkin
Scenario: Reversal fails after maximum retries — Saga marked as FAILED
  Given a reversal (MTI 0400) has been retried 5 times via Temporal compensation and still fails
  Then the Temporal workflow should:
    - Mark saga status as COMPENSATION_FAILED
    - Flag the Transaction for manual investigation
    - Create an AuditLog entry with action "FAIL"
    - Alert the backoffice operations team
  And the AgentFloat should remain in the rolled-back state
```

### BDD-V01-EC-03 [EC] — US-V01, FR-18.4 — No Financial Retry
```gherkin
Scenario: Financial authorization uses ZERO retries — Temporal compensation triggers immediately
  Given a CASH_WITHDRAWAL authorization is sent to PayNet via Temporal Activity
  And the network times out at 25 seconds
  When the Orchestrator processes the timeout
  Then the Temporal Activity should NOT retry the financial authorization
  And should immediately invoke the compensation handler
  And the SagaStepLog should record "COMPENSATION_TRIGGERED"
  And the AgentFloat should be released
```

### BDD-V01-ECHO [HP] — FR-18.5 — Non-Financial Retry
```gherkin
Scenario: Non-financial echo uses exponential backoff retry
  Given a network Echo/Heartbeat is sent to PayNet
  And the first attempt times out
  When the retry logic activates
  Then the system should retry with exponential backoff: 1s, 2s, 4s
  And if all 3 retries fail, alert the network monitoring team
```

### BDD-V02 [HP] — US-V02, FR-18.3 — Dispute Investigation
```gherkin
Scenario: Maker investigates double-deduction dispute
  Given a customer claims their float was debited twice for the same withdrawal
  And two Transaction records exist with the same amount and idempotency key
  When the Maker opens the dispute case
  Then the system should display both Transaction records
  And the Maker should be able to propose a manual debit reversal
  And the reason code "MANUAL_OVERRIDE" should be selected
  And evidence (transaction logs) should be attached
```

### BDD-V02-EC-01 [EC] — US-V02, FR-18.3
```gherkin
Scenario: Dispute investigation — no evidence of double-deduction
  Given a customer claims double-deduction
  But only one Transaction record exists (no duplicate found)
  When the Maker investigates
  Then the Maker should be able to mark the dispute as "UNFOUNDED"
  And the dispute status should be CANCELLED
```

### BDD-V03 [HP] — US-V03, FR-18.3 — Checker Approval
```gherkin
Scenario: Checker approves dispute resolution
  Given a Maker has proposed a manual debit reversal for dispute case
  And the reason code is "MANUAL_OVERRIDE"
  And evidence has been attached
  When the Checker reviews the case
  And the Checker selects APPROVE
  Then the Ledger Service should perform the PESSIMISTIC_WRITE update
  And the AgentFloat should be debited the reversal amount
  And the dispute case status should be RESOLVED
  And a ManualAdjustmentEntry should be created
```

### BDD-V03-EC-01 [EC] — US-V03, FR-18.3
```gherkin
Scenario: Checker rejects dispute resolution
  Given a Maker has proposed a manual debit reversal
  When the Checker reviews and selects REJECT
  And the Checker provides comment "Insufficient evidence"
  Then the dispute case should be returned to Maker's queue
  And the AgentFloat should NOT be modified
```

### BDD-V03-EC-02 [EC] — US-V03, FR-17.6
```gherkin
Scenario: Checker is the same person as Maker — rejection
  Given user "OFFICER-01" proposed the adjustment as Maker
  When user "OFFICER-01" attempts to approve as Checker
  Then the system should return error code "ERR_SELF_APPROVAL_PROHIBITED"
  And the approval should NOT proceed
```

---

## 13. Merchant Services

### BDD-M01 [HP] — US-M01, FR-15.1, FR-15.3 — Retail Sale
```gherkin
Scenario: Successful retail sale via debit card
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And a FeeConfig for RETAIL_SALE with MDR rate 1%
  And the customer purchases goods worth RM 100.00
  And the customer presents a valid debit card with correct PIN
  When the POS terminal processes RETAIL_SALE of RM 100.00
  Then the system should debit the customer account RM 100.00
  And the AgentFloat should be credited RM 99.00 (after 1% MDR)
  And the Transaction should be created:
    | field            | value        |
    | transactionType  | RETAIL_SALE  |
    | amount           | 100.00       |
    | status           | COMPLETED    |
  And a MerchantTransaction should be created:
    | field            | value        |
    | merchantType     | RETAIL_SALE  |
    | grossAmount      | 100.00       |
    | mdrRate          | 0.01         |
    | mdrAmount        | 1.00         |
    | netCreditToFloat | 99.00        |
    | receiptType      | SALES_RECEIPT |
  And AgentFloat.balance should be "5099.00"
```

### BDD-M01-EC-01 [EC] — US-M01, FR-15.1
```gherkin
Scenario: Retail sale — customer card declined
  Given the customer presents a debit card with incorrect PIN
  When the POS terminal processes RETAIL_SALE
  Then the system should return error code "ERR_INVALID_PIN"
  And the Transaction.status should be FAILED
  And AgentFloat should not be modified
```

### BDD-M01-QR [HP] — US-M01, FR-15.1 — DuitNow QR
```gherkin
Scenario: Successful retail sale via DuitNow QR
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And a FeeConfig for RETAIL_SALE with MDR rate 1.5%
  And the customer purchases goods worth RM 50.00
  And the POS terminal generates a Dynamic QR code
  And the customer scans and confirms via their banking app
  When the QR payment is confirmed
  Then the AgentFloat should be credited RM 49.25 (after 1.5% MDR)
  And the Transaction.status should be COMPLETED
  And AgentFloat.balance should be "5049.25"
```

### BDD-M01-RTP [HP] — US-M01, FR-15.1 — Request to Pay
```gherkin
Scenario: Successful retail sale via DuitNow Request-to-Pay
  Given the customer provides their mobile number "0123456789"
  And the Agent enters RM 45.00 on the POS
  When the API Gateway fires a DuitNow Request to the customer's bank
  And the customer receives a push notification and approves via FaceID
  Then the system should receive a "Cleared" confirmation
  And AgentFloat should be credited RM 44.55 (after 1% MDR)
  And the Transaction.status should be COMPLETED
```

### BDD-M02 [HP] — US-M02, FR-15.2, FR-15.3 — PIN Purchase
```gherkin
Scenario: Successful PIN purchase (digital voucher) via cash
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And a FeeConfig for PIN_PURCHASE with agent commission RM 0.50
  And the customer requests a Digi RM 10 prepaid PIN
  When the customer hands RM 10.50 cash to the agent (face value + fee)
  Then the system should debit AgentFloat RM 10.00
  And a 16-digit PIN code should be generated and printed on a slip
  And the Transaction should be created:
    | field            | value         |
    | transactionType  | PIN_PURCHASE  |
    | amount           | 10.00         |
    | agentCommission  | 0.50          |
    | status           | COMPLETED     |
  And a MerchantTransaction should be created:
    | field            | value         |
    | merchantType     | PIN_PURCHASE  |
    | grossAmount      | 10.50         |
    | netCreditToFloat | -10.00        |
    | receiptType      | PIN_SLIP      |
  And AgentFloat.balance should be "4990.00"
```

### BDD-M02-EC-01 [EC] — US-M02, FR-15.2
```gherkin
Scenario: PIN purchase — digital inventory depleted
  Given the bank's digital PIN inventory for Digi RM 10 is depleted
  When the customer requests a Digi RM 10 PIN
  Then the system should return error code "ERR_PIN_INVENTORY_DEPLETED"
  And the Transaction.status should be FAILED
  And AgentFloat should not be modified
```

### BDD-M02-CARD [HP] — US-M02, FR-15.2 — Card PIN Purchase
```gherkin
Scenario: Successful PIN purchase via card
  Given the customer presents a valid debit card with correct PIN
  When the POS terminal processes PIN_PURCHASE of RM 25.00
  Then the system should debit the customer card RM 25.50 (face value + fee)
  And a 16-digit PIN code should be generated
  And AgentFloat should be debited RM 25.00
  And the Transaction.status should be COMPLETED
```

### BDD-M03 [HP] — US-M03, FR-15.5 — Cash-Back
```gherkin
Scenario: Successful cash-back transaction (purchase + withdrawal)
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer purchases RM 20.00 of bread
  And the customer requests RM 50.00 cash back
  And the customer presents a valid debit card with correct PIN
  When the POS terminal processes the hybrid transaction (total RM 70.00)
  Then the system should:
    1. Debit customer account RM 70.00
    2. Credit AgentFloat RM 19.80 (product sale after MDR)
    3. Credit AgentFloat RM 50.00 (cash withdrawal credit)
  And the agent should hand RM 50.00 physical cash to the customer
  And the Transaction should be created:
    | field            | value          |
    | transactionType  | CASH_BACK      |
    | amount           | 70.00          |
    | status           | COMPLETED      |
  And AgentFloat.balance should be "5069.80"
```

### BDD-M03-EC-01 [EC] — US-M03, FR-15.5
```gherkin
Scenario: Cash-back — insufficient agent float for cash portion
  Given AgentFloat for "AGT-01" has balance "30.00"
  And the customer requests RM 50.00 cash back
  When the POS terminal attempts the hybrid transaction
  Then the system should return error code "ERR_INSUFFICIENT_FLOAT"
  And the Transaction.status should be FAILED
```

### BDD-M03-NET [HP] — US-M01 — Net Settlement Impact
```gherkin
Scenario: Retail sale netted against withdrawal at EOD
  Given an agent processed RM 1000 in retail sales today
  And the agent gave out RM 1000 in cash withdrawals today
  When EOD settlement is calculated
  Then the net settlement for this agent should be RM 0
  And no money movement is needed (cash recirculated)
```

---

## 14. EOD Settlement & Reconciliation

### BDD-EFM01 [HP] — US-EFM01, FR-14.5 — Smurfing Detection
```gherkin
Scenario: Velocity engine detects smurfing pattern and freezes terminal
  Given Agent "AGT-01" has processed 8 cash deposits of RM 2900.00 each in the last 5 minutes
  And the daily limit per transaction is RM 3000.00
  And the velocity rule flags structuring when > 5 transactions in 10 minutes under the limit
  When the 9th deposit of RM 2900.00 is attempted
  Then the velocity engine should detect the smurfing pattern
  And the Agent's terminal should be instantly frozen
  And the Transaction.status should be FAILED
  And the system should return error code "ERR_VELOCITY_STRUCTURING_DETECTED"
  And an alert should be sent to the compliance team
```

### BDD-EFM01-EC-01 [EC] — US-EFM01, FR-14.5
```gherkin
Scenario: Normal high-volume agent is not flagged for smurfing
  Given Agent "AGT-02" (PREMIER) has processed 20 transactions of RM 500.00 each over 8 hours
  And all transactions are legitimate
  When the velocity engine evaluates
  Then the pattern should NOT be flagged as smurfing
  And the Agent should remain ACTIVE
```

### BDD-EFM02 [HP] — US-EFM02, FR-14.4 — Checker Review
```gherkin
Scenario: Compliance Officer reviews frozen terminal
  Given Agent "AGT-01" terminal is frozen due to smurfing detection
  When the Compliance Officer (Checker) reviews the terminal logs
  And confirms the pattern is legitimate (e.g., rural high-volume area)
  And selects UNFREEZE
  Then the Agent status should change back to ACTIVE
  And the terminal should be operational again
  And an AuditLog entry should be created
```

### BDD-EFM02-EC-01 [EC] — US-EFM02, FR-14.4
```gherkin
Scenario: Compliance Officer confirms smurfing and files STR
  Given Agent "AGT-01" terminal is frozen due to smurfing detection
  When the Compliance Officer reviews and confirms smurfing
  And selects "FILE_STR" (Suspicious Transaction Report)
  Then the system should generate an STR for BNM submission
  And the Agent status should remain FROZEN
  And the Agent should be DEACTIVATED
```

### BDD-EFM03 [HP] — NFR-4.2 — Geofencing
```gherkin
Scenario: Transaction outside geofence — auto-decline
  Given Agent "AGT-01" is registered at GPS (3.1390, 101.6869)
  And the POS terminal reports GPS (3.2000, 101.7000) — 7km away
  When any transaction is attempted
  Then the system should return error code "ERR_GEOFENCE_VIOLATION"
  And the terminal should be locked
```

### BDD-EFM04 [HP] — US-R03, FR-1.3 — Per-NRIC Velocity
```gherkin
Scenario: Customer exceeds per-NRIC daily transaction limit
  Given customer MyKad "123456789012" has reached the max 5 transactions today
  When a 6th transaction is initiated for this MyKad
  Then the velocity check should fail
  And the system should return error code "ERR_VELOCITY_COUNT_EXCEEDED"
  And the Transaction.status should be FAILED
```

### BDD-EFM04-EC-01 [EC] — US-R03, FR-1.3
```gherkin
Scenario: Customer exceeds per-NRIC daily amount limit
  Given customer MyKad "123456789012" has transactions totaling RM 24,000 today
  And the max amount per day is RM 25,000
  When a transaction of RM 2,000 is initiated
  Then the velocity check should fail
  And the system should return error code "ERR_VELOCITY_AMOUNT_EXCEEDED"
```

### BDD-SM01 [HP] — US-SM01, FR-16.1 — Positive Settlement
```gherkin
Scenario: EOD net settlement — Bank owes Agent (positive)
  Given Agent "AGT-01" has the following daily activity:
    | transactionType | totalAmount |
    | CASH_WITHDRAWAL | 10000.00    |
    | CASH_DEPOSIT    | 3000.00     |
    | BILL_PAYMENT    | 2000.00     |
    | COMMISSION      | 500.00      |
  When the EOD settlement job runs at 23:59:59 MYT
  Then the net settlement calculation should be:
    | field                | value     |
    | totalWithdrawals     | 10000.00  |
    | totalDeposits        | 3000.00   |
    | totalBillPayments    | 2000.00   |
    | totalCommissions     | 500.00    |
    | netSettlementAmount  | 5500.00   |
    | settlementDirection  | BANK_OWES_AGENT |
  And a SettlementSummary should be created with status PENDING
```

### BDD-SM01-EC-01 [EC] — US-SM01, FR-16.2 — Negative Settlement
```gherkin
Scenario: EOD net settlement — Agent owes Bank (negative)
  Given Agent "AGT-02" has the following daily activity:
    | transactionType | totalAmount |
    | CASH_WITHDRAWAL | 2000.00     |
    | CASH_DEPOSIT    | 8000.00     |
    | BILL_PAYMENT    | 3000.00     |
    | COMMISSION      | 100.00      |
  When the EOD settlement job runs
  Then the net settlement calculation should be:
    | field                | value     |
    | netSettlementAmount  | -8900.00  |
    | settlementDirection  | AGENT_OWES_BANK |
```

### BDD-SM01-EC-02 [EC] — US-SM01, FR-16.4 — Pending Transactions
```gherkin
Scenario: EOD settlement — pending transactions block settlement
  Given Agent "AGT-01" has 1 transaction in PENDING status
  When the EOD settlement job runs
  Then the settlement should be PAUSED for this agent
  And the SettlementSummary.status should be HELD
  And the settlement should proceed once all transactions are finalized
```

### BDD-SM01-EC-03 [EC] — US-SM01, FR-16.5 — Float Top-ups Excluded
```gherkin
Scenario: EOD settlement — float top-ups excluded from calculation
  Given Agent "AGT-01" topped up their float by RM 5000.00 today via DuitNow
  And the agent processed RM 3000.00 in withdrawals
  And the agent processed RM 1000.00 in deposits
  When the EOD settlement job runs
  Then the float top-up of RM 5000.00 should be excluded
  And the net settlement should be: (3000 + commissions) - 1000
```

### BDD-SM01-MER [HP] — US-SM01 — Retail Sales in Settlement
```gherkin
Scenario: EOD settlement includes retail sales
  Given Agent "AGT-01" has the following daily activity:
    | transactionType | totalAmount |
    | CASH_WITHDRAWAL | 5000.00     |
    | RETAIL_SALE     | 2000.00     |
    | CASH_DEPOSIT    | 1000.00     |
    | COMMISSION      | 200.00      |
  When the EOD settlement job runs
  Then the net settlement should be:
    | field                | value     |
    | totalRetailSales     | 2000.00   |
    | netSettlementAmount  | 6200.00   |
    | settlementDirection  | BANK_OWES_AGENT |
```

### BDD-SM02 [HP] — US-SM02, FR-16.3 — CBS File Generation
```gherkin
Scenario: Settlement file generated for CBS upload
  Given SettlementSummary for Agent "AGT-01" is PENDING
  When the settlement file generation runs at 02:00 AM
  Then a CSV file should be generated with format:
    | field              | value     |
    | agentId            | AGT-01    |
    | businessDate       | 2026-03-26 |
    | netSettlementAmount | 5500.00  |
    | direction          | CREDIT    |
  And the file should be placed at /sftp/cbs/outbound/settlement_20260326.csv
  And SettlementSummary.cbsFileGenerated should be TRUE
```

### BDD-SM02-EC-01 [EC] — US-SM02, FR-16.3
```gherkin
Scenario: Settlement file generation — no transactions for agent
  Given Agent "AGT-03" has zero transactions today
  When the EOD settlement job runs
  Then no settlement file entry should be generated for this agent
  And the SettlementSummary should not be created
```

### BDD-SM03 [HP] — US-SM03, FR-17.1 — Reconciliation Success
```gherkin
Scenario: Successful reconciliation — all transactions match
  Given the internal ledger has 1000 successful transactions for today
  And the PayNet PSR file has the same 1000 transactions
  When the EOD reconciliation script runs
  Then all 1000 transactions should match (Triple-Match Logic)
  And no discrepancies should be found
  And the reconciliation log should show "SUCCESSFUL"
```

### BDD-SM03-EC-01 [EC] — US-SM03, FR-17.2 — Ghost Transaction
```gherkin
Scenario: Reconciliation detects Ghost transaction
  Given the internal ledger has a CASH_WITHDRAWAL of RM 500.00 with status SUCCESS
  And the PayNet PSR file does NOT contain this transaction
  When the EOD reconciliation script runs
  Then the transaction should be categorized as GHOST
  And a DiscrepancyCase should be created:
    | field             | value     |
    | discrepancyType   | GHOST     |
    | internalStatus    | SUCCESS   |
    | paynetStatus      | MISSING   |
    | status            | PENDING_MAKER |
```

### BDD-SM03-EC-02 [EC] — US-SM03, FR-17.2 — Orphan Transaction
```gherkin
Scenario: Reconciliation detects Orphan transaction
  Given the internal ledger does NOT have transaction "TXN-999"
  And the PayNet PSR file shows transaction "TXN-999" with status SUCCESS
  When the EOD reconciliation script runs
  Then the transaction should be categorized as ORPHAN
  And a DiscrepancyCase should be created:
    | field             | value     |
    | discrepancyType   | ORPHAN    |
    | internalStatus    | MISSING   |
    | paynetStatus      | SUCCESS   |
    | status            | PENDING_MAKER |
```

### BDD-SM03-EC-03 [EC] — US-SM03, FR-17.2 — Mismatch Transaction
```gherkin
Scenario: Reconciliation detects amount mismatch
  Given the internal ledger has a transaction with amount RM 100.00
  And the PayNet PSR file has the same transaction with amount RM 100.01
  When the EOD reconciliation script runs
  Then the transaction should be categorized as MISMATCH
  And a DiscrepancyCase should be created with discrepancyType MISMATCH
  And the settlement for this agent should be PAUSED
```

### BDD-SM04 [HP] — US-SM04, FR-17.3 — Settlement Paused for Discrepancy
```gherkin
Scenario: Settlement paused for agent with unresolved discrepancy
  Given Agent "AGT-01" has 99 clean transactions and 1 discrepancy
  When the EOD settlement job runs
  Then the system should settle the 99 clean transactions
  And HOLD the value of the 1 discrepancy
  And SettlementSummary.status should be HELD
  And the agent's settlement should proceed once the discrepancy is resolved
```

### BDD-SM04-EC-01 [EC] — US-SM04, FR-17.8 — Resolution Deadline
```gherkin
Scenario: Discrepancy resolution deadline reminder
  Given a DiscrepancyCase has been in PENDING_MAKER status since yesterday
  When the time reaches 10:00 AM the next business day
  Then the system should send an escalation alert to the supervisor
  And the case status should remain PENDING_MAKER until resolved
```
## 15. Discrepancy Resolution

### BDD-DR01 [HP] — US-DR01, FR-17.4, FR-17.7 — Maker Investigation
```gherkin
Scenario: Maker investigates and proposes Ghost transaction resolution
  Given a DiscrepancyCase exists:
    | field             | value           |
    | caseId            | CASE-001        |
    | discrepancyType   | GHOST           |
    | internalStatus    | SUCCESS         |
    | paynetStatus      | MISSING         |
    | status            | PENDING_MAKER   |
  When the Maker opens the case
  And reviews the terminal journal and Spring Boot logs
  And selects makerAction "FORCE_REVERSE"
  And selects reasonCode "NETWORK_FAILURE"
  And attaches evidence (PayNet PSR screenshot)
  Then the DiscrepancyCase should be updated:
    | field       | value              |
    | makerId     | OFFICER-01         |
    | makerAction | FORCE_REVERSE      |
    | reasonCode  | NETWORK_FAILURE    |
    | status      | PENDING_CHECKER    |
```

### BDD-DR01-EC-01 [EC] — US-DR01, FR-17.4
```gherkin
Scenario: Maker tries to submit without reason code — rejected
  Given a DiscrepancyCase is in PENDING_MAKER status
  When the Maker attempts to submit without selecting a reason code
  Then the system should return error code "ERR_REASON_CODE_REQUIRED"
  And the submission should be rejected
```

### BDD-DR01-EC-02 [EC] — US-DR01, FR-17.4
```gherkin
Scenario: Maker tries to submit without evidence — rejected
  Given a DiscrepancyCase is in PENDING_MAKER status
  When the Maker attempts to submit without attaching evidence
  Then the system should return error code "ERR_EVIDENCE_REQUIRED"
  And the submission should be rejected
```

### BDD-DR02 [HP] — US-DR02, FR-17.5, FR-17.7 — Checker Approval
```gherkin
Scenario: Checker approves Ghost transaction resolution
  Given a DiscrepancyCase is in PENDING_CHECKER status
  And the Maker proposed FORCE_REVERSE with reason NETWORK_FAILURE
  When the Checker reviews the case
  And compares the internal ledger with the PayNet PSR
  And selects APPROVE
  Then the Ledger Service should perform the manual adjustment:
    | field            | value          |
    | adjustmentType   | DEBIT_AGENT    |
    | amount           | 500.00         |
    | accountCode      | AGT_FLOAT      |
  And a ManualAdjustmentEntry should be created
  And the DiscrepancyCase.status should be RESOLVED
  And an AuditLog entry should be created with action PROCESS
```

### BDD-DR02-ORPHAN [HP] — US-DR02, FR-17.5 — Orphan Resolution
```gherkin
Scenario: Checker approves Orphan transaction resolution
  Given a DiscrepancyCase with discrepancyType ORPHAN
  And the Maker proposed FORCE_SUCCESS
  When the Checker selects APPROVE
  Then the Ledger Service should credit the AgentFloat
  And the DiscrepancyCase.status should be RESOLVED
```

### BDD-DR02-EC-01 [EC] — US-DR02, FR-17.5 — Checker Rejects
```gherkin
Scenario: Checker rejects resolution — insufficient evidence
  Given a DiscrepancyCase is in PENDING_CHECKER status
  When the Checker selects REJECT
  And provides comment "Evidence does not match PayNet report"
  Then the DiscrepancyCase should be returned to PENDING_MAKER
  And the Checker comment should be visible to the Maker
  And no Ledger adjustment should occur
```

### BDD-DR03 [HP] — US-DR03, FR-17.6 — Four-Eyes Principle
```gherkin
Scenario: Four-Eyes Principle enforced at system level
  Given a DiscrepancyCase was proposed by Maker "OFFICER-01"
  When user "OFFICER-01" attempts to approve as Checker
  Then the system should return error code "ERR_SELF_APPROVAL_PROHIBITED"
  And the approval should NOT proceed
  And the DiscrepancyCase should remain PENDING_CHECKER
```

### BDD-DR03-EC-01 [EC] — US-DR03, FR-17.6
```gherkin
Scenario: Different user from same department can approve
  Given a DiscrepancyCase was proposed by Maker "OFFICER-01"
  When user "OFFICER-02" (different user, same department) attempts to approve
  Then the approval should proceed successfully
  And the Ledger adjustment should be applied
```

### BDD-DR01-MISMATCH [HP] — US-DR01 — Mismatch Resolution
```gherkin
Scenario: Maker resolves Mismatch discrepancy
  Given a DiscrepancyCase with discrepancyType MISMATCH
  And internalAmount is RM 100.00
  And paynetAmount is RM 100.01
  When the Maker reviews and selects makerAction "FORCE_SUCCESS"
  And selects reasonCode "MANUAL_OVERRIDE"
  Then the system should update the internal record to RM 100.01
  And the DiscrepancyCase should move to PENDING_CHECKER
```

---

## 16. API Gateway & Backoffice

### BDD-G01 [HP] — US-G01, FR-12.1
```gherkin
Scenario: Gateway routes POS request to correct service
  Given the POS terminal sends a POST to "/api/v1/withdrawal"
  And the request contains valid authentication token
  When the Gateway receives the request
  Then it should route to the Ledger & Float Service
  And the response should be returned to the POS terminal
```

### BDD-G02 [HP] — US-G02, FR-12.2
```gherkin
Scenario: Gateway authenticates external API request
  Given the POS terminal sends a request with a valid Bearer token
  When the Gateway receives the request
  Then the token should be validated
  And the request should be forwarded to the backend service
  And the agentId should be extracted from the token claims
```

### BDD-G01-EC-01 [EC] — US-G02, FR-12.2
```gherkin
Scenario: Gateway rejects request with expired token
  Given the POS terminal sends a request with an expired Bearer token
  When the Gateway receives the request
  Then the Gateway should return HTTP 401
  And the response body should be:
    """
    { "status": "FAILED", "error": { "code": "ERR_TOKEN_EXPIRED" } }
    """
  And the request should NOT be forwarded to any backend service
```

### BDD-G01-EC-02 [EC] — US-G02, FR-12.2
```gherkin
Scenario: Gateway rejects request with no authentication
  Given the POS terminal sends a request without any Authorization header
  When the Gateway receives the request
  Then the Gateway should return HTTP 401
  And the response should be:
    """
    { "status": "FAILED", "error": { "code": "ERR_MISSING_TOKEN" } }
    """
```

### BDD-G01-EC-03 [EC] — US-G01, NFR-2.2
```gherkin
Scenario: Gateway returns 503 when backend service is down
  Given the Ledger & Float Service is unavailable (circuit breaker open)
  When the POS terminal sends a withdrawal request
  Then the Gateway should return HTTP 503
  And the response should be:
    """
    { "status": "FAILED", "error": { "code": "ERR_SERVICE_UNAVAILABLE" } }
    """
```

### BDD-G01-EC-04 [EC] — US-G01, FR-14.5
```gherkin
Scenario: Gateway rate limits excessive requests from same terminal
  Given the POS terminal sends 6 withdrawal requests within 1 hour
  And the rate limit is 5 transactions per hour
  When the 6th request is received
  Then the Gateway should return HTTP 429
  And the response should be:
    """
    { "status": "FAILED", "error": { "code": "ERR_RATE_LIMIT_EXCEEDED" } }
    """
```

### BDD-G01-EC-05 [EC] — US-G01
```gherkin
Scenario: Gateway validates request body against OpenAPI spec
  Given the POS terminal sends a request with invalid JSON body
  When the Gateway validates against the OpenAPI schema
  Then the Gateway should return HTTP 400
  And the response should include validation error details
```


## 17. STP Processing

### BDD-S01 [HP] — US-S01, FR-14.1, FR-14.2 — 100% STP
```gherkin
Scenario: Bill payment processes with 100% STP (zero intervention)
  Given a BILL_PAYMENT transaction is initiated
  And the transaction type is classified as 100% STP
  And all system validations pass (float, limits, velocity, geofence)
  When the transaction is processed
  Then the system should complete the entire flow without human intervention
  And the Transaction.status should be COMPLETED
  And no manual review queue should be involved
```

### BDD-S01-EC-01 [EC] — US-S01, FR-14.5
```gherkin
Scenario: 100% STP transaction fails velocity check — auto-decline
  Given a CASH_WITHDRAWAL transaction is classified as 100% STP
  And the velocity engine detects the customer has exceeded the daily limit
  When the transaction is processed
  Then the system should instantly hard-decline
  And the system should return error code "ERR_VELOCITY_COUNT_EXCEEDED"
  And the Transaction.status should be FAILED
  And no manual review queue should be involved
```

### BDD-S02 [HP] — US-S02, FR-14.1, FR-14.3 — Conditional STP Auto-Approve
```gherkin
Scenario: Conditional STP auto-approves when all criteria pass
  Given a KYC verification is classified as Conditional STP
  And the decision matrix evaluates:
    | criterion             | result  |
    | OCR name match (100%) | PASS    |
    | Liveness score (> 95%)| PASS    |
    | AML watchlist         | CLEAN   |
    | GPS zone              | LOW_RISK|
  When the Rules Engine evaluates
  Then the verification should be AUTO_APPROVED
  And the time elapsed should be approximately 3 minutes
  And zero bank staff should be involved
```

### BDD-S02-EC-01 [EC] — US-S02, FR-14.3 — Conditional STP Fallback
```gherkin
Scenario: Conditional STP falls to manual review when criteria fail
  Given a KYC verification is classified as Conditional STP
  And the decision matrix evaluates:
    | criterion             | result        |
    | OCR name match (100%) | PASS          |
    | Liveness score (> 95%)| FAIL (blurry) |
    | AML watchlist         | CLEAN         |
  When the Rules Engine evaluates
  Then the verification should be routed to MANUAL_REVIEW
  And the case should appear in the backoffice review queue
  And the reason should be "Liveness check failed"
```

### BDD-S03 [HP] — US-S03, FR-14.1, FR-14.4 — Non-STP Maker-Checker
```gherkin
Scenario: Super-agent onboarding requires Non-STP Maker-Checker
  Given a super-agent application is submitted for PREMIER tier
  And the transaction type is classified as Non-STP
  When the onboarding request is received
  Then the system should route to Maker-Checker workflow
  And a Maker must review physical evidence
  And a separate Checker must approve
  And the Agent status should remain PENDING until Checker approval
```

### BDD-S03-EC-01 [EC] — US-S03, FR-14.4, FR-17.6
```gherkin
Scenario: Non-STP enforces Four-Eyes Principle
  Given a Non-STP application is in PENDING_CHECKER status
  And the Maker was user "OFFICER-01"
  When user "OFFICER-01" attempts to approve as Checker
  Then the system should return error code "ERR_SELF_APPROVAL_PROHIBITED"
  And the approval should NOT proceed
```

### BDD-S03-EC-02 [EC] — US-S03, FR-14.4
```gherkin
Scenario: Non-STP SLA queue escalation
  Given a Non-STP case has been in PENDING_CHECKER for 24 hours
  When the SLA deadline is reached
  Then the system should automatically escalate to a supervisor
  And an alert notification should be sent
  And the case status should remain PENDING_CHECKER until resolved
```

---

## 18. Translation Layer — Tier 4

### BDD-ISO01 [HP] — US-ISO01, FR-T4.1
```gherkin
Scenario: ISO Translation Engine converts internal JSON to ISO 8583 MTI 0100
  Given the Transaction Orchestrator sends a CASH_WITHDRAWAL request with amount RM 500.00
  When the ISO Adapter receives the request
  Then the ISO Translation Engine should generate an ISO 8583 MTI 0100 message
  And field DE-3 (Processing Code) should be "00" ( withdrawal)
  And field DE-4 (Amount Transaction) should be "000000000500"
  And field DE-7 (Transmission Date & Time) should be the current timestamp
  And field DE-11 (Systems Trace Audit Number) should be generated
  And the message should be sent via persistent TCP socket to PayNet
```

### BDD-ISO01-EC-01 [EC] — US-ISO01, FR-T4.1
```gherkin
Scenario: ISO message encoding fails — invalid field data
  Given the Transaction Orchestrator sends a CASH_WITHDRAWAL with invalid card data
  When the ISO Adapter attempts to encode the ISO 8583 message
  Then the ISO Translation Engine should return error code "ERR_ISO_ENCODING_FAILED"
  And the Transaction Orchestrator should receive a failed response
  And no TCP message should be sent to PayNet
```

### BDD-ISO01-EC-02 [EC] — US-ISO01, FR-T4.1
```gherkin
Scenario: TCP connection to PayNet is down — connection pooling retries
  Given the TCP connection to PayNet is unavailable
  When the ISO Adapter attempts to send MTI 0100
  Then the ISO Adapter should retry using a secondary TCP connection from the pool
  And after 3 failed attempts, return error code "ERR_SWITCH_CONNECTION_FAILED"
  And the Saga should trigger Temporal compensation
```

### BDD-ISO02 [HP] — US-ISO01, FR-T4.1
```gherkin
Scenario: ISO Translation Engine parses ISO 8583 MTI 0110 response
  Given PayNet sends an ISO 8583 MTI 0110 response:
    | field | value              |
    | DE-39 | "00" (Approved)    |
    | DE-3  | "000000000500"     |
  When the ISO Adapter receives the response
  Then the ISO Translation Engine should parse the message
  And the Transaction Orchestrator should receive:
    | field           | value    |
    | responseCode    | "00"     |
    | authorized       | true     |
    | stan            | [from DE-11] |
```

### BDD-ISO02-EC-01 [EC] — US-ISO01, FR-T4.1
```gherkin
Scenario: ISO response DE-39 is decline code
  Given PayNet sends MTI 0110 with DE-39 = "05" (Do Not Honor)
  When the ISO Translation Engine parses the response
  Then the Transaction Orchestrator should receive responseCode "05"
  And the Transaction.status should be FAILED
  And the Saga should NOT execute compensation (no debit occurred)
```

### BDD-ISO03 [HP] — US-ISO01, FR-T4.1
```gherkin
Scenario: ISO Translation Engine generates MTI 0400 Reversal
  Given the Transaction Orchestrator requests a reversal for transaction "TXN-001"
  When the ISO Adapter processes the reversal request
  Then the ISO Translation Engine should generate MTI 0400 message
  And field DE-37 (Retrieval Reference Number) should reference the original transaction
  And the message should be sent via TCP to PayNet
  And the SagaStepLog should record the reversal attempt
```

### BDD-ISO03-EC-01 [EC] — US-ISO01, FR-T4.1
```gherkin
Scenario: MTI 0400 Reversal response indicates original transaction not found
  Given PayNet sends MTI 0410 with DE-39 = "25" (Record Not Found)
  When the ISO Translation Engine parses the response
  Then the system should log the response
  And the DiscrepancyCase should be created for manual review
  And the Saga should be marked as REQUIRES_MANUAL_REVIEW
```

### BDD-CBS01 [HP] — US-CBS01, FR-T4.2
```gherkin
Scenario: CBS Connector credits customer account
  Given the Transaction Orchestrator requests credit of RM 500.00 to account "ACC-123"
  When the CBS Adapter processes the request
  Then the CBS Connector should translate to CBS protocol (SOAP/MQ)
  And the CBS should credit the account
  And the CBS Adapter should return success response
```

### BDD-CBS01-EC-01 [EC] — US-CBS01, FR-T4.2
```gherkin
Scenario: CBS Connector times out
  Given the CBS system does not respond within 30 seconds
  When the CBS Adapter processes the credit request
  Then the system should return error code "ERR_EXT_101"
  And the Saga should trigger compensation (reverse float debit)
```

### BDD-CBS02 [HP] — US-CBS01, FR-T4.2
```gherkin
Scenario: CBS Connector performs account inquiry
  Given the Transaction Orchestrator requests account inquiry for "ACC-123"
  When the CBS Adapter processes the request
  Then the CBS Connector should query CBS
  And return account details including balance and status
```

### BDD-CBS02-EC-01 [EC] — US-CBS01, FR-T4.2
```gherkin
Scenario: CBS account inquiry — account not found
  Given the CBS system returns "ACCOUNT_NOT_FOUND"
  When the CBS Adapter processes the inquiry
  Then the system should return error code "ERR_VAL_005"
  And the Transaction.status should be FAILED
```

### BDD-HSM01 [HP] — US-HSM01, FR-T4.3
```gherkin
Scenario: HSM Wrapper verifies PIN for card transaction
  Given a customer presents card "411111******1111" with PIN "123456"
  When the POS terminal requests PIN verification
  Then the HSM Adapter should generate PIN block (ISO 9564 Format 0)
  And the HSM Wrapper should send to Thales HSM for verification
  And the HSM should return MATCH or NO_MATCH
  And the card number should NEVER appear in application logs (HSM boundary)
```

### BDD-HSM01-EC-01 [EC] — US-HSM01, FR-T4.3
```gherkin
Scenario: HSM Wrapper — PIN block generation fails
  Given the HSM service returns an error during PIN block generation
  When the POS terminal requests PIN verification
  Then the system should return error code "ERR_HSM_001"
  And the Transaction.status should be FAILED
```

### BDD-HSM02 [HP] — US-HSM01, FR-T4.3
```gherkin
Scenario: HSM Wrapper generates random 6-digit PIN for new card
  Given the CBS Connector creates a new customer account
  When the Onboarding Service requests PIN generation
  Then the HSM Adapter should generate a random 6-digit PIN
  And the PIN should be encrypted by HSM
  And the encrypted PIN block should be stored in the HSM (never in application DB)
  And the PIN should be sent via SMS to the customer
```

### BDD-HSM02-EC-01 [EC] — US-HSM01, FR-T4.3
```gherkin
Scenario: HSM Wrapper — HSM hardware failure
  Given the Thales HSM returns a hardware failure response
  When the HSM Adapter processes PIN operations
  Then the system should return error code "ERR_HSM_002"
  And the Transaction Orchestrator should queue the operation for retry
  And the Saga should pause until HSM recovers
```

### BDD-BG01 [HP] — US-BG01, FR-T4.4
```gherkin
Scenario: Biller Gateway processes JomPAY payment
  Given the Transaction Orchestrator sends JomPAY request:
    | field       | value             |
    | billerCode  | "JOMPAY"         |
    | ref1        | "TNB-12345678"   |
    | amount      | 150.00           |
  When the Biller Adapter processes the request
  Then the Biller Gateway should normalize the request per JomPAY specification
  And route to the appropriate biller API (TNB)
  And return confirmation
```

### BDD-BG01-EC-01 [EC] — US-BG01, FR-T4.4
```gherkin
Scenario: Biller Gateway — biller API timeout
  Given the TNB biller API does not respond within 30 seconds
  When the Biller Adapter processes the JomPAY request
  Then the system should return error code "ERR_EXT_201"
  And the Saga should trigger compensation (restore float)
```

### BDD-BG01-EC-02 [EC] — US-BG01, FR-T4.4
```gherkin
Scenario: Biller Gateway — unknown biller code
  Given the Biller Adapter receives billerCode "UNKNOWN-BILLER"
  When the Biller Gateway normalizes the request
  Then the system should return error code "ERR_VAL_006"
  And no biller API call should be made
```

### BDD-BG02 [HP] — US-BG01, FR-T4.4
```gherkin
Scenario: Biller Gateway retrieves bill details before payment
  Given the customer provides billerCode "TNB" and ref1 "TNB-12345678"
  When the Biller Adapter requests bill inquiry
  Then the Biller Gateway should call TNB API
  And return bill details:
    | field           | value        |
    | billerName      | "Tenaga Nasional" |
    | customerName    | "AHMAD BIN ABU" |
    | outstandingAmount | 150.00     |
  And the agent should confirm the amount before payment
```

### BDD-EM01 [HP] — US-EM01, FR-T4.5
```gherkin
Scenario: Tier 4 normalizes ISO 8583 response codes to business exceptions
  Given PayNet sends ISO 8583 response with DE-39 = "14" (Invalid Card Number)
  When the ISO Adapter processes the response
  Then the Error Mapper should translate DE-39 "14" to "ERR_AUTH_003"
  And the Transaction Orchestrator should receive a business exception
  And the Saga should handle the exception appropriately
```

### BDD-EM01-EC-01 [EC] — US-EM01, FR-T4.5
```gherkin
Scenario: Tier 4 normalizes CBS SOAP fault to business exception
  Given the CBS system returns SOAP fault "AccountLocked"
  When the CBS Adapter processes the fault
  Then the Error Mapper should translate to "ERR_AUTH_004"
  And the Transaction Orchestrator should handle appropriately
```

### BDD-EM02 [HP] — US-EM01, FR-T4.5
```gherkin
Scenario: Tier 4 normalizes HSM error to business exception
  Given the Thales HSM returns error code "HSM-05" (Key Not Found)
  When the HSM Adapter processes the error
  Then the Error Mapper should translate to "ERR_SYS_002"
  And the Transaction Orchestrator should log and alert
```
## 19. Traceability Matrix

### Scenario → User Story → Functional Requirement

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|

#### Transaction Orchestrator (Temporal Saga)

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-TO01 | HP | US-TO01, US-TO02 | FR-19.1, FR-19.2, FR-19.3, FR-19.4 |
| BDD-TO01-EC-01 | EC | US-TO01 | FR-19.1, FR-19.6 |
| BDD-TO01-EC-02 | EC | US-TO01 | FR-19.3, FR-19.4 |
| BDD-TO01-EC-03 | EC | US-TO02 | FR-19.3 |
| BDD-TO01-EC-04 | EC | US-TO01 | FR-19.7 |

#### Rules & Fee Engine

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-R01 | HP | US-R01 | FR-1.1, FR-1.4 |
| BDD-R01-PCT | HP | US-R01 | FR-1.1 |
| BDD-R01-EC-01 | EC | US-R01 | FR-1.1 |
| BDD-R01-EC-02 | EC | US-R01 | FR-1.1 |
| BDD-R01-EC-03 | EC | US-R01 | FR-1.4 |
| BDD-R04 | HP | US-R04 | FR-1.4 |
| BDD-R02 | HP | US-R02 | FR-1.2 |
| BDD-R02-EC-01 | EC | US-R02 | FR-1.2 |
| BDD-R02-EC-02 | EC | US-R02 | FR-1.2 |
| BDD-R02-EC-03 | EC | US-R02 | FR-1.2 |
| BDD-R02-EC-04 | EC | US-R02 | FR-1.2 |
| BDD-R03 | HP | US-R03 | FR-1.3 |
| BDD-R03-EC-01 | EC | US-R03 | FR-1.3 |
| BDD-R03-EC-02 | EC | US-R03 | FR-1.3 |
| BDD-R03-EC-03 | EC | US-R03 | FR-1.3 |
| BDD-R03-EC-04 | EC | US-R03 | FR-1.3 |

#### Ledger & Float Service

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-L01 | HP | US-L01 | FR-2.1, FR-5.2 |
| BDD-L01-EC-01 | EC | US-L01 | FR-2.1 |
| BDD-L01-EC-02 | EC | US-L01 | FR-2.1 |
| BDD-L02 | HP | US-L02 | FR-2.2 |
| BDD-L02-DC | HP | US-L02 | FR-2.2 |
| BDD-L02-MER | HP | US-L02 | FR-2.2 |
| BDD-L02-PIN | HP | US-L02 | FR-2.2 |
| BDD-L03 | HP | US-L03 | FR-2.1, FR-2.3 |
| BDD-L03-EC-01 | EC | US-L03 | FR-2.1, FR-2.3 |
| BDD-L03-EC-02 | EC | US-L03 | FR-2.3 |
| BDD-L04 | HP | US-L04 | FR-5.1 |
| BDD-L04-EC-01 | EC | US-L04 | FR-5.1 |
| BDD-L04-EC-02 | EC | US-L04 | FR-2.4 |

#### Cash Withdrawal

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-W01 | HP | US-L05 | FR-2.4, FR-3.1, FR-3.3, FR-3.5 |
| BDD-W01-EC-01 | EC | US-L05 | FR-3.1 |
| BDD-W01-EC-02 | EC | US-L05 | FR-3.4 |
| BDD-W01-EC-03 | EC | US-L05 | FR-3.4, FR-18.1 |
| BDD-W01-EC-04 | EC | US-L05 | FR-3.3 |
| BDD-W01-EC-05 | EC | US-L05 | FR-3.4 |
| BDD-W01-EC-06 | EC | US-L05 | FR-3.4 |
| BDD-W01-EC-07 | EC | US-L05 | FR-2.4 |
| BDD-W01-EC-08 | EC | US-L05 | FR-3.4, FR-18.2 |
| BDD-W01-SMS | HP | US-L05 | FR-3.1 |
| BDD-W02 | HP | US-L06 | FR-3.2 |
| BDD-W02-EC-01 | EC | US-L06 | FR-3.2 |

#### Cash Deposit

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-D01 | HP | US-L07 | FR-2.5, FR-4.1, FR-4.3 |
| BDD-D01-EC-01 | EC | US-L07 | FR-2.5 |
| BDD-D01-EC-02 | EC | US-L07 | FR-4.1 |
| BDD-D01-EC-03 | EC | US-L07 | FR-4.1 |
| BDD-D01-EC-04 | EC | US-L07 | FR-2.1 |
| BDD-D01-NIC | HP | US-L07 | FR-4.1 |
| BDD-D01-BIO | HP | US-L07 | FR-4.1 |
| BDD-D02 | HP | US-L08 | FR-4.2 |
| BDD-D02-EC-01 | EC | US-L08 | FR-4.2 |

#### e-KYC & Onboarding

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-O01 | HP | US-O01 | FR-6.1 |
| BDD-O01-EC-01 | EC | US-O01 | FR-6.1 |
| BDD-O01-EC-02 | EC | US-O01 | FR-6.1 |
| BDD-O01-EC-03 | EC | US-O01 | FR-6.3 |
| BDD-O02 | HP | US-O02 | FR-6.2, FR-6.3 |
| BDD-O02-EC-01 | EC | US-O02 | FR-6.2, FR-6.4 |
| BDD-O02-EC-02 | EC | US-O02 | FR-6.4 |
| BDD-O02-EC-03 | EC | US-O02 | FR-6.2 |
| BDD-O02-EC-04 | EC | US-O02 | FR-6.1 |
| BDD-O03 | HP | US-O03 | FR-6.3, FR-6.4 |
| BDD-O03-EC-01 | EC | US-O03 | FR-6.3, FR-6.4 |
| BDD-O03-EC-02 | EC | US-O03 | FR-6.3 |
| BDD-O04 | HP | US-O04 | FR-6.5 |
| BDD-O04-EC-01 | EC | US-O04 | FR-6.5 |
| BDD-O05 | HP | US-O05 | FR-6.3 |

#### Bill Payments

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-B01 | HP | US-B01 | FR-7.1 |
| BDD-B01-EC-01 | EC | US-B05 | FR-7.5 |
| BDD-B01-EC-02 | EC | US-B01 | FR-7.1 |
| BDD-B01-EC-03 | EC | US-B01 | FR-7.1 |
| BDD-B02 | HP | US-B02 | FR-7.2 |
| BDD-B02-CARD | HP | US-B02 | FR-7.2 |
| BDD-B03 | HP | US-B03 | FR-7.3 |
| BDD-B04 | HP | US-B04 | FR-7.4 |
| BDD-B04-EC-01 | EC | US-B04 | FR-7.4 |

#### Prepaid Top-up

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-T01 | HP | US-T01 | FR-8.1 |
| BDD-T01-EC-01 | EC | US-T03 | FR-8.3 |
| BDD-T01-EC-02 | EC | US-T01 | FR-8.1 |
| BDD-T01-CARD | HP | US-T01 | FR-8.1 |
| BDD-T02 | HP | US-T02 | FR-8.2 |
| BDD-T02-EC-01 | EC | US-T02 | FR-8.3 |

#### DuitNow & JomPAY

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-DNOW-01 | HP | US-D01 | FR-9.1, FR-9.2 |
| BDD-DNOW-01-EC-01 | EC | US-D01 | FR-9.1 |
| BDD-DNOW-01-EC-02 | EC | US-D01 | FR-9.3 |
| BDD-DNOW-01-EC-03 | EC | US-D01 | FR-9.1 |
| BDD-DNOW-01-NRIC | HP | US-D01 | FR-9.2 |
| BDD-DNOW-01-BRN | HP | US-D01 | FR-9.2 |
| BDD-DNOW-02 | HP | US-D02 | FR-7.1 |
| BDD-DNOW-03 | HP | US-D02 | FR-7.1 |

#### e-Wallet & eSSP

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-WAL-01 | HP | US-W01 | FR-10.1 |
| BDD-WAL-01-EC-01 | EC | US-W01 | FR-10.1 |
| BDD-WAL-01-CARD | HP | US-W01 | FR-10.1 |
| BDD-WAL-02 | HP | US-W02 | FR-10.2 |
| BDD-WAL-02-CARD | HP | US-W02 | FR-10.2 |
| BDD-ESSP-01 | HP | US-E01 | FR-10.3 |
| BDD-ESSP-01-EC-01 | EC | US-E01 | FR-10.3 |
| BDD-ESSP-01-CARD | HP | US-E01 | FR-10.3 |

#### Agent Onboarding

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-A01 | HP | US-A01 | FR-6.1, FR-6.2, FR-6.3, FR-14.3 |
| BDD-A01-EC-01 | EC | US-A01 | FR-14.3 |
| BDD-A01-EC-02 | EC | US-A01 | FR-14.3 |
| BDD-A01-EC-03 | EC | US-A01 | FR-14.3 |
| BDD-A02 | HP | US-A02 | FR-14.4 |
| BDD-A02-EC-01 | EC | US-A02 | FR-14.4, FR-17.6 |
| BDD-A02-EC-02 | EC | US-A02 | FR-14.4 |

#### Reversals & Disputes

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-V01 | HP | US-V01 | FR-18.1, FR-18.4 |
| BDD-V01-EC-01 | EC | US-V01 | FR-18.2 |
| BDD-V01-EC-02 | EC | US-V01 | FR-18.2 |
| BDD-V01-EC-03 | EC | US-V01 | FR-18.4 |
| BDD-V01-ECHO | HP | N/A | FR-18.5 |
| BDD-V02 | HP | US-V02 | FR-18.3 |
| BDD-V02-EC-01 | EC | US-V02 | FR-18.3 |
| BDD-V03 | HP | US-V03 | FR-18.3 |
| BDD-V03-EC-01 | EC | US-V03 | FR-18.3 |
| BDD-V03-EC-02 | EC | US-V03 | FR-17.6 |

#### Merchant Services

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-M01 | HP | US-M01 | FR-15.1, FR-15.3 |
| BDD-M01-EC-01 | EC | US-M01 | FR-15.1 |
| BDD-M01-QR | HP | US-M01 | FR-15.1 |
| BDD-M01-RTP | HP | US-M01 | FR-15.1 |
| BDD-M02 | HP | US-M02 | FR-15.2, FR-15.3 |
| BDD-M02-EC-01 | EC | US-M02 | FR-15.2 |
| BDD-M02-CARD | HP | US-M02 | FR-15.2 |
| BDD-M03 | HP | US-M03 | FR-15.5 |
| BDD-M03-EC-01 | EC | US-M03 | FR-15.5 |
| BDD-M03-NET | HP | US-M01 | FR-15.1 |

#### AML/Fraud & Velocity

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-EFM01 | HP | US-EFM01 | FR-14.5 |
| BDD-EFM01-EC-01 | EC | US-EFM01 | FR-14.5 |
| BDD-EFM02 | HP | US-EFM02 | FR-14.4 |
| BDD-EFM02-EC-01 | EC | US-EFM02 | FR-14.4 |
| BDD-EFM03 | HP | N/A | FR-14.5 |
| BDD-EFM04 | HP | US-R03 | FR-1.3 |
| BDD-EFM04-EC-01 | EC | US-R03 | FR-1.3 |

#### EOD Settlement & Reconciliation

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-SM01 | HP | US-SM01 | FR-16.1 |
| BDD-SM01-EC-01 | EC | US-SM01 | FR-16.2 |
| BDD-SM01-EC-02 | EC | US-SM01 | FR-16.4 |
| BDD-SM01-EC-03 | EC | US-SM01 | FR-16.5 |
| BDD-SM01-MER | HP | US-SM01 | FR-16.1 |
| BDD-SM02 | HP | US-SM02 | FR-16.3 |
| BDD-SM02-EC-01 | EC | US-SM02 | FR-16.3 |
| BDD-SM03 | HP | US-SM03 | FR-17.1 |
| BDD-SM03-EC-01 | EC | US-SM03 | FR-17.2 |
| BDD-SM03-EC-02 | EC | US-SM03 | FR-17.2 |
| BDD-SM03-EC-03 | EC | US-SM03 | FR-17.2 |
| BDD-SM04 | HP | US-SM04 | FR-17.3 |
| BDD-SM04-EC-01 | EC | US-SM04 | FR-17.8 |

#### Discrepancy Resolution

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-DR01 | HP | US-DR01 | FR-17.4, FR-17.7 |
| BDD-DR01-EC-01 | EC | US-DR01 | FR-17.4 |
| BDD-DR01-EC-02 | EC | US-DR01 | FR-17.4 |
| BDD-DR02 | HP | US-DR02 | FR-17.5, FR-17.7 |
| BDD-DR02-ORPHAN | HP | US-DR02 | FR-17.5 |
| BDD-DR02-EC-01 | EC | US-DR02 | FR-17.5 |
| BDD-DR03 | HP | US-DR03 | FR-17.6 |
| BDD-DR03-EC-01 | EC | US-DR03 | FR-17.6 |
| BDD-DR01-MISMATCH | HP | US-DR01 | FR-17.4 |

#### API Gateway & Backoffice

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-G01 | HP | US-G01 | FR-12.1 |
| BDD-G02 | HP | US-G02 | FR-12.2 |
| BDD-G01-EC-01 | EC | US-G02 | FR-12.2 |
| BDD-G01-EC-02 | EC | US-G02 | FR-12.2 |
| BDD-G01-EC-03 | EC | US-G01 | FR-12.1 |
| BDD-G01-EC-04 | EC | US-G01 | FR-14.5 |
| BDD-G01-EC-05 | EC | US-G01 | FR-12.1 |
| BDD-BO01 | HP | US-BO01 | FR-13.1 |
| BDD-BO01-EC-01 | EC | US-BO01 | FR-13.1 |
| BDD-BO01-EC-02 | EC | US-BO01 | FR-13.1 |
| BDD-BO01-EC-03 | EC | US-BO01 | FR-13.1 |
| BDD-BO02 | HP | US-BO02 | FR-13.2 |
| BDD-BO02-EC-01 | EC | US-BO02 | FR-13.2 |
| BDD-BO03 | HP | US-BO03 | FR-13.3 |
| BDD-BO03-EC-01 | EC | US-BO03 | FR-13.3 |
| BDD-BO04 | HP | US-BO04 | FR-13.4 |
| BDD-BO05 | HP | US-BO05 | FR-13.5 |
| BDD-BO05-EC-01 | EC | US-BO05 | FR-13.5 |
| BDD-BO06 | HP | US-BO06 | FR-13.5 |
| BDD-BO06-EC-01 | EC | US-BO06 | FR-13.5 |

#### STP Processing

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-S01 | HP | US-S01 | FR-14.1, FR-14.2 |
| BDD-S01-EC-01 | EC | US-S01 | FR-14.5 |
| BDD-S02 | HP | US-S02 | FR-14.1, FR-14.3 |
| BDD-S02-EC-01 | EC | US-S02 | FR-14.3 |
| BDD-S03 | HP | US-S03 | FR-14.1, FR-14.4 |
| BDD-S03-EC-01 | EC | US-S03 | FR-14.4, FR-17.6 |
| BDD-S03-EC-02 | EC | US-S03 | FR-14.4 |

#### Tier 4: ISO Translation Engine

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-ISO01 | HP | US-ISO01 | FR-20.1, FR-20.2 |
| BDD-ISO01-EC-01 | EC | US-ISO01 | FR-20.1 |
| BDD-ISO01-EC-02 | EC | US-ISO01 | FR-20.3 |
| BDD-ISO01-EC-03 | EC | US-ISO01 | FR-20.4 |
| BDD-ISO01-EC-04 | EC | US-ISO01 | FR-20.5 |
| BDD-ISO01-HEARTBEAT | HP | US-ISO01 | FR-20.3 |

#### Tier 4: CBS Connector

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-CBS01 | HP | US-CBS01 | FR-21.1, FR-21.2 |
| BDD-CBS01-EC-01 | EC | US-CBS01 | FR-21.3 |
| BDD-CBS01-EC-02 | EC | US-CBS01 | FR-21.2 |
| BDD-CBS01-SETTLEMENT | HP | US-CBS01 | FR-21.4 |

#### Tier 4: HSM Wrapper

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-HSM01 | HP | US-HSM01 | FR-22.1, FR-22.2 |
| BDD-HSM01-EC-01 | EC | US-HSM01 | FR-22.2 |
| BDD-HSM01-EC-02 | EC | US-HSM01 | FR-22.3 |
| BDD-HSM01-EC-03 | EC | US-HSM01 | FR-22.4 |

#### Tier 4: Biller Gateway

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-BG01 | HP | US-BG01 | FR-23.1, FR-23.2 |
| BDD-BG01-EC-01 | EC | US-BG01 | FR-23.3 |
| BDD-BG01-EC-02 | EC | US-BG01 | FR-23.4 |

#### Tier 4: Error Mapping

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-EM01 | HP | US-EM01 | FR-24.1, FR-24.2 |
| BDD-EM01-EC-01 | EC | US-EM01 | FR-24.1 |
| BDD-EM01-EC-02 | EC | US-EM01 | FR-24.3 |

---

## 20. Error Code Reference

### 20.1 Validation Errors (ERR_VAL_xxx)

| Error Code | Category | Used In | Description |
|------------|----------|---------|-------------|
| ERR_VAL_001 | Validation | BDD-R02-EC-03, BDD-R02-EC-04, BDD-D01-EC-02, BDD-D01-EC-03 | Invalid request format or malformed input |
| ERR_VAL_002 | Validation | BDD-R02-EC-03 | Invalid amount format (negative, zero, exceeds precision) |
| ERR_VAL_003 | Validation | BDD-D01-EC-01 | Invalid account identifier format |
| ERR_VAL_004 | Validation | BDD-O01-EC-01 | Invalid MyKad format (must be 12 digits) |
| ERR_VAL_005 | Validation | BDD-O02-EC-04 | Invalid MyKad data format |
| ERR_VAL_006 | Validation | BDD-T01-EC-01, BDD-T02-EC-01 | Invalid phone number format |
| ERR_VAL_007 | Validation | BDD-DNOW-01-EC-03 | Invalid DuitNow proxy format |
| ERR_VAL_008 | Validation | BDD-L04-EC-01, BDD-W01-EC-01, BDD-D02-EC-01, BDD-M01-EC-01 | Invalid PIN format or length |
| ERR_VAL_009 | Validation | BDD-R02-EC-01 | Amount exceeds daily limit for agent tier |
| ERR_VAL_010 | Validation | BDD-R02-EC-02 | Transaction count exceeds daily limit |
| ERR_VAL_011 | Validation | BDD-L04-EC-01 | PIN verification failed (3 attempts exceeded) |
| ERR_VAL_012 | Validation | BDD-R01-EC-01 | Fee configuration not found for transaction type/tier |
| ERR_VAL_013 | Validation | BDD-R01-EC-02 | Fee configuration expired or not yet effective |
| ERR_VAL_014 | Validation | BDD-R01-EC-03 | Fee components mismatch between config and request |

### 20.2 Business Errors (ERR_BIZ_xxx)

| Error Code | Category | Used In | Description |
|------------|----------|---------|-------------|
| ERR_BIZ_201 | Business | BDD-L03-EC-01, BDD-L03-EC-02, BDD-B01-EC-03, BDD-M03-EC-01 | Insufficient agent float balance |
| ERR_BIZ_202 | Business | BDD-W01-EC-04, BDD-D01-EC-02 | Insufficient customer balance |
| ERR_BIZ_203 | Business | BDD-R03-EC-01, BDD-S01-EC-01, BDD-EFM04 | Daily transaction limit exceeded |
| ERR_BIZ_204 | Business | BDD-R03-EC-02, BDD-EFM04-EC-01 | Daily amount limit exceeded |
| ERR_BIZ_205 | Business | BDD-D01-EC-04 | Agent float cap exceeded |
| ERR_BIZ_206 | Business | BDD-L01-EC-01 | Agent float account not found |
| ERR_BIZ_207 | Business | BDD-L01-EC-02 | Agent account is deactivated |
| ERR_BIZ_208 | Business | BDD-WAL-01-EC-01 | Insufficient e-wallet balance |
| ERR_BIZ_209 | Business | BDD-W01-EC-05 | Geofence violation - transaction outside agent location |
| ERR_BIZ_210 | Business | BDD-A02-EC-01, BDD-V03-EC-02, BDD-DR03-EC-01, BDD-S03-EC-01 | Self-approval prohibited (Four-Eyes Principle violation) |
| ERR_BIZ_211 | Business | BDD-DR01-EC-01 | Reason code required for manual adjustment |
| ERR_BIZ_212 | Business | BDD-DR01-EC-02 | Evidence attachment required for discrepancy resolution |
| ERR_BIZ_213 | Business | BDD-O04-EC-01 | Duplicate account detected |
| ERR_BIZ_214 | Business | BDD-B01-EC-01 | Invalid biller reference number |
| ERR_BIZ_215 | Business | BDD-B04-EC-01 | Invalid EPF member number |
| ERR_BIZ_216 | Business | BDD-B01-EC-02 | Biller service timeout |
| ERR_BIZ_217 | Business | BDD-T01-EC-02 | Aggregator/aggregator timeout |
| ERR_BIZ_218 | Business | BDD-EFM01 | Smurfing/structuring pattern detected |
| ERR_BIZ_219 | Business | BDD-BO01-EC-01 | Duplicate agent registration |
| ERR_BIZ_220 | Business | BDD-BO01-EC-02 | Agent has pending transactions |
| ERR_BIZ_221 | Business | BDD-L02-EC-01 | Account not found for double-entry posting |
| ERR_BIZ_222 | Business | BDD-M02-EC-01 | PIN voucher inventory depleted |
| ERR_BIZ_223 | Business | BDD-ESSP-01-EC-01 | eSSP service unavailable |
| ERR_BIZ_301 | Business | BDD-V01-EC-01, BDD-V01-EC-02 | Reversal window expired (>5 minutes) |
| ERR_BIZ_302 | Business | BDD-V03-EC-01 | Transaction already reversed |
| ERR_BIZ_303 | Business | BDD-SM04-EC-01 | Settlement paused due to unresolved discrepancies |
| ERR_BIZ_304 | Business | BDD-DR01-MISMATCH | Amount mismatch in discrepancy resolution |

### 20.3 External Errors (ERR_EXT_xxx)

| Error Code | Category | Used In | Description |
|------------|----------|---------|-------------|
| ERR_EXT_101 | External | BDD-CBS01-EC-01 | CBS connector timeout |
| ERR_EXT_102 | External | BDD-CBS01-EC-02 | CBS connector error |
| ERR_EXT_103 | External | BDD-CBS01-SETTLEMENT | CBS settlement file generation failed |
| ERR_EXT_201 | External | BDD-B01-EC-02 | Biller gateway timeout |
| ERR_EXT_202 | External | BDD-B01-EC-02 | Biller gateway error |
| ERR_EXT_203 | External | BDD-BG01-EC-01 | Biller API authentication failure |
| ERR_EXT_204 | External | BDD-BG01-EC-02 | Biller idempotency conflict |
| ERR_EXT_301 | External | BDD-O01-EC-02 | JPN/KYC service unavailable |
| ERR_EXT_302 | External | BDD-O02-EC-03 | Biometric scanner unavailable |
| ERR_EXT_303 | External | BDD-O02-EC-01 | Biometric verification mismatch |
| ERR_EXT_401 | External | BDD-DNOW-01-EC-02 | PayNet ISO timeout |
| ERR_EXT_402 | External | BDD-DNOW-01-EC-01 | PayNet ISO error |
| ERR_EXT_403 | External | BDD-ISO01-EC-02 | ISO TCP socket connection lost |
| ERR_EXT_404 | External | BDD-ISO01-EC-03 | STAN exhaustion (999999 reached) |

### 20.4 Authentication Errors (ERR_AUTH_xxx)

| Error Code | Category | Used In | Description |
|------------|----------|---------|-------------|
| ERR_AUTH_001 | Authentication | BDD-G01-EC-01 | Invalid OAuth2 token |
| ERR_AUTH_002 | Authentication | BDD-G01-EC-02 | Missing authentication token |
| ERR_AUTH_003 | Authentication | BDD-G01-EC-03 | Service unavailable for authentication |
| ERR_AUTH_004 | Authentication | BDD-G01-EC-04 | Rate limit exceeded |
| ERR_AUTH_101 | Authentication | BDD-EFM03 | Account/terminal sanctioned or blocked |
| ERR_AUTH_102 | Authentication | BDD-L04-EC-01 | PIN locked after max attempts |
| ERR_AUTH_103 | Authentication | BDD-G01-EC-05 | Token expired |

### 20.5 System Errors (ERR_SYS_xxx)

| Error Code | Category | Used In | Description |
|------------|----------|---------|-------------|
| ERR_SYS_001 | System | BDD-G01-EC-03 | Internal server error |
| ERR_SYS_002 | System | BDD-W01-EC-06 | GPS service unavailable |
| ERR_SYS_003 | System | BDD-TO01-EC-02 | Temporal workflow state corruption |
| ERR_SYS_004 | System | BDD-TO01-EC-03 | Temporal workflow not found |
| ERR_SYS_005 | System | BDD-TO01-EC-04 | Temporal activity timeout misconfiguration |

### 20.6 Tier 4 Specific Errors

| Error Code | Category | Used In | Description |
|------------|----------|---------|-------------|
| ERR_ISO_001 | ISO Translation | BDD-ISO01-EC-01 | ISO 8583 bitmap generation failed |
| ERR_ISO_002 | ISO Translation | BDD-ISO01-EC-04 | ISO response unmarshal failed |
| ERR_ISO_003 | ISO Translation | BDD-ISO01-EC-03 | STAN generation failed |
| ERR_ISO_004 | ISO Translation | BDD-ISO01-HEARTBEAT | PayNet heartbeat timeout |
| ERR_CBS_001 | CBS Connector | BDD-CBS01-EC-01 | SOAP/MQ request timeout |
| ERR_CBS_002 | CBS Connector | BDD-CBS01-EC-02 | CBS reply queue empty |
| ERR_CBS_003 | CBS Connector | BDD-CBS01-SETTLEMENT | CBS flat-file format error |
| ERR_HSM_001 | HSM Wrapper | BDD-HSM01-EC-01 | HSM PIN translation failed |
| ERR_HSM_002 | HSM Wrapper | BDD-HSM01-EC-02 | HSM PIN verification failed |
| ERR_HSM_003 | HSM Wrapper | BDD-HSM01-EC-03 | HSM key vault access violation |
| ERR_HSM_004 | HSM Wrapper | BDD-HSM01 | HSM connection lost |
| ERR_BG_001 | Biller Gateway | BDD-BG01-EC-01 | Biller API key injection failed |
| ERR_BG_002 | Biller Gateway | BDD-BG01-EC-02 | Biller idempotency key conflict |
| ERR_EM_001 | Error Mapping | BDD-EM01-EC-01 | Unknown legacy error code received |
| ERR_EM_002 | Error Mapping | BDD-EM01-EC-02 | Fallback mapping applied for unknown error |
| ERR_EM_003 | Error Mapping | BDD-EM01 | Error normalization pipeline failure |

### 20.7 Error Code to Action Mapping

| Action Code | Description | Applicable Error Codes |
|-------------|-------------|----------------------|
| DECLINE | Transaction rejected, no retry | ERR_VAL_xxx, ERR_BIZ_2xx, ERR_AUTH_xxx |
| RETRY | Transient failure, retry with backoff | ERR_EXT_xxx, ERR_ISO_xxx, ERR_CBS_xxx, ERR_HSM_xxx, ERR_BG_xxx |
| REVIEW | Manual investigation required | ERR_BIZ_301+, ERR_AUTH_101, ERR_SYS_xxx |
| STOP_ALERT | Immediate escalation required | ERR_HSM_003, ERR_EM_001 |

---

## 21. Summary

### 21.1 Scenario Count Summary

| Category | Count |
|----------|-------|
| Happy Path (HP) | 92 |
| Edge Case (EC) | 110 |
| **Total Scenarios** | **202** |

### 21.2 Coverage by User Story

| User Story Series | Description | Scenario Count | HP | EC |
|-------------------|-------------|----------------|-----|-----|
| US-TO01/TO02 | Transaction Orchestrator (Temporal Saga) | 5 | 1 | 4 |
| US-R01 to R04 | Rules & Fee Engine | 14 | 6 | 8 |
| US-L01 to L04 | Ledger & Float | 13 | 7 | 6 |
| US-L05/L06 | Cash Withdrawal | 11 | 3 | 8 |
| US-L07/L08 | Cash Deposit | 9 | 4 | 5 |
| US-O01 to O05 | e-KYC & Onboarding | 15 | 6 | 9 |
| US-B01 to B05 | Bill Payments | 9 | 5 | 4 |
| US-T01 to T03 | Prepaid Top-up | 6 | 3 | 3 |
| US-D01/D02 | DuitNow & JomPAY | 8 | 4 | 4 |
| US-W01/W02, US-E01 | e-Wallet & eSSP | 8 | 5 | 3 |
| US-A01/A02 | Agent Onboarding | 7 | 2 | 5 |
| US-V01 to V03 | Reversals & Disputes | 9 | 4 | 5 |
| US-M01 to M03 | Merchant Services | 10 | 7 | 3 |
| US-EFM01/EFM02 | AML/Fraud & Velocity | 7 | 4 | 3 |
| US-SM01 to SM04 | EOD Settlement | 13 | 5 | 8 |
| US-DR01 to DR03 | Discrepancy Resolution | 9 | 5 | 4 |
| US-G01/G02 | API Gateway | 5 | 2 | 3 |
| US-BO01 to BO06 | Backoffice | 12 | 7 | 5 |
| US-S01 to S03 | STP Processing | 7 | 3 | 4 |
| US-ISO01 | ISO Translation Engine | 6 | 2 | 4 |
| US-CBS01 | CBS Connector | 4 | 2 | 2 |
| US-HSM01 | HSM Wrapper | 4 | 1 | 3 |
| US-BG01 | Biller Gateway | 3 | 1 | 2 |
| US-EM01 | Error Mapping | 3 | 1 | 2 |

### 21.3 Functional Requirements Coverage

| FR Group | Requirements | Covered By |
|----------|-------------|------------|
| FR-1: Rules & Fee Engine | FR-1.1 to FR-1.4 | BDD-R01, BDD-R02, BDD-R03, BDD-R04 series |
| FR-2: Ledger & Float | FR-2.1 to FR-2.5 | BDD-L01, BDD-L02, BDD-L03 series |
| FR-3: Cash Withdrawal | FR-3.1 to FR-3.5 | BDD-W01, BDD-W02 series |
| FR-4: Cash Deposit | FR-4.1 to FR-4.3 | BDD-D01, BDD-D02 series |
| FR-5: Balance Inquiry | FR-5.1, FR-5.2 | BDD-L01, BDD-L04 series |
| FR-6: e-KYC & Onboarding | FR-6.1 to FR-6.5 | BDD-O01 to BDD-O05 series |
| FR-7: Bill Payments | FR-7.1 to FR-7.5 | BDD-B01 to BDD-B04, BDD-DNOW series |
| FR-8: Prepaid Top-up | FR-8.1 to FR-8.3 | BDD-T01, BDD-T02 series |
| FR-9: DuitNow | FR-9.1 to FR-9.3 | BDD-DNOW series |
| FR-10: e-Wallet & eSSP | FR-10.1 to FR-10.3 | BDD-WAL, BDD-ESSP series |
| FR-12: API Gateway | FR-12.1 to FR-12.3 | BDD-G01, BDD-G02 series |
| FR-13: Backoffice | FR-13.1 to FR-13.5 | BDD-BO01 to BDD-BO06 series |
| FR-14: STP Processing | FR-14.1 to FR-14.5 | BDD-S01 to BDD-S03, BDD-A01, BDD-A02, BDD-EFM series |
| FR-15: Merchant Services | FR-15.1 to FR-15.5 | BDD-M01 to BDD-M03 series |
| FR-16: EOD Settlement | FR-16.1 to FR-16.5 | BDD-SM01, BDD-SM02 series |
| FR-17: Reconciliation | FR-17.1 to FR-17.8 | BDD-SM03, BDD-SM04, BDD-DR01 to DR03 series |
| FR-18: Reversals & Disputes | FR-18.1 to FR-18.5 | BDD-V01 to V03 series |
| FR-19: Transaction Orchestrator | FR-19.1 to FR-19.7 | BDD-TO01 series |
| FR-20: ISO Translation | FR-20.1 to FR-20.5 | BDD-ISO01 series |
| FR-21: CBS Connector | FR-21.1 to FR-21.4 | BDD-CBS01 series |
| FR-22: HSM Wrapper | FR-22.1 to FR-22.4 | BDD-HSM01 series |
| FR-23: Biller Gateway | FR-23.1 to FR-23.4 | BDD-BG01 series |
| FR-24: Error Mapping | FR-24.1 to FR-24.3 | BDD-EM01 series |

### 21.4 Key Architectural Changes from v1.1 to v2.0

| Aspect | v1.1 | v2.0 |
|--------|------|------|
| **Saga Orchestration** | None (direct service calls) | Temporal-based Saga coordinator |
| **Error Handling** | Scattered error codes | Centralized error registry (ERR_VAL/BIZ/EXT/AUTH/SYS) |
| **Translation Layer** | Not specified | Tier 4 ISO/CBS/HSM/BG/EM services added |
| **Total Scenarios** | 120 | 202 (+68%) |
| **User Stories** | 35 | 55+ (including Tier 4) |
| **Phase Coverage** | MVP focused | MVP + Phase 2 full platform |
| **Scenario Types** | HP: 58, EC: 62 | HP: 92, EC: 110 |
| **Error Code Count** | ~40 codes | 60+ codes with categorization |

### 21.5 BDD v2.0 New Additions

#### Transaction Orchestrator (New in v2.0)
- **Temporal Saga Patterns**: 5 scenarios covering workflow execution, crash recovery, compensation handlers
- **Key Focus**: Durable execution, zero-retries for financial activities, idempotency

#### Tier 4 Translation Services (New in v2.0)
- **ISO Translation Engine**: 6 scenarios for ISO 8583/20022 translation, TCP socket management
- **CBS Connector**: 4 scenarios for legacy protocol translation, settlement file generation
- **HSM Wrapper**: 4 scenarios for PIN block translation, key vault security
- **Biller Gateway**: 3 scenarios for multi-biller normalization, idempotency
- **Error Mapping**: 3 scenarios for legacy error code normalization

### 21.6 STP Classification Summary

| STP Category | Scenario Count | Percentage |
|--------------|----------------|------------|
| 100% STP (Happy Path) | 92 | 45.5% |
| 100% STP (Edge Cases) | 45 | 22.3% |
| Conditional STP | 38 | 18.8% |
| Non-STP (Maker-Checker) | 27 | 13.4% |
| **Total** | **202** | **100%** |

---

*Document generated for BDD v2.0 specification (2026-04-06)*
