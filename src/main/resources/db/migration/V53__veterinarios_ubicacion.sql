-- Ubicacion del veterinario independiente (mismas columnas que empresas y perfiles_cliente).
-- Todas opcionales: un veterinario que solo trabaja como staff de una clinica no tiene local propio.
ALTER TABLE veterinarios
    ADD COLUMN direccion VARCHAR(255),
    ADD COLUMN ciudad VARCHAR(100),
    ADD COLUMN ubicacion_lat DECIMAL(10,8),
    ADD COLUMN ubicacion_lng DECIMAL(11,8);

-- El punto del mapa se guarda completo o no se guarda.
ALTER TABLE veterinarios ADD CONSTRAINT chk_veterinario_ubicacion_completa
    CHECK ((ubicacion_lat IS NULL) = (ubicacion_lng IS NULL));
