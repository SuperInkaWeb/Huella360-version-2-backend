-- Hash auth tokens: rename token column to token_hash
-- Auth tokens are short-lived, so dropping existing plaintext tokens is safe
ALTER TABLE auth_tokens DROP CONSTRAINT IF EXISTS auth_tokens_token_key;
ALTER TABLE auth_tokens DROP COLUMN IF EXISTS token;
-- Delete existing rows before adding NOT NULL column
DELETE FROM auth_tokens;
ALTER TABLE auth_tokens ADD COLUMN token_hash VARCHAR(500) NOT NULL;
ALTER TABLE auth_tokens ADD CONSTRAINT auth_tokens_token_hash_unique UNIQUE (token_hash);
CREATE INDEX idx_auth_tokens_token_hash ON auth_tokens(token_hash);
