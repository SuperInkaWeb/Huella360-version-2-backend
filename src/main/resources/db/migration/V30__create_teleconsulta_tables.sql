-- Tabla de consultas (teleconsultas)
CREATE TABLE consultas (
    id_consulta BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    veterinario_id BIGINT NOT NULL REFERENCES veterinarios(id_veterinario) ON DELETE CASCADE,
    mascota_id BIGINT REFERENCES mascotas(id_mascota) ON DELETE SET NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'EN_CURSO', 'FINALIZADA', 'CANCELADA')),
    jitsi_room_id VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tabla de mensajes de chat
CREATE TABLE chat_mensajes (
    id_mensaje BIGSERIAL PRIMARY KEY,
    consulta_id BIGINT NOT NULL REFERENCES consultas(id_consulta) ON DELETE CASCADE,
    remitente_id BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    contenido TEXT NOT NULL,
    tipo VARCHAR(20) DEFAULT 'TEXTO' CHECK (tipo IN ('TEXTO', 'IMAGEN', 'SISTEMA')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices
CREATE INDEX idx_consultas_cliente ON consultas(cliente_id);
CREATE INDEX idx_consultas_veterinario ON consultas(veterinario_id);
CREATE INDEX idx_consultas_estado ON consultas(estado);
CREATE INDEX idx_chat_mensajes_consulta ON chat_mensajes(consulta_id);
CREATE INDEX idx_chat_mensajes_created ON chat_mensajes(consulta_id, created_at);
