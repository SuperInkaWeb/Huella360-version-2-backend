ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS auth0_sub VARCHAR(255) UNIQUE;
CREATE INDEX IF NOT EXISTS idx_usuarios_auth0_sub ON usuarios(auth0_sub);
