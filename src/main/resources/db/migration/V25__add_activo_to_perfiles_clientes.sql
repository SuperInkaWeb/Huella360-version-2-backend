-- Add activo column for soft delete on client profiles
ALTER TABLE perfiles_clientes ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_perfiles_clientes_activo ON perfiles_clientes(activo);
