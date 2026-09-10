-- Agrega código de referido único por usuario (aleatorio, alfanumérico).
-- Se genera on-demand cuando el usuario visita la página de referidos.
ALTER TABLE usuarios ADD COLUMN codigo_referral VARCHAR(20);

CREATE UNIQUE INDEX idx_usuarios_codigo_referral
    ON usuarios (codigo_referral)
    WHERE codigo_referral IS NOT NULL;
