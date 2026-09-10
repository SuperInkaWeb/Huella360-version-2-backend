-- Permite reutilizar un código de referido (H360-<id_referente>) entre varios
-- usuarios referidos del mismo referente. Antes tenía UNIQUE y solo permitía
-- referir a 1 persona, rompiendo el desbloqueo por 10 referidos.
ALTER TABLE referidos DROP CONSTRAINT IF EXISTS referidos_codigo_referido_key;
