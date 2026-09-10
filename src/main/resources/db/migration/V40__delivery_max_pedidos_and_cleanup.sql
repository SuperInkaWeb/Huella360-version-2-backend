-- V41: Delivery refactor - max pedidos simultaneos + limpieza de scheduler
-- PUNTO 1: Campo max_pedidos_simultaneos en repartidores (default 3)
-- PUNTO 4: Documentación explícita del comportamiento sin scheduler

ALTER TABLE repartidores
ADD COLUMN IF NOT EXISTS max_pedidos_simultaneos INTEGER NOT NULL DEFAULT 3;

COMMENT ON TABLE deliveries IS 'Deliveries - modelo sin autoasignación. El repartidor decide si toma el pedido. Sin scheduler.';
COMMENT ON COLUMN repartidores.max_pedidos_simultaneos IS 'Límite de pedidos activos por repartidor. Bloqueo pesimista PESSIMISTIC_WRITE en aceptarPedido() previene race condition.';
