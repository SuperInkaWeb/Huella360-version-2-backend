CREATE TABLE delivery_otp_history (
           id BIGSERIAL PRIMARY KEY,
           otp_fingerprint CHAR(64) NOT NULL UNIQUE,
           delivery_id BIGINT NOT NULL REFERENCES deliveries(id_delivery),
           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_otp_history_delivery
    ON delivery_otp_history(delivery_id);
