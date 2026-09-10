-- Migracion V40: Horario de atencion + cupo por dia de la semana, por empresa

CREATE TABLE IF NOT EXISTS horarios_atencion (
    id_horario BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id_empresa) ON DELETE CASCADE,
    dia_semana VARCHAR(10) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    capacidad INT NOT NULL DEFAULT 1,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_horario_empresa_dia UNIQUE (empresa_id, dia_semana)
);

CREATE INDEX idx_horarios_atencion_empresa ON horarios_atencion(empresa_id);
