-- Registro de pagos de Mercado Pago ya aplicados a una suscripcion.
-- La restriccion UNIQUE sobre mp_payment_id es la garantia de idempotencia:
-- un mismo pago (webhook, reintentos de Mercado Pago o GET /payments/sync)
-- solo puede extender/activar una suscripcion una vez, incluso si llegan
-- en paralelo.
CREATE TABLE suscripcion_pagos (
    id_suscripcion_pago BIGSERIAL PRIMARY KEY,
    mp_payment_id VARCHAR(100) NOT NULL UNIQUE,
    empresa_id BIGINT REFERENCES empresas(id_empresa) ON DELETE SET NULL,
    veterinario_id BIGINT REFERENCES veterinarios(id_veterinario) ON DELETE SET NULL,
    plan_id BIGINT NOT NULL REFERENCES planes(id_plan),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
