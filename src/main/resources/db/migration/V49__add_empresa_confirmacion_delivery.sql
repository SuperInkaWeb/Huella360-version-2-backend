ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS empresa_confirmado_at TIMESTAMPTZ;
