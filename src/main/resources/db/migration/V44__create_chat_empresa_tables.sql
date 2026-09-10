-- Chat en tiempo real entre cliente y empresa (consulta rapida sin cita previa)

CREATE TABLE chat_rooms (
    id_chat_room BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id_empresa) ON DELETE CASCADE,
    cliente_id BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_chat_room_empresa_cliente UNIQUE (empresa_id, cliente_id)
);

CREATE TABLE chat_mensajes_empresa (
    id_mensaje BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL REFERENCES chat_rooms(id_chat_room) ON DELETE CASCADE,
    remitente_id BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    contenido TEXT NOT NULL,
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_chat_rooms_empresa ON chat_rooms(empresa_id);
CREATE INDEX idx_chat_rooms_cliente ON chat_rooms(cliente_id);
CREATE INDEX idx_chat_mensajes_empresa_room ON chat_mensajes_empresa(chat_room_id, created_at);
