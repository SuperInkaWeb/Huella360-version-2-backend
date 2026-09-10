-- TABLA DE PLANES (Definición de tiers de suscripción)
CREATE TABLE planes (
    id_plan BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    precio_mensual DECIMAL(10,2) NOT NULL DEFAULT 0,
    limite_mascotas INT DEFAULT 0, -- 0 puede significar ilimitado en lógica de negocio
    limite_productos INT DEFAULT 0,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- TABLA DE SUSCRIPCIONES (Relación entre Empresa y Plan)
CREATE TABLE suscripciones (
    id_suscripcion BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL UNIQUE REFERENCES empresas(id_empresa) ON DELETE CASCADE,
    plan_id BIGINT NOT NULL REFERENCES planes(id_plan),
    
    fecha_inicio TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_fin TIMESTAMPTZ, -- Si es null, podría ser una suscripción vitalicia o manual
    
    estado VARCHAR(20) DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'CANCELADA', 'EXPIRADA', 'PENDIENTE_PAGO')),
    
    mp_preapproval_id VARCHAR(255), -- ID de suscripción en Mercado Pago (Suscripciones recurrentes)
    mp_next_payment_date TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Insertar planes iniciales por defecto
INSERT INTO planes (nombre, descripcion, precio_mensual, limite_mascotas, limite_productos) VALUES
('Básico', 'Plan gratuito para pequeñas veterinarias independientes.', 0.00, 10, 5),
('Pro', 'Ideal para clínicas en crecimiento.', 49.90, 100, 50),
('Premium', 'Sin límites para grandes hospitales veterinarios.', 99.90, 0, 0);

-- Índices
CREATE INDEX idx_suscripciones_empresa ON suscripciones(empresa_id);
CREATE INDEX idx_suscripciones_estado ON suscripciones(estado);
