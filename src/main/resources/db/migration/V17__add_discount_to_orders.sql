-- Agregar columna de descuento a las órdenes
ALTER TABLE ordenes ADD COLUMN descuento NUMERIC(10, 2) DEFAULT 0.00;
