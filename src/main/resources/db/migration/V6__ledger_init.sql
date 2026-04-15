-- Flyway Migration: V6__ledger_init.sql
-- Context: Ledger (Account and LedgerEntry tables)
-- Description: Creates account and ledger_entry tables for ledger bounded context

-- Create account table
CREATE TABLE account (
    id VARCHAR(50) PRIMARY KEY,
    account_type VARCHAR(50) NOT NULL,
    owner_id VARCHAR(50),
    owner_type VARCHAR(50),
    currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create ledger_entry table
CREATE TABLE ledger_entry (
    id UUID PRIMARY KEY,
    account_id VARCHAR(50) NOT NULL REFERENCES account(id),
    entry_type VARCHAR(50) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    balance_before DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    transaction_id VARCHAR(50),
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_account_type ON account(account_type);
CREATE INDEX idx_account_owner ON account(owner_id, owner_type);
CREATE INDEX idx_entry_account ON ledger_entry(account_id);
CREATE INDEX idx_entry_txn ON ledger_entry(transaction_id);
CREATE INDEX idx_entry_created ON ledger_entry(created_at);