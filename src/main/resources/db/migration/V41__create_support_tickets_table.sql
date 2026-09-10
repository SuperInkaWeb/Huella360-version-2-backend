-- Migración V41: Canal de soporte del negocio (tickets a la plataforma)

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ticket_status') THEN
        CREATE TYPE ticket_status AS ENUM ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ticket_priority') THEN
        CREATE TYPE ticket_priority AS ENUM ('LOW', 'NORMAL', 'HIGH', 'URGENT');
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS support_tickets (
    id_ticket   BIGSERIAL PRIMARY KEY,
    public_id   UUID NOT NULL UNIQUE,
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    asunto      VARCHAR(255) NOT NULL,
    descripcion TEXT NOT NULL,
    estado      ticket_status DEFAULT 'OPEN',
    prioridad   ticket_priority DEFAULT 'NORMAL',
    categoria   VARCHAR(50),
    resolved_at TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índice para listar los tickets de un negocio por fecha
CREATE INDEX IF NOT EXISTS idx_support_tickets_usuario ON support_tickets(usuario_id, created_at DESC);
