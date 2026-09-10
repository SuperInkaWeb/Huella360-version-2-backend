-- Agregar campos de MercadoPago a veterinarios
ALTER TABLE veterinarios ADD COLUMN mp_access_token VARCHAR(500);
ALTER TABLE veterinarios ADD COLUMN mp_public_key VARCHAR(255);

-- Hacer empresa_id opcional en ordenes y agregar veterinario_id
ALTER TABLE ordenes ALTER COLUMN empresa_id DROP NOT NULL;
ALTER TABLE ordenes ADD COLUMN veterinario_id BIGINT REFERENCES veterinarios(id_veterinario);

-- Hacer empresa_id opcional en pagos y agregar veterinario_id
ALTER TABLE pagos ALTER COLUMN empresa_id DROP NOT NULL;
ALTER TABLE pagos ADD COLUMN veterinario_id BIGINT REFERENCES veterinarios(id_veterinario);

-- Índice para mejorar búsquedas por veterinario en órdenes y pagos
CREATE INDEX idx_ordenes_veterinario ON ordenes(veterinario_id);
CREATE INDEX idx_pagos_veterinario ON pagos(veterinario_id);
