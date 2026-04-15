-- Flyway Migration: V9__settlement_init.sql
-- Context: Settlement (SettlementBatch, ReconciliationRecord, DiscrepancyCase tables)
-- Description: Creates settlement tables for settlement bounded context

-- Create settlement_batch table
CREATE TABLE settlement_batch (
    id UUID PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    business_date DATE NOT NULL,
    total_withdrawals DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_deposits DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_commissions DECIMAL(10,4) NOT NULL DEFAULT 0,
    net_settlement DECIMAL(19,4) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    cbs_file_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create reconciliation_record table
CREATE TABLE reconciliation_record (
    id UUID PRIMARY KEY,
    settlement_batch_id UUID REFERENCES settlement_batch(id),
    transaction_id VARCHAR(50) NOT NULL,
    discrepancy_type VARCHAR(30),
    internal_status VARCHAR(30),
    paynet_status VARCHAR(30),
    internal_amount DECIMAL(19,4),
    paynet_amount DECIMAL(19,4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create discrepancy_case table
CREATE TABLE discrepancy_case (
    id UUID PRIMARY KEY,
    case_id VARCHAR(50) NOT NULL UNIQUE,
    discrepancy_type VARCHAR(30) NOT NULL,
    reconciliation_record_id UUID REFERENCES reconciliation_record(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_MAKER',
    maker_id VARCHAR(50),
    maker_action VARCHAR(30),
    reason_code VARCHAR(30),
    checker_id VARCHAR(50),
    checker_comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_settlement_agent ON settlement_batch(agent_id);
CREATE INDEX idx_settlement_date ON settlement_batch(business_date);
CREATE INDEX idx_settlement_status ON settlement_batch(status);
CREATE INDEX idx_discrepancy_status ON discrepancy_case(status);
CREATE INDEX idx_discrepancy_case_id ON discrepancy_case(case_id);