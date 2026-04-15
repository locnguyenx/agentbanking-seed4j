-- Commission service migration
CREATE TABLE IF NOT EXISTS commission_rates (
    id UUID PRIMARY KEY,
    transaction_type VARCHAR(50) NOT NULL,
    rate DECIMAL(5, 4) NOT NULL,
    min_amount DECIMAL(15, 2) DEFAULT 0,
    max_amount DECIMAL(15, 2),
    agent_tier VARCHAR(20) NOT NULL,
    valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS commission_transactions (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    agent_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    transaction_amount DECIMAL(15, 2) NOT NULL,
    commission_amount DECIMAL(15, 2) NOT NULL,
    rate_used DECIMAL(5, 4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_commission_rates_type_tier ON commission_rates(transaction_type, agent_tier);
CREATE INDEX idx_commission_transactions_agent ON commission_transactions(agent_id);
CREATE INDEX idx_commission_transactions_status ON commission_transactions(status);