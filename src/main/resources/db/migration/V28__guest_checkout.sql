-- V28: Guest Checkout support
-- Make usuario_cliente_id nullable so guest orders can exist without an account
ALTER TABLE ordenes ALTER COLUMN usuario_cliente_id DROP NOT NULL;

-- Add guest fields for orders placed without authentication
ALTER TABLE ordenes ADD COLUMN IF NOT EXISTS guest_email VARCHAR(255);
ALTER TABLE ordenes ADD COLUMN IF NOT EXISTS guest_nombre VARCHAR(255);

-- Ensure either usuario_cliente_id OR guest_email is present
ALTER TABLE ordenes ADD CONSTRAINT chk_orden_user_or_guest
    CHECK (usuario_cliente_id IS NOT NULL OR guest_email IS NOT NULL);
