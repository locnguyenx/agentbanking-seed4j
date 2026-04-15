-- Flyway Migration: V1__common_init.sql
-- Context: Common (Audit and System tables)
-- Description: Creates audit_log table for centralized audit trail

-- Create audit_log table
CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100),
    actor_type VARCHAR(50),
    payload JSONB,
    trace_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_trace ON audit_log(trace_id);
CREATE INDEX idx_audit_created ON audit_log(created_at);