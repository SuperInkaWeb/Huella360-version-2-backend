DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'staff_status') THEN
        CREATE TYPE staff_status AS ENUM ('PENDIENTE', 'ACTIVO', 'RECHAZADO', 'FINALIZADO');
    END IF;
END$$;

ALTER TABLE staff_veterinario ADD COLUMN estado staff_status;

UPDATE staff_veterinario SET estado = 'ACTIVO' WHERE activo = TRUE;
UPDATE staff_veterinario SET estado = 'FINALIZADO' WHERE activo = FALSE;

UPDATE staff_veterinario SET estado = 'PENDIENTE' WHERE estado IS NULL;

ALTER TABLE staff_veterinario ALTER COLUMN estado SET NOT NULL;
ALTER TABLE staff_veterinario ALTER COLUMN estado SET DEFAULT 'PENDIENTE';

ALTER TABLE staff_veterinario ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE staff_veterinario DROP COLUMN activo;