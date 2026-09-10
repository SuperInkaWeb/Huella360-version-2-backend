-- Tabla de referidos
CREATE TABLE referidos (
    id_referido BIGSERIAL PRIMARY KEY,
    usuario_que_refirio_id BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    usuario_refirido_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    codigo_referido VARCHAR(50) UNIQUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices
CREATE INDEX idx_referidos_que_refirio ON referidos(usuario_que_refirio_id);
CREATE INDEX idx_referidos_codigo ON referidos(codigo_referido);
