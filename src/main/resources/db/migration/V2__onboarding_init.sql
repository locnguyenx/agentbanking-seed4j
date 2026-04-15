-- Flyway Migration: V2__onboarding_init.sql
-- Context: Onboarding (Agent and AgentUser tables)
-- Description: Creates agent and agent_user tables for onboarding bounded context

-- Create agent table
CREATE TABLE agent (
    id VARCHAR(50) PRIMARY KEY,
    business_registration_number VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    bank_code VARCHAR(10),
    bank_account_number VARCHAR(30),
    phone_number VARCHAR(20),
    address TEXT,
    max_float_limit DECIMAL(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create agent_user table
CREATE TABLE agent_user (
    id VARCHAR(50) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL REFERENCES agent(id),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(200),
    role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_agent_status ON agent(status);
CREATE INDEX idx_agent_type ON agent(type);
CREATE INDEX idx_agent_user_agent ON agent_user(agent_id);