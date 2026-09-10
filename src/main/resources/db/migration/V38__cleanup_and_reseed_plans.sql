-- ============================================================
-- V38: Limpiar planes duplicados y resembrar según spec Huella360
-- ============================================================

-- Desactivar todos los planes existentes (limpieza segura)
UPDATE planes SET activo = FALSE;

-- ══════════════════════════════════════════════════════════════
-- B2C — Planes para dueños de mascotas
-- ══════════════════════════════════════════════════════════════

-- Plan Gratis B2C
INSERT INTO planes (nombre, descripcion, precio_mensual, limite_mascotas, limite_productos, limite_servicios, limite_recordatorios, limite_ia_uso, tipo, activo)
VALUES (
    'Huella Básica',
    'Historial médico digital básico, recordatorios de vacunas (hasta 3/mes), reserva de citas, directorio pet, perfil de 1 mascota. Recomienda 10 usuarios para desbloquear 2da mascota.',
    0.00, 1, 0, 0, 3, 0, 'B2C', TRUE
)
ON CONFLICT (nombre) DO UPDATE SET
    descripcion = EXCLUDED.descripcion,
    precio_mensual = EXCLUDED.precio_mensual,
    limite_mascotas = EXCLUDED.limite_mascotas,
    limite_productos = EXCLUDED.limite_productos,
    limite_servicios = EXCLUDED.limite_servicios,
    limite_recordatorios = EXCLUDED.limite_recordatorios,
    limite_ia_uso = EXCLUDED.limite_ia_uso,
    tipo = EXCLUDED.tipo,
    activo = TRUE;

-- Plan Huella Care B2C
INSERT INTO planes (nombre, descripcion, precio_mensual, limite_mascotas, limite_productos, limite_servicios, limite_recordatorios, limite_ia_uso, tipo, activo)
VALUES (
    'Huella Care',
    'Todo lo del plan Básico + historial clínico completo con IA (50 consultas/mes), recordatorios inteligentes, teleconsultas ilimitadas, historial compartido con vets, carnet digital, mapa en tiempo real, descuentos en marketplace. Hasta 4 mascotas.',
    14.90, 4, 0, 0, -1, 50, 'B2C', TRUE
)
ON CONFLICT (nombre) DO UPDATE SET
    descripcion = EXCLUDED.descripcion,
    precio_mensual = EXCLUDED.precio_mensual,
    limite_mascotas = EXCLUDED.limite_mascotas,
    limite_productos = EXCLUDED.limite_productos,
    limite_servicios = EXCLUDED.limite_servicios,
    limite_recordatorios = EXCLUDED.limite_recordatorios,
    limite_ia_uso = EXCLUDED.limite_ia_uso,
    tipo = EXCLUDED.tipo,
    activo = TRUE;

-- Plan Huella Premium B2C
INSERT INTO planes (nombre, descripcion, precio_mensual, limite_mascotas, limite_productos, limite_servicios, limite_recordatorios, limite_ia_uso, tipo, activo)
VALUES (
    'Huella Premium',
    'Todo de Care + usuarios familiares, alertas compartidas, álbum de recuerdos, GPS + collar inteligente, seguro pet básico, consultas prioritarias, comunidad exclusiva con IA, hasta 8 mascotas con reportes mensuales.',
    29.90, 8, 0, 0, -1, 200, 'B2C', TRUE
)
ON CONFLICT (nombre) DO UPDATE SET
    descripcion = EXCLUDED.descripcion,
    precio_mensual = EXCLUDED.precio_mensual,
    limite_mascotas = EXCLUDED.limite_mascotas,
    limite_productos = EXCLUDED.limite_productos,
    limite_servicios = EXCLUDED.limite_servicios,
    limite_recordatorios = EXCLUDED.limite_recordatorios,
    limite_ia_uso = EXCLUDED.limite_ia_uso,
    tipo = EXCLUDED.tipo,
    activo = TRUE;

-- ══════════════════════════════════════════════════════════════
-- B2B — Planes para empresas / veterinarias
-- ══════════════════════════════════════════════════════════════

-- Plan Huella Free B2B
INSERT INTO planes (nombre, descripcion, precio_mensual, limite_mascotas, limite_productos, limite_servicios, limite_recordatorios, limite_ia_uso, tipo, activo)
VALUES (
    'Huella Free B2B',
    'Perfil público básico, hasta 4 servicios/productos, formulario de contacto, aparece en directorio, panel de leads básico, estadísticas básicas.',
    0.00, 0, 4, 4, 0, 0, 'B2B', TRUE
)
ON CONFLICT (nombre) DO UPDATE SET
    descripcion = EXCLUDED.descripcion,
    precio_mensual = EXCLUDED.precio_mensual,
    limite_mascotas = EXCLUDED.limite_mascotas,
    limite_productos = EXCLUDED.limite_productos,
    limite_servicios = EXCLUDED.limite_servicios,
    limite_recordatorios = EXCLUDED.limite_recordatorios,
    limite_ia_uso = EXCLUDED.limite_ia_uso,
    tipo = EXCLUDED.tipo,
    activo = TRUE;

-- Plan Negocio Starter
INSERT INTO planes (nombre, descripcion, precio_mensual, limite_mascotas, limite_productos, limite_servicios, limite_recordatorios, limite_ia_uso, tipo, activo)
VALUES (
    'Negocio Starter',
    'Perfil destacado, agenda de citas + recordatorios, catálogo de 30 productos/servicios, gestión de inventario, acceso a leads en tu zona, estadísticas de rendimiento.',
    49.00, 0, 30, 30, 0, 0, 'B2B', TRUE
)
ON CONFLICT (nombre) DO UPDATE SET
    descripcion = EXCLUDED.descripcion,
    precio_mensual = EXCLUDED.precio_mensual,
    limite_mascotas = EXCLUDED.limite_mascotas,
    limite_productos = EXCLUDED.limite_productos,
    limite_servicios = EXCLUDED.limite_servicios,
    limite_recordatorios = EXCLUDED.limite_recordatorios,
    limite_ia_uso = EXCLUDED.limite_ia_uso,
    tipo = EXCLUDED.tipo,
    activo = TRUE;

-- Plan Negocio Pro
INSERT INTO planes (nombre, descripcion, precio_mensual, limite_mascotas, limite_productos, limite_servicios, limite_recordatorios, limite_ia_uso, tipo, activo)
VALUES (
    'Negocio Pro',
    'Todo del Starter + tienda online integrada, marketing automatizado (1 campaña/mes email), analíticas avanzadas, 5 ubicaciones, promociones destacadas con IA, integración delivery, soporte prioritario.',
    129.00, 0, -1, -1, 0, 0, 'B2B', TRUE
)
ON CONFLICT (nombre) DO UPDATE SET
    descripcion = EXCLUDED.descripcion,
    precio_mensual = EXCLUDED.precio_mensual,
    limite_mascotas = EXCLUDED.limite_mascotas,
    limite_productos = EXCLUDED.limite_productos,
    limite_servicios = EXCLUDED.limite_servicios,
    limite_recordatorios = EXCLUDED.limite_recordatorios,
    limite_ia_uso = EXCLUDED.limite_ia_uso,
    tipo = EXCLUDED.tipo,
    activo = TRUE;

-- Eliminar planes legacy duplicados que ya no se usan
UPDATE planes SET activo = FALSE WHERE nombre = 'Basico';
UPDATE planes SET activo = FALSE WHERE nombre = 'Pro';
UPDATE planes SET activo = FALSE WHERE nombre = 'Premium';
UPDATE planes SET activo = FALSE WHERE nombre = 'Negocio Starter + Marketplace';

-- Migrar suscripciones que usan el plan "Basico" al "Huella Free B2B"
UPDATE suscripciones s
SET plan_id = (SELECT id_plan FROM planes WHERE nombre = 'Huella Free B2B' AND activo = TRUE LIMIT 1)
WHERE s.plan_id = (SELECT id_plan FROM planes WHERE nombre = 'Basico')
  AND s.estado = 'ACTIVA';
