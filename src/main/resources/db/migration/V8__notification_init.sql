-- Flyway Migration: V8__notification_init.sql
-- Context: Notification (Notification table)
-- Description: Creates notification table for notification bounded context

-- Create notification table
CREATE TABLE notification (
    id UUID PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    recipient VARCHAR(50),
    channel VARCHAR(20) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_notification_status ON notification(status);
CREATE INDEX idx_notification_recipient ON notification(recipient);
CREATE INDEX idx_notification_created ON notification(created_at);