-- Add estado and notasInternas columns to reclamos for workflow tracking
ALTER TABLE reclamos ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'RECIBIDO';
ALTER TABLE reclamos ADD COLUMN notas_internas TEXT;

CREATE INDEX idx_reclamos_estado ON reclamos(estado);
