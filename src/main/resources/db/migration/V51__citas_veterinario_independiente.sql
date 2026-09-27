-- V2: reserva con veterinario independiente (sin horario fijo).
-- El cliente propone fecha y hora y el veterinario confirma o rechaza desde su Agenda.
-- Esas citas no pertenecen a ninguna empresa, pero toda cita debe tener empresa o veterinario.
ALTER TABLE citas ALTER COLUMN empresa_id DROP NOT NULL;

ALTER TABLE citas ADD CONSTRAINT chk_citas_empresa_o_veterinario
    CHECK (empresa_id IS NOT NULL OR veterinario_asignado_id IS NOT NULL);
