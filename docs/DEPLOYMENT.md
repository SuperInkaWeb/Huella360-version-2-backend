# Vet-SaaS — Guía de Despliegue

## Requisitos previos

- Docker 20.10+ y Docker Compose v2
- Java 17+ (solo para desarrollo local)
- PostgreSQL 16+ (o usar Docker)
- Redis 7+ (o usar Docker)

## Variables de entorno

Copiar `.env.example` a `.env` y completar:

```bash
cp .env.example .env
```

### Variables obligatorias

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC de PostgreSQL (ej: `jdbc:postgresql://localhost:5432/vet_saas`) |
| `DB_USERNAME` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `JWT_SECRET` | Clave secreta para firmar JWT (mínimo 32 bytes) |
| `APP_ENCRYPTION_SECRET` | Clave para cifrar datos sensibles (32 bytes) |

### Variables de servicios externos

| Variable | Descripción |
|---|---|
| `MP_ACCESS_TOKEN` | Token de acceso de Mercado Pago |
| `MP_CLIENT_ID` | Client ID de Mercado Pago |
| `MP_CLIENT_SECRET` | Client Secret de Mercado Pago |
| `MP_WEBHOOK_SECRET` | Secreto para validar webhooks de Mercado Pago |
| `CLOUDINARY_CLOUD_NAME` | Nombre del cloud de Cloudinary |
| `CLOUDINARY_API_KEY` | API Key de Cloudinary |
| `CLOUDINARY_API_SECRET` | API Secret de Cloudinary |
| `AUTH0_ISSUER_URI` | URI del issuer de Auth0 |
| `AUTH0_AUDIENCE` | Audience de Auth0 |
| `RESEND_API_KEY` | API Key de Resend para emails |

## Despliegue con Docker Compose

```bash
# Levantar todos los servicios
docker compose up -d

# Ver logs
docker compose logs -f app

# Detener servicios
docker compose down
```

Los servicios incluidos:
- **app**: Backend Spring Boot (puerto 8080)
- **postgres**: PostgreSQL 16 (puerto 5432)
- **redis**: Redis 7 (puerto 6379)
- **mailhog**: Testing de emails (dev, puerto 8025)

## Despliegue en Producción

### 1. Preparar la imagen Docker

```bash
# Build de la imagen
docker build -t vet-saas-backend:latest .

# Tag para registry
docker tag vet-saas-backend:latest your-registry.com/vet-saas-backend:1.0
```

### 2. Variables de producción

```bash
# Seguridad
JWT_SECRET=<generar-clave-aleatoria-64-bytes>
APP_ENCRYPTION_SECRET=<generar-clave-aleatoria-32-bytes>
```

Generar claves seguras:
```bash
openssl rand -base64 64  # Para JWT_SECRET
openssl rand -base64 32  # Para APP_ENCRYPTION_SECRET
```

### 3. Endpoints de salud

- `GET /actuator/health` — Estado de la aplicación
- `GET /actuator/metrics` — Métricas de la aplicación
- `GET /swagger-ui.html` — Documentación Swagger (solo en dev/staging)

## Desarrollo local

```bash
# Levantar infraestructura
docker compose up -d postgres redis mailhog

# Ejecutar la app
./mvnw spring-boot:run

# Ejecutar tests
./mvnw test
```

## Migraciones de base de datos

Flyway ejecuta automáticamente las migraciones al iniciar la app. Las migraciones están en:
`src/main/resources/db/migration/`

Para migrar manualmente:
```bash
./mvnw flyway:migrate
```

## Monitoreo

- **Health check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics` (requiere autenticación)
- **Logs**: En producción, los logs se escriben a stdout (recomendado para contenedores)

## Resolución de problemas

### Error: "JWT secret key is X bytes"
La clave JWT es demasiado corta. Generar una nueva con `openssl rand -base64 64`.

### Error: "The verification key's size is not secure enough"
El Auth0 JWT decoder no puede validar tokens legacy. Esto es normal en test — el sistema intenta Auth0 primero y luego fallback a JWT interno.

### Rate limiting
El rate limiting es en memoria (no distribuido). Se reinicia con cada restart del contenedor. Para rate limiting distribuido, considerar usar Redis con Bucket4j distributed.

### CryptoUtil
Los tokens cifrados con el esquema ECB anterior no son compatibles con el nuevo esquema GCM. Los tokens se re-cifrarán automáticamente en el próximo acceso.
