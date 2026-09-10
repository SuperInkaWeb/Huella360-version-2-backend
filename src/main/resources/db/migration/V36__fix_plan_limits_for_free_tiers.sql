-- Corregir el plan "Basico" (B2B) que tiene limites en 0 (tratados como ilimitados)
-- Se actualiza con limites reales para el plan gratuito B2B
UPDATE planes
SET limite_servicios = 5,
    limite_recordatorios = 3,
    limite_ia_uso = 0
WHERE nombre = 'Basico' AND tipo = 'B2B';

-- Corregir el plan "Huella Básica" (B2C) - limite_ia_uso=0 ya es correcto (sin IA)
-- Solo asegurar que tiene los valores correctos
UPDATE planes
SET limite_ia_uso = 0,
    limite_recordatorios = 3,
    limite_mascotas = 1
WHERE nombre = 'Huella Básica' AND tipo = 'B2C';
