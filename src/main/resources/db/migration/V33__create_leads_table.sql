-- Tabla de leads para B2B
CREATE TABLE leads (
    id_lead BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id_empresa) ON DELETE CASCADE,
    cliente_nombre VARCHAR(255) NOT NULL,
    cliente_email VARCHAR(255),
    cliente_telefono VARCHAR(50),
    servicio_solicitado VARCHAR(255),
    mensaje TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'NUEVO' CHECK (estado IN ('NUEVO', 'EN_CONTACTO', 'CONVERTIDO', 'PERDIDO')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices
CREATE INDEX idx_leads_empresa ON leads(empresa_id);
CREATE INDEX idx_leads_estado ON leads(estado);
CREATE INDEX idx_leads_created ON leads(empresa_id, created_at DESC);
