CREATE TABLE reclamos (
    id BIGSERIAL PRIMARY KEY,
    tipo_documento VARCHAR(50),
    numero_documento VARCHAR(50),
    primer_nombre VARCHAR(100),
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100),
    segundo_apellido VARCHAR(100),
    direccion VARCHAR(255),
    departamento VARCHAR(100),
    provincia VARCHAR(100),
    distrito VARCHAR(100),
    correo VARCHAR(255),
    telefono VARCHAR(50),
    es_menor BOOLEAN,
    
    apoderado_tipo_documento VARCHAR(50),
    apoderado_numero_documento VARCHAR(50),
    apoderado_primer_nombre VARCHAR(100),
    apoderado_segundo_nombre VARCHAR(100),
    apoderado_primer_apellido VARCHAR(100),
    apoderado_segundo_apellido VARCHAR(100),
    apoderado_correo VARCHAR(255),
    apoderado_telefono VARCHAR(50),
    
    numero_orden VARCHAR(100),
    monto_reclamado DECIMAL(10, 2),
    nombre_producto VARCHAR(255),
    
    tipo_reclamo VARCHAR(50),
    resumen VARCHAR(255),
    detalle_pedido TEXT,
    archivo_adjunto_url VARCHAR(500),
    pdf_reclamo_url TEXT,
    
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
