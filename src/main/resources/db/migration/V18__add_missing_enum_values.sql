-- V18: Agregar valores faltantes a los ENUMs de Delivery y Ordenes
-- IMPORTANTE: ALTER TYPE ... ADD VALUE no puede ejecutarse dentro de un bloque DO en algunas versiones de Postgres.
-- Flyway ejecutará esto fuera de una transacción si es necesario.

ALTER TYPE delivery_status ADD VALUE IF NOT EXISTS 'INCIDENCIA';

ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'FALLIDO';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'ENVIADO';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'ENTREGADO';
