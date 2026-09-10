-- Tabla de recordatorios
CREATE TABLE recordatorios (
    id_recordatorio BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    mascota_id BIGINT NOT NULL REFERENCES mascotas(id_mascota) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('VACUNA', 'DESPARASITACION', 'CITA', 'CHEQUEO', 'MEDICAMENTO', 'OTRO')),
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    fecha_programada TIMESTAMPTZ NOT NULL,
    enviado BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices
CREATE INDEX idx_recordatorios_usuario ON recordatorios(usuario_id);
CREATE INDEX idx_recordatorios_fecha ON recordatorios(fecha_programada);
CREATE INDEX idx_recordatorios_pendientes ON recordatorios(enviado, activo, fecha_programada);
