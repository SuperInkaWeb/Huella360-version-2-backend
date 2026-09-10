-- V4: Mejoras criticas para el Modulo de Catalogo (SaaS Production-Ready)

-- 1. Mejoras en Categorias
ALTER TABLE categorias ADD COLUMN activo BOOLEAN DEFAULT TRUE;
ALTER TABLE categorias ADD COLUMN orden INTEGER DEFAULT 0;

-- 2. Mejoras en Productos
ALTER TABLE productos ADD COLUMN activo BOOLEAN DEFAULT TRUE;
ALTER TABLE productos ADD COLUMN visible BOOLEAN DEFAULT TRUE;
ALTER TABLE productos ADD COLUMN oferta_inicio TIMESTAMPTZ;
ALTER TABLE productos ADD COLUMN oferta_fin TIMESTAMPTZ;
ALTER TABLE productos ADD COLUMN version BIGINT DEFAULT 0;

-- Restricción estricta de SKU único por Empresa
DROP INDEX IF EXISTS idx_productos_sku;
ALTER TABLE productos ADD CONSTRAINT unique_empresa_sku UNIQUE (empresa_id, sku);

-- Indices para queries criticas de marketplace (Alta velocidad de lectura)
CREATE INDEX idx_productos_marketplace ON productos(estado, visible, activo);
CREATE INDEX idx_productos_ofertas ON productos(oferta_inicio, oferta_fin) WHERE oferta_inicio IS NOT NULL;

-- 3. Mejoras en Servicios
ALTER TABLE servicios ADD COLUMN visible BOOLEAN DEFAULT TRUE;
ALTER TABLE servicios ADD COLUMN version BIGINT DEFAULT 0;

-- Actualización en constraint de modalidad para soportar HIBRIDO (PostgreSQL maneja esto agregando valor a ENUM si se creó como Type, como lo hicimos en V1)
ALTER TYPE service_modality ADD VALUE IF NOT EXISTS 'HIBRIDO';

-- Indices para queries de servicios
CREATE INDEX idx_servicios_marketplace ON servicios(visible, activo);
CREATE INDEX idx_servicios_vet ON servicios(veterinario_id) WHERE veterinario_id IS NOT NULL;
