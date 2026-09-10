-- Tabla de uso de IA para contabilizar consultas por usuario/mes
CREATE TABLE IF NOT EXISTS ia_usage (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    mascota_id BIGINT,
    tokens_usados INT,
    modelo VARCHAR(50) NOT NULL DEFAULT 'gpt-4o-mini',
    exitoso BOOLEAN NOT NULL DEFAULT true,
    fecha TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ia_usage_usuario ON ia_usage(usuario_id);
CREATE INDEX IF NOT EXISTS idx_ia_usage_usuario_fecha ON ia_usage(usuario_id, fecha);
