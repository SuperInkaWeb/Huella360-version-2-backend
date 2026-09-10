-- 1. Configuracion de Puntos (Admin)
CREATE TABLE configuracion_puntos (
    id BIGSERIAL PRIMARY KEY,
    accion VARCHAR(100) NOT NULL UNIQUE, -- Ej: 'REGISTRO', 'PRIMERA_COMPRA', 'COMPRA', 'ADOPCION', 'SERVICIO', 'PRIMERA_MASCOTA', 'CALIFICAR_DELIVERY'
    puntos_otorgados INT NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    descripcion VARCHAR(255),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Inserciones de configuracion inicial
INSERT INTO configuracion_puntos (accion, puntos_otorgados, descripcion) VALUES
('REGISTRO', 50, 'Puntos otorgados al cliente al crear su cuenta'),
('PRIMERA_COMPRA', 100, 'Puntos otorgados por la primera compra en la plataforma'),
('COMPRA', 10, 'Puntos base otorgados por cada compra'),
('ADOPCION', 50, 'Puntos otorgados por concretar una adopción'),
('SERVICIO', 20, 'Puntos otorgados por contratar o asistir a un servicio'),
('PRIMERA_MASCOTA', 30, 'Puntos otorgados al registrar la primera mascota'),
('CALIFICAR_DELIVERY', 10, 'Puntos otorgados al dejar una reseña al repartidor');

-- 2. Puntos del Cliente (Balance)
CREATE TABLE puntos_cliente (
    id_perfil BIGINT PRIMARY KEY REFERENCES perfiles_clientes(id_perfil) ON DELETE CASCADE,
    puntos_totales INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Historial de Puntos (Ledger)
CREATE TABLE historial_puntos (
    id BIGSERIAL PRIMARY KEY,
    id_perfil BIGINT NOT NULL REFERENCES puntos_cliente(id_perfil) ON DELETE CASCADE,
    puntos INT NOT NULL,
    tipo_accion VARCHAR(100) NOT NULL,
    referencia_id BIGINT,
    descripcion VARCHAR(255),
    fecha TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Recompensas (Company Offers)
CREATE TABLE recompensas (
    id BIGSERIAL PRIMARY KEY,
    id_empresa BIGINT NOT NULL REFERENCES empresas(id_empresa) ON DELETE CASCADE,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    costo_puntos INT NOT NULL,
    tipo_descuento VARCHAR(50) NOT NULL CHECK (tipo_descuento IN ('PORCENTAJE', 'MONTO_FIJO')), 
    valor_descuento DECIMAL(10,2) NOT NULL,
    aplica_a_ciertos_productos BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Recompensas - Productos (Join Table for specific products)
CREATE TABLE recompensa_productos (
    id_recompensa BIGINT NOT NULL REFERENCES recompensas(id) ON DELETE CASCADE,
    id_producto BIGINT NOT NULL REFERENCES productos(id_producto) ON DELETE CASCADE,
    PRIMARY KEY (id_recompensa, id_producto)
);

-- 6. Canjes de Recompensas
CREATE TABLE canjes_recompensas (
    id BIGSERIAL PRIMARY KEY,
    id_perfil BIGINT NOT NULL REFERENCES puntos_cliente(id_perfil) ON DELETE CASCADE,
    id_recompensa BIGINT NOT NULL REFERENCES recompensas(id) ON DELETE CASCADE,
    fecha_canje TIMESTAMPTZ DEFAULT NOW(),
    utilizado BOOLEAN DEFAULT FALSE,
    fecha_utilizacion TIMESTAMPTZ,
    orden_id BIGINT REFERENCES ordenes(id_orden) ON DELETE SET NULL
);

-- Indices
CREATE INDEX idx_historial_puntos_perfil ON historial_puntos(id_perfil);
CREATE INDEX idx_recompensas_empresa ON recompensas(id_empresa);
CREATE INDEX idx_canjes_perfil ON canjes_recompensas(id_perfil);
