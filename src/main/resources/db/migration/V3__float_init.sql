-- Flyway Migration: V3__float_init.sql
-- Context: Float Management (AgentFloat and FloatTransaction tables)
-- Description: Creates agent_float and float_transaction tables for float management bounded context

-- Create agent_float table
CREATE TABLE agent_float (
    agent_id VARCHAR(50) PRIMARY KEY REFERENCES agent(id),
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    reserved_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Create float_transaction table
CREATE TABLE float_transaction (
    id UUID PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL REFERENCES agent(id),
    transaction_id VARCHAR(50) NOT NULL,
    type VARCHAR(30) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    balance_before DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_float_txn_agent ON float_transaction(agent_id);
CREATE INDEX idx_float_txn_transaction ON float_transaction(transaction_id);
CREATE INDEX idx_float_txn_created ON float_transaction(created_at);