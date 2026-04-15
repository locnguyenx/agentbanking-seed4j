# Agent Banking Platform - End User Guide

## Welcome to Agent Banking

The Agent Banking Platform enables financial services at third-party retail locations. Agents can perform cash deposits, withdrawals, bill payments, and more on behalf of customers.

---

## Getting Started

### Prerequisites
- Registered agent account with KYC verification
- POS (Point of Sale) terminal or mobile app
- Secure login credentials

### Authentication
All API requests require a JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

---

## Available Services

### 1. Agent Onboarding
**Endpoint:** `POST /api/onboarding/register`
**Purpose:** Register a new agent in the system

**Request:**
```json
{
  "agentName": "John Doe",
  "phoneNumber": "+60123456789",
  "email": "john@agent.com",
  "shopName": "John's Mini Mart",
  "shopAddress": "123 Main Street, Kuala Lumpur",
  "state": "WPKL"
}
```

**Response:**
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING_KYC",
  "registeredAt": "2026-01-15T10:30:00+08:00"
}
```

**Get Agent Details**
`GET /api/onboarding/agent/{agentId}`

---

### 2. KYC Verification
**Endpoint:** `POST /api/onboarding/agent/{agentId}/kyc`
**Purpose:** Complete Know Your Customer verification

**Request:**
```json
{
  "mykadNumber": "740101011234",
  "phoneNumber": "+60123456789"
}
```

**Response:** `true` (verification successful)

---

### 3. Float Management (Agent Wallet)

#### Check Balance
**Endpoint:** `GET /api/float/{agentId}/balance`
**Purpose:** View current float balance

**Response:**
```json
{
  "available": 5000.00,
  "reserved": 1000.00,
  "currency": "MYR"
}
```

#### Credit Float (Top-up)
**Endpoint:** `POST /api/float/{agentId}/credit`
**Purpose:** Add funds to agent float

**Request:**
```json
{
  "amount": 10000.00
}
```

**Response:**
```json
{
  "newBalance": 15000.00,
  "transactionId": "TXN-20260115-001"
}
```

---

### 4. Transaction Processing

#### Execute Transaction
**Endpoint:** `POST /api/transaction/execute`
**Purpose:** Process a cash in/out transaction

**Request (Cash Deposit):**
```json
{
  "type": "CASH_DEPOSIT",
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "amount": 500.00,
  "channel": "POS"
}
```

**Request (Cash Withdrawal):**
```json
{
  "type": "CASH_WITHDRAWAL",
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "amount": 200.00,
  "channel": "POS"
}
```

**Request (Bill Payment):**
```json
{
  "type": "BILL_PAYMENT",
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "amount": 150.00,
  "billerCode": "TNB",
  "referenceNumber": "123456789012",
  "channel": "POS"
}
```

**Request (Balance Inquiry):**
```json
{
  "type": "BALANCE_INQUIRY",
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "channel": "POS"
}
```

**Response:**
```json
{
  "sagaId": "SAGA-20260115-001",
  "status": "COMPLETED",
  "customerId": "CUST-001",
  "amount": 500.00,
  "type": "CASH_DEPOSIT",
  "completedAt": "2026-01-15T10:35:00+08:00"
}
```

#### Check Transaction Status
**Endpoint:** `GET /api/transaction/{sagaId}/status`
**Purpose:** Check status of a previous transaction

**Response:**
```json
{
  "sagaId": "SAGA-20260115-001",
  "status": "COMPLETED",
  "amount": 500.00
}
```

#### Cancel Transaction
**Endpoint:** `POST /api/transaction/{sagaId}/cancel`
**Purpose:** Cancel a pending transaction

**Request:**
```json
{
  "reason": "Customer changed mind"
}
```

**Response:**
```json
{
  "cancelled": true
}
```

---

### 5. Commission

#### Calculate Commission
**Endpoint:** `POST /api/commission/calculate`
**Purpose:** Calculate commission for a transaction

**Request:**
```json
{
  "transactionId": "TXN-001",
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "DEPOSIT",
  "amount": 500.00
}
```

**Response:**
```json
{
  "commissionId": "COMM-001",
  "amount": 2.50,
  "rate": 0.005,
  "type": "DEPOSIT"
}
```

#### Settle Commissions
**Endpoint:** `POST /api/commission/settle`
**Purpose:** Settle all pending commissions to agent account

**Response:**
```json
{
  "settledCount": 15,
  "totalAmount": 37.50
}
```

---

### 6. Bill Payment

#### Pay Bills
**Endpoint:** `POST /api/biller-adapter/pay`
**Purpose:** Pay utility bills (electricity, water, etc.)

**Request:**
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "billerCode": "TNB",
  "accountNumber": "123456789012",
  "amount": 150.00
}
```

**Response:**
```json
{
  "receiptNumber": "RCP-20260115-001",
  "status": "SUCCESS",
  "billerName": "Tenaga Nasional Berhad",
  "amount": 150.00,
  "paidAt": "2026-01-15T10:40:00+08:00"
}
```

#### Check Bill Status
**Endpoint:** `GET /api/biller-adapter/status/{billerCode}/{referenceNumber}`
**Purpose:** Check outstanding bill amount

**Response:**
```json
{
  "billerCode": "TNB",
  "referenceNumber": "123456789012",
  "outstandingAmount": 250.00,
  "dueDate": "2026-02-28"
}
```

#### Reverse Payment
**Endpoint:** `POST /api/biller-adapter/reverse`
**Purpose:** Reverse a bill payment

**Request:**
```json
{
  "receiptNumber": "RCP-20260115-001",
  "reason": "Customer request"
}
```

**Response:**
```json
{
  "reversed": true,
  "refundAmount": 150.00
}
```

---

### 7. Notifications

#### Send Notification
**Endpoint:** `POST /api/notification/send`
**Purpose:** Send SMS/Push to customer

**Request:**
```json
{
  "type": "SMS",
  "phoneNumber": "+60123456789",
  "message": "Your transaction of RM500.00 is completed."
}
```

---

### 8. Rules Validation

#### Validate Transaction
**Endpoint:** `POST /api/rules/validate`
**Purpose:** Validate transaction against business rules

**Request:**
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "CASH_DEPOSIT",
  "amount": 500.00
}
```

**Response:**
```json
{
  "valid": true,
  "approved": true
}
```

---

### 9. Ledger

#### Get Transaction Entries
**Endpoint:** `GET /api/ledger/transaction/{transactionId}/entries`
**Purpose:** View ledger entries for a transaction

**Response:**
```json
{
  "transactionId": "TXN-001",
  "entries": [
    {
      "accountType": "AGENT_FLOAT",
      "debit": 500.00,
      "credit": 0.00
    },
    {
      "accountType": "CUSTOMER_ACCOUNT",
      "debit": 0.00,
      "credit": 500.00
    }
  ]
}
```

---

### 10. Settlement

#### Process Daily Settlement
**Endpoint:** `POST /api/settlement/daily`
**Purpose:** Process end-of-day settlements

**Response:**
```json
{
  "processedCount": 50,
  "totalAmount": 25000.00,
  "completedAt": "2026-01-15T23:59:00+08:00"
}
```

#### Generate Report
**Endpoint:** `POST /api/settlement/report`
**Purpose:** Generate settlement report

**Request:**
```json
{
  "startDate": "2026-01-15",
  "endDate": "2026-01-15"
}
```

**Response:**
```json
{
  "reportId": "RPT-20260115-001",
  "totalTransactions": 150,
  "totalVolume": 75000.00,
  "totalCommission": 375.00
}
```

---

### 11. Idempotency

#### Check Cached Response
**Endpoint:** `GET /api/idempotency/{key}`
**Purpose:** Get cached response for idempotent request

**Response:**
```json
{
  "cached": true,
  "originalRequest": {...},
  "response": {...}
}
```

#### Delete Idempotency Key
**Endpoint:** `DELETE /api/idempotency/{key}`
**Purpose:** Remove cached idempotent response

---

### 12. ISO 8583 Adapter

#### ISO Withdrawal
**Endpoint:** `POST /api/iso-adapter/withdrawal`
**Purpose:** Process ISO 8583 withdrawal request

**Request:**
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 500.00,
  "pan": "411111******1111"
}
```

#### ISO Deposit
**Endpoint:** `POST /api/iso-adapter/deposit`
**Purpose:** Process ISO 8583 deposit request

**Request:**
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 500.00,
  "pan": "411111******1111"
}
```

#### ISO Reversal
**Endpoint:** `POST /api/iso-adapter/reversal`
**Purpose:** Reverse an ISO transaction

**Request:**
```json
{
  "originalTransactionId": "TXN-001",
  "reason": "Customer request"
}
```

---

### 13. CBS Adapter

#### Balance Inquiry
**Endpoint:** `GET /api/cbs-adapter/balance/{accountId}`
**Purpose:** Query CBS for account balance

**Response:**
```json
{
  "accountId": "CUST-001",
  "availableBalance": 5000.00,
  "ledgerBalance": 5000.00,
  "currency": "MYR"
}
```

#### CBS Debit
**Endpoint:** `POST /api/cbs-adapter/debit`
**Purpose:** Debit customer account

**Request:**
```json
{
  "accountId": "CUST-001",
  "amount": 500.00,
  "description": "Cash withdrawal"
}
```

**Response:**
```json
{
  "transactionId": "CBS-001",
  "newBalance": 4500.00,
  "status": "SUCCESS"
}
```

#### CBS Credit
**Endpoint:** `POST /api/cbs-adapter/credit`
**Purpose:** Credit customer account

**Request:**
```json
{
  "accountId": "CUST-001",
  "amount": 500.00,
  "description": "Cash deposit"
}
```

**Response:**
```json
{
  "transactionId": "CBS-001",
  "newBalance": 5500.00,
  "status": "SUCCESS"
}
```

---

### 14. HSM Adapter

#### Translate PIN
**Endpoint:** `POST /api/hsm-adapter/translate-pin`
**Purpose:** Translate PIN to HSM-compatible format

**Request:**
```json
{
  "pin": "1234",
  "pan": "4111111111111111"
}
```

#### Verify PIN
**Endpoint:** `POST /api/hsm-adapter/verify-pin`
**Purpose:** Verify PIN against HSM

**Request:**
```json
{
  "encryptedPin": "...",
  "pinBlock": "...",
  "pan": "4111111111111111"
}
```

**Response:**
```json
{
  "verified": true
}
```

#### Generate PIN Block
**Endpoint:** `POST /api/hsm-adapter/generate-pin-block`
**Purpose:** Generate encrypted PIN block

**Request:**
```json
{
  "pin": "1234",
  "pan": "4111111111111111"
}
```

---

## Transaction Types

| Type | Description | Fee |
|------|-------------|-----|
| CASH_DEPOSIT | Customer deposits cash to account | RM0.50 + 0.5% |
| CASH_WITHDRAWAL | Customer withdraws cash from account | RM1.00 + 1% |
| BILL_PAYMENT | Pay utility bills | RM0.50 flat |
| BALANCE_INQUIRY | Check account balance | FREE |

---

## Status Codes

| Status | Meaning |
|--------|---------|
| PENDING | Transaction in progress |
| COMPLETED | Transaction successful |
| FAILED | Transaction failed |
| CANCELLED | Transaction cancelled |

---

## Error Responses

All errors follow this format:
```json
{
  "status": "FAILED",
  "error": {
    "code": "ERR_BIZ_001",
    "message": "Insufficient float balance",
    "action_code": "DECLINE"
  }
}
```

**Common Error Codes:**

| Code | Meaning | Action |
|------|---------|--------|
| ERR_VAL_001 | Missing required field | FIX_INPUT |
| ERR_VAL_002 | Invalid format | FIX_INPUT |
| ERR_BIZ_001 | Insufficient float balance | TOP_UP |
| ERR_BIZ_002 | Transaction limit exceeded | DECLINE |
| ERR_BIZ_003 | Agent not authorized | CONTACT_SUPPORT |
| ERR_AUTH_001 | Invalid credentials | RETRY |
| ERR_AUTH_002 | Token expired | RE_LOGIN |
| ERR_EXT_001 | External service timeout | RETRY |
| ERR_EXT_002 | External service unavailable | RETRY |
| ERR_SYS_001 | System error | CONTACT_SUPPORT |

---

## Idempotency

For duplicate transaction protection, include a unique key in requests:
```
X-Idempotency-Key: <unique-request-id>
```

If a request with the same key is sent twice, the system returns the cached response (valid for 24 hours).

---

## Backoffice UI

The Backoffice UI provides a web-based interface for managing the Agent Banking platform.

### Access
Navigate to `http://localhost:5173` (development) or your deployed URL.

### Login
Use your backoffice credentials to log in.

### Roles
| Role | Permissions |
|------|-------------|
| VIEWER | View dashboard, transactions, agents |
| OPERATOR | All viewer permissions + approve e-KYC |
| ADMIN | All permissions + manage agents, configure system |

### Dashboard
- Overview of operations
- Today's statistics
- Pending actions (e-KYC, settlements)
- Quick alerts

### Agent Management
- View all agents
- Filter by status
- Approve/Suspend/Terminate agents

### Transaction Monitor
- Real-time transactions
- Filter by status/type
- View details

### Settlement
- Daily settlement processing
- Commission reports
- Agent payout tracking

### e-KYC Review
- Pending KYC applications
- Approve/Reject with notes

### Audit Logs
- System activity logs
- Search and filter

### Compliance
- Regulatory metrics
- Alerts and warnings

---

## Testing Endpoints

You can test the API using curl:

```bash
# Register agent
curl -X POST http://localhost:8080/api/onboarding/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"agentName":"Test Agent","phoneNumber":"+60123456789",...}'

# Check float balance
curl -X GET http://localhost:8080/api/float/{agentId}/balance \
  -H "Authorization: Bearer <token>"

# Execute transaction
curl -X POST http://localhost:8080/api/transaction/execute \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"type":"CASH_DEPOSIT","agentId":"...","amount":500}'

# Pay bill
curl -X POST http://localhost:8080/api/biller-adapter/pay \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"billerCode":"TNB","accountNumber":"123456789012","amount":150}'
```

---

## Support

For issues:
- Check transaction status using saga ID
- Verify float balance before transactions
- Ensure KYC is completed before processing transactions
- Contact support for system issues

---

*This guide covers the main user-facing functionality. For technical details, see the Developer Guide.*