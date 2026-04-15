-- V11__merchant_init.sql
CREATE TABLE IF NOT EXISTS merchant_transaction (
  id UUID PRIMARY KEY,
  agent_id VARCHAR(32) NOT NULL,
  trace_id UUID,
  merchant_id VARCHAR(32) NOT NULL,
  card_type VARCHAR(20) NOT NULL,
  masked_pan VARCHAR(20),
  transaction_amount DECIMAL(18,2) NOT NULL,
  mdr_amount DECIMAL(18,2) NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL,
  cash_back_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL,
  authorization_code VARCHAR(20),
  created_at TIMESTAMP NOT NULL,
  captured_at TIMESTAMP,
  invoice_number VARCHAR(50),
  terminal_id VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS mdr_config (
  id UUID PRIMARY KEY,
  category VARCHAR(20) NOT NULL,
  card_type VARCHAR(20) NOT NULL,
  mdr_percentage DECIMAL(5,2) NOT NULL,
  mdr_fixed DECIMAL(18,2),
  min_amount DECIMAL(18,2),
  max_amount DECIMAL(18,2),
  cash_back_allowed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_merchant_agent ON merchant_transaction(agent_id);
CREATE INDEX idx_merchant_merchant ON merchant_transaction(merchant_id);
CREATE INDEX idx_merchant_status ON merchant_transaction(status);