-- Migración para añadir calificación de producto al sistema de delivery
ALTER TABLE deliveries 
ADD COLUMN calificacion_producto SMALLINT CHECK (calificacion_producto BETWEEN 1 AND 5),
ADD COLUMN comentario_producto TEXT;

-- Comentario para auditoría
COMMENT ON COLUMN deliveries.calificacion_producto IS 'Calificación del cliente sobre los productos recibidos';
COMMENT ON COLUMN deliveries.comentario_producto IS 'Comentario del cliente sobre los productos recibidos';
