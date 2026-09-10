-- Add usuario_id to reclamos for tracking which user submitted the complaint
ALTER TABLE reclamos ADD COLUMN usuario_id BIGINT;

ALTER TABLE reclamos ADD CONSTRAINT fk_reclamos_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id_usuario);

CREATE INDEX idx_reclamos_usuario_id ON reclamos(usuario_id);
