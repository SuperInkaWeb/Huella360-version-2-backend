-- ==============================================================================
-- V14: SISTEMA DE DELIVERY
-- ==============================================================================

-- Nuevos ENUMs
CREATE TYPE repartidor_status AS ENUM ('DISPONIBLE', 'OCUPADO', 'INACTIVO', 'OFFLINE');
CREATE TYPE vehicle_type      AS ENUM ('MOTO', 'BICICLETA', 'AUTO', 'A_PIE');
CREATE TYPE delivery_status   AS ENUM (
    'BUSCANDO_REPARTIDOR',
    'REPARTIDOR_ASIGNADO',
    'EN_TIENDA',
    'RECOGIDO',
    'EN_CAMINO',
    'CERCA',
    'ENTREGADO',
    'FALLIDO',
    'CANCELADO'
);

-- Nuevo rol en user_role (si no lo tienes)
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'REPARTIDOR';

-- REPARTIDORES
CREATE TABLE repartidores (
    id_repartidor           BIGSERIAL PRIMARY KEY,
    usuario_id              BIGINT NOT NULL UNIQUE REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    nombres                 VARCHAR(100) NOT NULL,
    apellidos               VARCHAR(100) NOT NULL,
    telefono                VARCHAR(20),
    dni                     VARCHAR(20) UNIQUE,
    foto_perfil_url         VARCHAR(255),
    tipo_vehiculo           vehicle_type DEFAULT 'MOTO',
    placa_vehiculo          VARCHAR(20),
    estado_validacion       verification_status DEFAULT 'PENDIENTE',
    estado_actual           repartidor_status DEFAULT 'OFFLINE',
    ubicacion_lat           DECIMAL(10,8),
    ubicacion_lng           DECIMAL(11,8),
    ultima_ubicacion_at     TIMESTAMPTZ,
    calificacion_promedio   DECIMAL(3,2) DEFAULT 5.00,
    total_entregas          INT DEFAULT 0,
    activo                  BOOLEAN DEFAULT TRUE,
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

-- ZONAS DE COBERTURA por empresa
CREATE TABLE zonas_cobertura (
    id_zona         BIGSERIAL PRIMARY KEY,
    empresa_id      BIGINT NOT NULL REFERENCES empresas(id_empresa) ON DELETE CASCADE,
    nombre          VARCHAR(100) NOT NULL,
    radio_km        DECIMAL(5,2),
    costo_envio     DECIMAL(10,2) DEFAULT 0,
    activo          BOOLEAN DEFAULT TRUE
);

-- DELIVERIES (corazon del sistema)
CREATE TABLE deliveries (
    id_delivery             BIGSERIAL PRIMARY KEY,
    orden_id                BIGINT NOT NULL UNIQUE REFERENCES ordenes(id_orden),
    repartidor_id           BIGINT REFERENCES repartidores(id_repartidor),
    zona_id                 BIGINT REFERENCES zonas_cobertura(id_zona),

    origen_lat              DECIMAL(10,8) NOT NULL,
    origen_lng              DECIMAL(11,8) NOT NULL,
    origen_direccion        VARCHAR(255),

    destino_lat             DECIMAL(10,8) NOT NULL,
    destino_lng             DECIMAL(11,8) NOT NULL,
    destino_direccion       VARCHAR(255) NOT NULL,
    destino_referencia      VARCHAR(255),

    estado                  delivery_status DEFAULT 'BUSCANDO_REPARTIDOR',
    distancia_km            DECIMAL(6,2),
    tiempo_estimado_min     INT,
    costo_delivery          DECIMAL(10,2) NOT NULL DEFAULT 0,

    -- Timestamps por cada fase (para analytics)
    asignado_at             TIMESTAMPTZ,
    en_tienda_at            TIMESTAMPTZ,
    recogido_at             TIMESTAMPTZ,
    entregado_at            TIMESTAMPTZ,

    -- OTP confirmacion (se guarda hash BCrypt)
    codigo_confirmacion     VARCHAR(255),
    codigo_expira_at        TIMESTAMPTZ,

    -- Foto de entrega (Cloudinary)
    foto_entrega_url        VARCHAR(255),

    -- Calificaciones
    calificacion_cliente        SMALLINT CHECK (calificacion_cliente BETWEEN 1 AND 5),
    calificacion_repartidor     SMALLINT CHECK (calificacion_repartidor BETWEEN 1 AND 5),
    comentario_cliente          TEXT,

    intentos_asignacion     INT DEFAULT 0,

    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

-- HISTORIAL DE ESTADOS (auditoria completa)
CREATE TABLE delivery_estados (
    id              BIGSERIAL PRIMARY KEY,
    delivery_id     BIGINT NOT NULL REFERENCES deliveries(id_delivery) ON DELETE CASCADE,
    estado          delivery_status NOT NULL,
    descripcion     TEXT,
    registrado_by   BIGINT REFERENCES usuarios(id_usuario),
    registrado_at   TIMESTAMPTZ DEFAULT NOW()
);

-- TRACKING GPS (puntos guardados periodicamente, buffer en Redis)
CREATE TABLE tracking_repartidor (
    id              BIGSERIAL PRIMARY KEY,
    delivery_id     BIGINT NOT NULL REFERENCES deliveries(id_delivery) ON DELETE CASCADE,
    repartidor_id   BIGINT NOT NULL REFERENCES repartidores(id_repartidor),
    lat             DECIMAL(10,8) NOT NULL,
    lng             DECIMAL(11,8) NOT NULL,
    velocidad_kmh   DECIMAL(5,2),
    registrado_at   TIMESTAMPTZ DEFAULT NOW()
);

-- FK en ordenes (relacion inversa)
ALTER TABLE ordenes ADD COLUMN delivery_id BIGINT REFERENCES deliveries(id_delivery);

-- INDICES
CREATE INDEX idx_deliveries_orden          ON deliveries(orden_id);
CREATE INDEX idx_deliveries_repartidor     ON deliveries(repartidor_id);
CREATE INDEX idx_deliveries_estado         ON deliveries(estado);
CREATE INDEX idx_deliveries_busqueda       ON deliveries(estado, created_at)
    WHERE estado = 'BUSCANDO_REPARTIDOR';
CREATE INDEX idx_tracking_delivery         ON tracking_repartidor(delivery_id, registrado_at DESC);
CREATE INDEX idx_repartidores_estado       ON repartidores(estado_actual, activo);
CREATE INDEX idx_repartidores_geo          ON repartidores(ubicacion_lat, ubicacion_lng)
    WHERE estado_actual = 'DISPONIBLE' AND activo = TRUE;
CREATE INDEX idx_zonas_empresa             ON zonas_cobertura(empresa_id);
CREATE INDEX idx_delivery_estados_delivery ON delivery_estados(delivery_id);
