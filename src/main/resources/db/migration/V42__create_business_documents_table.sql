-- Migración V42: Verificación de identidad de la empresa (KYC) - documentos
-- Nota: depende de que V41 (support_tickets) se aplique antes; mantener el orden al mergear.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'document_status') THEN
        CREATE TYPE document_status AS ENUM ('PENDING', 'VERIFIED', 'REJECTED');
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS business_documents (
    id_documento     BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT NOT NULL REFERENCES empresas(id_empresa),
    tipo_documento   VARCHAR(50) NOT NULL,
    numero_documento VARCHAR(100) NOT NULL,
    archivo_url      VARCHAR(500) NOT NULL,
    estado           document_status DEFAULT 'PENDING',
    revisado_at      TIMESTAMP,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_business_documents_empresa ON business_documents(empresa_id, created_at DESC);
