-- CONFIGURACION INICIAL
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; -- Recomendado si decidimos migrar IDs a UUID en el futuro
CREATE EXTENSION IF NOT EXISTS "citext";    -- Para emails insensibles a mayusculas/minusculas

-- DEFINICION DE ENUMS (Mas eficiente y limpio que CHECK strings)
CREATE TYPE user_role AS ENUM ('CLIENTE', 'EMPRESA', 'VETERINARIO', 'ADMIN');
CREATE TYPE verification_status AS ENUM ('PENDIENTE', 'VERIFICADO', 'RECHAZADO');
CREATE TYPE sex_type AS ENUM ('MACHO', 'HEMBRA');
CREATE TYPE product_status AS ENUM ('ACTIVO', 'INACTIVO', 'AGOTADO');
CREATE TYPE service_modality AS ENUM ('PRESENCIAL', 'VIRTUAL', 'DOMICILIO');
CREATE TYPE order_status AS ENUM ('PENDIENTE', 'PAGADO', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO', 'REEMBOLSADO');
CREATE TYPE appointment_status AS ENUM ('SOLICITADA', 'CONFIRMADA', 'RECHAZADA', 'COMPLETADA', 'CANCELADA', 'NOSHOW');

-- USUARIOS Y SEGURIDAD
CREATE TABLE usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    correo CITEXT NOT NULL UNIQUE, -- CITEXT maneja 'User@Example.com' igual que 'user@example.com'
    contrasenia VARCHAR(255) NOT NULL,
    rol user_role NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    email_verificado BOOLEAN DEFAULT FALSE,
    ultimo_login TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tokens de verificacion (Email, Password Reset)
CREATE TABLE auth_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    tipo VARCHAR(50) NOT NULL CHECK (tipo IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- TABLA DE REFRESH TOKENS PARA AUTENTICACION
CREATE TABLE refresh_tokens (
    id_token BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    token_hash VARCHAR(500) NOT NULL UNIQUE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- PERFILES DE ACTORES
CREATE TABLE perfiles_clientes (
    id_perfil BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    direccion VARCHAR(255),
    ciudad VARCHAR(100),
    pais VARCHAR(100) DEFAULT 'Perú',
    foto_perfil_url VARCHAR(255),
    ubicacion_lat DECIMAL(10,8),
    ubicacion_lng DECIMAL(11,8),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE empresas (
    id_empresa BIGSERIAL PRIMARY KEY,
    usuario_propietario_id BIGINT NOT NULL REFERENCES usuarios(id_usuario), -- El duenio de la cuenta
    nombre_comercial VARCHAR(150) NOT NULL,
    razon_social VARCHAR(200),
    ruc VARCHAR(20) UNIQUE,
    descripcion TEXT,
    tipo_servicio VARCHAR(50) CHECK (tipo_servicio IN ('VETERINARIA', 'PETSHOP', 'GROOMING', 'HIBRIDO')),
    telefono_contacto VARCHAR(20),
    email_contacto VARCHAR(100),
    direccion VARCHAR(255),
    ciudad VARCHAR(100),
    pais VARCHAR(100),
    ubicacion_lat DECIMAL(10,8), -- Importante para el mapa del Marketplace
    ubicacion_lng DECIMAL(11,8),
    logo_url VARCHAR(255),
    banner_url VARCHAR(255),
    estado_validacion verification_status DEFAULT 'PENDIENTE',
    documentos_url JSONB, -- Guardar URLs de RUC/Licencia en formato JSON
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE veterinarios (
    id_veterinario BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id_usuario),
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    especialidad VARCHAR(100),
    numero_colegiatura VARCHAR(50) UNIQUE,
    biografia TEXT,
    anios_experiencia INT DEFAULT 0,
    foto_perfil_url VARCHAR(255),
    estado_validacion verification_status DEFAULT 'PENDIENTE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Relación N:M (Veterinarios que trabajan en Empresas)
CREATE TABLE staff_veterinario (
    id_staff BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id_empresa) ON DELETE CASCADE,
    veterinario_id BIGINT NOT NULL REFERENCES veterinarios(id_veterinario) ON DELETE CASCADE,
    rol_interno VARCHAR(100), -- Ej: "Director Medico", "Cirujano"
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(empresa_id, veterinario_id)
);

-- 4. MASCOTAS
CREATE TABLE mascotas (
    id_mascota BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    nombre VARCHAR(50) NOT NULL,
    especie VARCHAR(50) NOT NULL, -- Perro, Gato, Conejo
    raza VARCHAR(100),
    sexo sex_type,
    fecha_nacimiento DATE,
    peso_kg DECIMAL(5,2),
    foto_url VARCHAR(255),
    esterilizado BOOLEAN DEFAULT FALSE,
    observaciones_medicas TEXT,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. CATALOGO (PRODUCTOS Y SERVICIOS)
CREATE TABLE categorias (
    id_categoria BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE, -- Para URLs amigables SEO
    padre_id BIGINT REFERENCES categorias(id_categoria), -- Para subcategorías recursivas
    icono_url VARCHAR(255)
);

CREATE TABLE productos (
    id_producto BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id_empresa) ON DELETE CASCADE,
    categoria_id BIGINT REFERENCES categorias(id_categoria),
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10,2) NOT NULL CHECK (precio >= 0),
    precio_oferta DECIMAL(10,2) CHECK (precio_oferta < precio),
    stock_actual INT NOT NULL DEFAULT 0,
    imagenes JSONB, -- Array de URLs de imágenes
    estado product_status DEFAULT 'ACTIVO',
    sku VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Unificación de Servicios (Tanto de Empresa como de Veterinario independiente)
CREATE TABLE servicios (
    id_servicio BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT REFERENCES empresas(id_empresa) ON DELETE CASCADE,
    veterinario_id BIGINT REFERENCES veterinarios(id_veterinario) ON DELETE CASCADE,
    -- Constraint: Un servicio pertenece a una empresa O a un vet independiente, no ambos (o ambos si es staff)
    -- Para simplificar SaaS: Generalmente la Empresa factura. Si es vet independiente, se registra como Empresa unipersonal.
    -- Asumiremos que el servicio está ligado a una entidad facturable (Empresa).

    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10,2) NOT NULL CHECK (precio >= 0),
    duracion_minutos INT DEFAULT 30,
    modalidad service_modality DEFAULT 'PRESENCIAL',
    activo BOOLEAN DEFAULT TRUE,

    CHECK (empresa_id IS NOT NULL OR veterinario_id IS NOT NULL)
);

-- MARKETPLACE Y ORDENES (Corazon transaccional)
CREATE TABLE ordenes (
    id_orden BIGSERIAL PRIMARY KEY,
    codigo_orden VARCHAR(20) NOT NULL UNIQUE, -- Ej: ORD-2024-001
    usuario_cliente_id BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    empresa_id BIGINT NOT NULL REFERENCES empresas(id_empresa), -- Una orden pertenece a una tienda especifica (Modelo Amazon)

    subtotal DECIMAL(10,2) NOT NULL,
    costo_envio DECIMAL(10,2) DEFAULT 0,
    comision_plataforma DECIMAL(10,2) DEFAULT 0, -- Tu ganancia SaaS
    total DECIMAL(10,2) NOT NULL,

    estado order_status DEFAULT 'PENDIENTE',
    metodo_pago VARCHAR(50), -- MercadoPago, Yape, Tarjeta
    referencia_pago_externa VARCHAR(255), -- ID de MercadoPago

    direccion_envio JSONB, -- Snapshot de la direccion al momento de compra

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Detalle Polimorfico Seguro
CREATE TABLE detalles_orden (
    id_detalle BIGSERIAL PRIMARY KEY,
    orden_id BIGINT NOT NULL REFERENCES ordenes(id_orden) ON DELETE CASCADE,

    -- Link a Producto O Servicio (Integridad Referencial Estricta)
    producto_id BIGINT REFERENCES productos(id_producto),
    servicio_id BIGINT REFERENCES servicios(id_servicio),

    cantidad INT NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,

    metadata JSONB, -- Para detalles extra (Ej: Talla, Color, Nombre de la mascota para el servicio)

    CONSTRAINT check_item_type CHECK (
        (producto_id IS NOT NULL AND servicio_id IS NULL) OR
        (producto_id IS NULL AND servicio_id IS NOT NULL)
    )
);

-- AGENDAMIENTO
CREATE TABLE citas (
    id_cita BIGSERIAL PRIMARY KEY,
    usuario_cliente_id BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    mascota_id BIGINT REFERENCES mascotas(id_mascota),
    servicio_id BIGINT NOT NULL REFERENCES servicios(id_servicio),
    empresa_id BIGINT NOT NULL REFERENCES empresas(id_empresa),
    veterinario_asignado_id BIGINT REFERENCES veterinarios(id_veterinario), -- Opcional, puede ser cualquiera de la empresa

    fecha_programada DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,

    estado appointment_status DEFAULT 'SOLICITADA',
    orden_asociada_id BIGINT REFERENCES ordenes(id_orden), -- Si se pagó por adelantado

    notas_cliente TEXT,
    notas_internas TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ADOPCIONES
CREATE TABLE adopciones (
    id_adopcion BIGSERIAL PRIMARY KEY,
    mascota_id BIGINT NOT NULL REFERENCES mascotas(id_mascota),
    publicado_por_id BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    titulo VARCHAR(200) NOT NULL,
    historia TEXT,
    requisitos TEXT,
    estado VARCHAR(20) DEFAULT 'DISPONIBLE' CHECK (estado IN ('DISPONIBLE', 'EN_PROCESO', 'ADOPTADO', 'PAUSADO')),
    ubicacion_ciudad VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE solicitudes_adopcion (
    id_solicitud BIGSERIAL PRIMARY KEY,
    adopcion_id BIGINT NOT NULL REFERENCES adopciones(id_adopcion),
    interesado_id BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    mensaje_presentacion TEXT,
    estado VARCHAR(20) DEFAULT 'PENDIENTE',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- INDICES
CREATE INDEX idx_productos_empresa ON productos(empresa_id);
CREATE INDEX idx_productos_categoria ON productos(categoria_id);
CREATE INDEX idx_ordenes_usuario ON ordenes(usuario_cliente_id);
CREATE INDEX idx_ordenes_empresa ON ordenes(empresa_id);
CREATE INDEX idx_citas_fecha ON citas(fecha_programada);
CREATE INDEX idx_empresas_geo ON empresas(ciudad, pais);
CREATE INDEX idx_refresh_tokens_usuario ON refresh_tokens(usuario_id);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(usuario_id, revoked) WHERE revoked = FALSE;
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);