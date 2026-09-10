CREATE TABLE webhook_events (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    empresa_id VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    next_retry_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_events_status_retry ON webhook_events(status, next_retry_at) WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX idx_webhook_events_payment_id ON webhook_events(payment_id);
