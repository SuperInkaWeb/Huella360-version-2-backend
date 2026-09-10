-- Modificar la tabla de suscripciones para soportar Veterinarios Independientes

-- 1. Eliminar la restriccion de NOT NULL y la unicidad en empresa_id
ALTER TABLE suscripciones ALTER COLUMN empresa_id DROP NOT NULL;
ALTER TABLE suscripciones DROP CONSTRAINT IF EXISTS suscripciones_empresa_id_key;

-- 2. Agregar la nueva columna veterinario_id
ALTER TABLE suscripciones ADD COLUMN veterinario_id BIGINT UNIQUE REFERENCES veterinarios(id_veterinario) ON DELETE CASCADE;

-- 3. Agregar CHECK constraint para asegurar que la suscripcion pertenece a una Empresa o a un Veterinario, pero no a ambos
ALTER TABLE suscripciones ADD CONSTRAINT check_suscripcion_owner CHECK (
    (empresa_id IS NOT NULL AND veterinario_id IS NULL) OR
    (empresa_id IS NULL AND veterinario_id IS NOT NULL)
);

-- 4. Agregar indice
CREATE INDEX idx_suscripciones_veterinario ON suscripciones(veterinario_id);
