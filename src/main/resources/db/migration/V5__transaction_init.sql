-- Flyway Migration: V5__transaction_init.sql
-- Context: Transaction (Transaction and Idempotency tables)
-- Description: Creates transaction and idempotency_record tables for transaction bounded context

-- Create transaction table
CREATE TABLE transaction (
    id VARCHAR(50) PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    agent_id VARCHAR(50) NOT NULL,
    customer_account_id VARCHAR(50),
    idempotency_key VARCHAR(100) UNIQUE,
    saga_execution_id VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    customer_fee DECIMAL(10,4),
    agent_commission DECIMAL(10,4),
    bank_share DECIMAL(10,4),
    customer_card_masked VARCHAR(20),
    error_code VARCHAR(30),
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- Create idempotency_record table
CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    response_payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_txn_agent ON transaction(agent_id);
CREATE INDEX idx_txn_status ON transaction(status);
CREATE INDEX idx_txn_idempotency ON transaction(idempotency_key);
CREATE INDEX idx_txn_saga ON transaction(saga_execution_id);
CREATE INDEX idx_txn_initiated ON transaction(initiated_at);
CREATE INDEX idx_idem_expires ON idempotency_record(expires_at);