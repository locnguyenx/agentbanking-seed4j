-- Flyway Migration: V4__rules_init.sql
-- Context: Rules (FeeConfig and VelocityRule tables)
-- Description: Creates fee_config and velocity_rule tables for rules bounded context

-- Create fee_config table
CREATE TABLE fee_config (
    id UUID PRIMARY KEY,
    transaction_type VARCHAR(30) NOT NULL,
    agent_tier VARCHAR(20) NOT NULL,
    fee_type VARCHAR(20) NOT NULL,
    customer_fee_value DECIMAL(10,4) NOT NULL,
    agent_commission_value DECIMAL(10,4) NOT NULL,
    bank_share_value DECIMAL(10,4) NOT NULL,
    daily_limit_amount DECIMAL(19,4),
    daily_limit_count INTEGER,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create velocity_rule table
CREATE TABLE velocity_rule (
    id UUID PRIMARY KEY,
    scope VARCHAR(30) NOT NULL,
    max_transactions_per_day INTEGER,
    max_amount_per_day DECIMAL(19,4),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_fee_config_type_tier ON fee_config(transaction_type, agent_tier);
CREATE INDEX idx_fee_config_effective ON fee_config(effective_from, effective_to);