-- Horario de atencion del veterinario independiente (mismo esquema que el de las empresas).
-- Un horario pertenece a una empresa o a un veterinario, nunca a ambos.
-- Si el veterinario no configura ninguno, sigue funcionando la propuesta libre de fecha y hora (V51).
ALTER TABLE horarios_atencion ALTER COLUMN empresa_id DROP NOT NULL;

ALTER TABLE horarios_atencion
    ADD COLUMN veterinario_id BIGINT REFERENCES veterinarios(id_veterinario) ON DELETE CASCADE;

ALTER TABLE horarios_atencion ADD CONSTRAINT chk_horario_empresa_o_veterinario
    CHECK ((empresa_id IS NOT NULL) <> (veterinario_id IS NOT NULL));

ALTER TABLE horarios_atencion ADD CONSTRAINT uq_horario_veterinario_dia
    UNIQUE (veterinario_id, dia_semana);

CREATE INDEX idx_horarios_atencion_veterinario ON horarios_atencion(veterinario_id);
