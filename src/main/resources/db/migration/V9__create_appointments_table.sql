-- Migración V9: Crear tabla de citas (appointments)

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'appointment_status') THEN
        CREATE TYPE appointment_status AS ENUM (
            'SOLICITADA',
            'CONFIRMADA',
            'RECHAZADA',
            'COMPLETADA',
            'CANCELADA',
            'NOSHOW'
        );
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS citas (
    id_cita BIGSERIAL PRIMARY KEY,
    usuario_cliente_id BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    mascota_id BIGINT REFERENCES mascotas(id_mascota),
    servicio_id BIGINT NOT NULL REFERENCES servicios(id_servicio),
    empresa_id BIGINT NOT NULL REFERENCES empresas(id_empresa),
    veterinario_asignado_id BIGINT REFERENCES veterinarios(id_veterinario),
    fecha_programada DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado appointment_status DEFAULT 'SOLICITADA',
    orden_asociada_id BIGINT REFERENCES ordenes(id_orden),
    notas_cliente TEXT,
    notas_internas TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para mejorar rendimiento de consultas de agenda
CREATE INDEX idx_citas_empresa_fecha ON citas(empresa_id, fecha_programada);
CREATE INDEX idx_citas_vet_fecha ON citas(veterinario_asignado_id, fecha_programada);
CREATE INDEX idx_citas_cliente ON citas(usuario_cliente_id);
