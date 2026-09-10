-- ============================================================
-- V39: Corregir plan "Básico" legacy y migrar suscripciones
-- ============================================================

-- Desactivar el plan "Básico" legacy (con tilde) que V38 no alcanzó a desactivar
UPDATE planes SET activo = FALSE WHERE nombre = 'Básico';

-- Asegurar que todos los planes legacy estén desactivados
UPDATE planes SET activo = FALSE WHERE nombre IN ('Basico', 'Pro', 'Premium', 'Negocio Starter + Marketplace');

-- Migrar suscripciones activas que aún apuntan al plan "Básico" o "Basico"
UPDATE suscripciones s
SET plan_id = (SELECT id_plan FROM planes WHERE nombre = 'Huella Free B2B' AND activo = TRUE LIMIT 1)
WHERE s.plan_id IN (SELECT id_plan FROM planes WHERE nombre IN ('Basico', 'Básico'))
  AND s.estado = 'ACTIVA';
