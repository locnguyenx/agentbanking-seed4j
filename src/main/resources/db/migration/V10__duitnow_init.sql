-- V10__duitnow_init.sql
CREATE TABLE IF NOT EXISTS duitnow_transaction (
  id UUID PRIMARY KEY,
  idempotency_key VARCHAR(64) UNIQUE NOT NULL,
  agent_id VARCHAR(32),
  trace_id UUID,
  proxy_type VARCHAR(20) NOT NULL,
  proxy_value VARCHAR(50) NOT NULL,
  resolved_account VARCHAR(20),
  bank_code VARCHAR(6),
  recipient_name VARCHAR(100),
  amount DECIMAL(18,2) NOT NULL,
  reference VARCHAR(100),
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP,
  failure_reason VARCHAR(255)
);

CREATE INDEX idx_duit_transaction_idempotency ON duitnow_transaction(idempotency_key);
CREATE INDEX idx_duit_transaction_status ON duitnow_transaction(status);
CREATE INDEX idx_duit_transaction_agent ON duitnow_transaction(agent_id);