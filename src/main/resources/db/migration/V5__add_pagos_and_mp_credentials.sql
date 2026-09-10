-- Añadir credenciales de Mercado Pago a empresas
ALTER TABLE empresas
ADD COLUMN mp_access_token VARCHAR(500),
ADD COLUMN mp_public_key VARCHAR(255);

-- Añadir mp_preference_id a ordenes
ALTER TABLE ordenes
ADD COLUMN mp_preference_id VARCHAR(255);

-- Modificar el ENUM order_status para soportar los nuevos estados si no existen
-- (PostgreSQL permite ADD VALUE al final de un ENUM desde su versión 9.1)
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'FALLIDO';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'REEMBOLSADO';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'ENVIADO';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'ENTREGADO';

-- Crear tabla pagos para auditoría y registro de webhook
CREATE TABLE pagos (
    id_pago BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id_empresa),
    orden_id BIGINT NOT NULL REFERENCES ordenes(id_orden),
    mp_payment_id VARCHAR(100) NOT NULL UNIQUE,
    monto DECIMAL(10,2) NOT NULL,
    metodo_pago VARCHAR(50),
    estado VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indice para búsqueda rápida del webhook
CREATE INDEX idx_pagos_mp_payment_id ON pagos(mp_payment_id);
