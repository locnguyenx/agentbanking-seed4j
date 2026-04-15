-- Flyway Migration: V7__commission_init.sql
-- Context: Commission (CommissionEntry table)
-- Description: Creates commission_entry table for commission bounded context

-- Create commission_entry table
CREATE TABLE commission_entry (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    agent_id VARCHAR(50) NOT NULL,
    commission_type VARCHAR(50) NOT NULL,
    transaction_amount DECIMAL(19,4) NOT NULL,
    commission_amount DECIMAL(10,4) NOT NULL,
    rate_applied DECIMAL(10,6) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    settled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_commission_agent ON commission_entry(agent_id);
CREATE INDEX idx_commission_txn ON commission_entry(transaction_id);
CREATE INDEX idx_commission_status ON commission_entry(status);