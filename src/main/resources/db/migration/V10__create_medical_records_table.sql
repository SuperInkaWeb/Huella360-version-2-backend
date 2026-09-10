-- Migración V10: Crear tabla de historias clínicas (medical records)

CREATE TABLE IF NOT EXISTS historias_clinicas (
    id_historia_clinica BIGSERIAL PRIMARY KEY,
    mascota_id BIGINT NOT NULL REFERENCES mascotas(id_mascota),
    veterinario_id BIGINT NOT NULL REFERENCES veterinarios(id_veterinario),
    cita_id BIGINT REFERENCES citas(id_cita),
    diagnostico TEXT NOT NULL,
    tratamiento TEXT NOT NULL,
    notas TEXT,
    peso_kg DECIMAL(5,2),
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para mejorar rendimiento de consultas de historial médico
CREATE INDEX idx_historial_mascota ON historias_clinicas(mascota_id);
CREATE INDEX idx_historial_vet ON historias_clinicas(veterinario_id);
CREATE INDEX idx_historial_cita ON historias_clinicas(cita_id);
