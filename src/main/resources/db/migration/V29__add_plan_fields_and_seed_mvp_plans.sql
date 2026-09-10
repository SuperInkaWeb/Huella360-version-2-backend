-- Agregar nuevas columnas a la tabla planes
ALTER TABLE planes ADD COLUMN IF NOT EXISTS limite_servicios INT DEFAULT 0;
ALTER TABLE planes ADD COLUMN IF NOT EXISTS limite_recordatorios INT DEFAULT 0;
ALTER TABLE planes ADD COLUMN IF NOT EXISTS limite_ia_uso INT DEFAULT 0;
ALTER TABLE planes ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'B2B';

-- Agregar columna usuario_id a suscripciones para soportar B2C
ALTER TABLE suscripciones ADD COLUMN IF NOT EXISTS usuario_id BIGINT UNIQUE REFERENCES usuarios(id_usuario) ON DELETE CASCADE;

-- Actualizar check constraint para soportar B2C (empresa OR veterinario OR usuario)
ALTER TABLE suscripciones DROP CONSTRAINT IF EXISTS check_suscripcion_owner;
ALTER TABLE suscripciones ADD CONSTRAINT check_suscripcion_owner CHECK (
    (empresa_id IS NOT NULL AND veterinario_id IS NULL AND usuario_id IS NULL) OR
    (empresa_id IS NULL AND veterinario_id IS NOT NULL AND usuario_id IS NULL) OR
    (empresa_id IS NULL AND veterinario_id IS NULL AND usuario_id IS NOT NULL)
);

-- Insertar planes B2C (Huella) con ON CONFLICT para no destruir datos existentes
INSERT INTO planes (nombre, descripcion, precio_mensual, limite_mascotas, limite_productos, limite_servicios, limite_recordatorios, limite_ia_uso, tipo, activo) VALUES
('Huella Básica', 'Plan gratis para dueños casuales. Perfil de 1 mascota, historial médico básico y reservas de citas.', 0.00, 1, 0, 0, 3, 0, 'B2C', true),
('Huella Care', 'El cuidado que tu peludo merece. Historial clínico completo, teleconsultas ilimitadas y alertas de IA.', 14.90, 4, 0, 0, -1, 50, 'B2C', true),
('Huella Premium', 'Vida premium para tu familia peluda. Hasta 8 mascotas, GPS, seguro pet y comunidad exclusiva.', 29.90, 8, 0, 0, -1, 200, 'B2C', true)
ON CONFLICT (nombre) DO UPDATE SET
    descripcion = EXCLUDED.descripcion,
    precio_mensual = EXCLUDED.precio_mensual,
    limite_mascotas = EXCLUDED.limite_mascotas,
    limite_productos = EXCLUDED.limite_productos,
    limite_servicios = EXCLUDED.limite_servicios,
    limite_recordatorios = EXCLUDED.limite_recordatorios,
    limite_ia_uso = EXCLUDED.limite_ia_uso,
    tipo = EXCLUDED.tipo,
    activo = EXCLUDED.activo;

-- Insertar planes B2B (Negocio) con ON CONFLICT para no destruir datos existentes
INSERT INTO planes (nombre, descripcion, precio_mensual, limite_mascotas, limite_productos, limite_servicios, limite_recordatorios, limite_ia_uso, tipo, activo) VALUES
('Huella Free B2B', 'Presencia básica gratis. Perfil público y hasta 4 servicios/productos listados.', 0.00, 0, 4, 4, 0, 0, 'B2B', true),
('Negocio Starter', 'Crece desde el primer día. Hasta 30 productos/servicios, agenda de citas y estadísticas.', 49.00, 0, 30, 30, 0, 0, 'B2B', true),
('Negocio Pro', 'Acelera tu crecimiento. Productos/servicios ilimitados, marketing automatizado y delivery.', 129.00, 0, -1, -1, 0, 0, 'B2B', true)
ON CONFLICT (nombre) DO UPDATE SET
    descripcion = EXCLUDED.descripcion,
    precio_mensual = EXCLUDED.precio_mensual,
    limite_mascotas = EXCLUDED.limite_mascotas,
    limite_productos = EXCLUDED.limite_productos,
    limite_servicios = EXCLUDED.limite_servicios,
    limite_recordatorios = EXCLUDED.limite_recordatorios,
    limite_ia_uso = EXCLUDED.limite_ia_uso,
    tipo = EXCLUDED.tipo,
    activo = EXCLUDED.activo;

-- Índices
CREATE INDEX IF NOT EXISTS idx_planes_tipo ON planes(tipo);
CREATE INDEX IF NOT EXISTS idx_planes_activo ON planes(activo);
CREATE INDEX IF NOT EXISTS idx_suscripciones_usuario ON suscripciones(usuario_id);
