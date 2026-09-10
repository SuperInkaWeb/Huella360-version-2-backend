# Contexto Completo — Huella360 Backend

> Documento maestro del proyecto. Autocontenido. Cualquier agente o desarrollador debe entender el proyecto solo con leer este archivo.

---

## 1. Visión del Proyecto

**Huella360** es una plataforma SaaS integral para la gestión de veterinarias, adopciones, servicios y marketplace de productos para mascotas. El objetivo estratégico es convertirse en la **infraestructura tecnológica que conecte todo el ecosistema de salud animal** (dueños, veterinarias, laboratorios, municipalidades, refugios, farmacias, aseguradoras) potenciada con **Inteligencia Artificial y Big Data** para:

- Reducir el abandono animal
- Detectar enfermedades tempranamente
- Generar alertas epidemiológicas
- Predecir brotes de enfermedades por zona
- Crear un historial clínico digital único y compartido

### Cadena de Valor (Visión Futura)

```
Dueño → Huella360 (plataforma central) → Veterinaria → Laboratorio
                                                    → Municipalidad
                                                    → Refugios
                                                    → Farmacias
                                                    → Aseguradoras
                                                    → IA + Big Data
                                                    → Alertas epidemiológicas
                                                    → Predicción de enfermedades
```

### Problema que Resuelve

- Más del 60% de mascotas en Latinoamérica no poseen historial clínico digital unificado
- Cada veterinaria guarda sus propios registros de forma aislada
- Se repiten exámenes, se pierden vacunas, los diagnósticos llegan tarde
- Mayor propagación de enfermedades por falta de datos centralizados
- Abandono animal por seguimiento deficiente

---

## 2. Stack Tecnológico

| Componente | Tecnología | Versión |
|------------|-----------|---------|
| Lenguaje | Java | 17 |
| Framework | Spring Boot | 3.5.10 |
| Base de Datos | PostgreSQL | 16 |
| Migraciones | Flyway | (via Spring Boot) |
| Seguridad | Spring Security + JWT | jjwt 0.12.5 |
| OAuth2 | Auth0 | OAuth2 Resource Server |
| Caché/Buffer | Redis | 7 (Alpine) |
| WebSockets | STOMP over SockJS | (via Spring Boot) |
| Email | Resend | 3.1.0 |
| Email (alternativo) | SMTP (MailHog dev) | - |
| Almacenamiento | Cloudinary | cloudinary-http5 2.0.0 |
| Pagos | MercadoPago | sdk-java 2.1.7 |
| PDF | JasperReports | 7.0.6 |
| PDF (alternativo) | Apache PDFBox | 3.0.2 |
| API Docs | SpringDoc/Swagger | 2.6.0 |
| Rate Limiting | Bucket4j | 8.19.0 |
| Mappers | MapStruct | 1.5.5.Final |
| Utilities | Lombok | (via Spring Boot) |
| Testing | Testcontainers | 1.21.3 |
| IA | OpenAI API | gpt-4o-mini |
| Validación Peru | API Peru | DNI/RUC (configurado, no expuesto) |

---

## 3. Arquitectura

### 3.1 Estructura de Paquetes (package-by-module)

```
com.vet_saas/
├── VetSaasApplication.java
├── config/                          (10 clases de configuración)
│   ├── AppProperties.java           (central @ConfigurationProperties)
│   ├── ApplicationConfig.java       (UserDetailsService, PasswordEncoder)
│   ├── AsyncConfig.java             (2 thread pools: mail + webhook)
│   ├── CacheConfig.java             (Redis con TTLs por dominio)
│   ├── CloudinaryConfig.java
│   ├── FlywayConfig.java
│   ├── MyMercadoPagoConfig.java
│   ├── SwaggerConfig.java
│   ├── WebConfig.java
│   └── WebSocketConfig.java         (STOMP + JWT auth + Redis relay)
├── security/
│   ├── jwt/                         (JwtService, JwtFilter, Auth0JwtDecoder)
│   ├── config/                      (SecurityConfig)
│   └── filter/                      (JwtAuthenticationFilter)
├── core/
│   ├── exceptions/                  (BusinessException, ResourceNotFoundException, etc.)
│   ├── response/                    (ApiResponse, PagedResponse)
│   ├── service/                     (StorageService)
│   └── utils/                       (CryptoUtil, etc.)
└── modules/                         (20+ módulos de negocio)
```

### 3.2 Módulos Existentes (20+)

| # | Módulo | Paquetes | Estado |
|---|--------|----------|--------|
| 1 | **auth** | controller, service, repository, model, dto | ✅ Completo |
| 2 | **user** | controller, service, repository, model, dto | ✅ Completo |
| 3 | **client** | controller, service, repository, model, dto | ✅ Completo |
| 4 | **company** | controller, service, repository, model, dto + staff/ | ✅ Completo |
| 5 | **veterinarian** | controller, service, repository, model, dto | ✅ Completo |
| 6 | **pet** | controller, service, repository, model, dto | ✅ Completo |
| 7 | **medical_record** | controller, service, repository, model, dto | ✅ Completo |
| 8 | **catalog** | controller (4), service (3), repository (3), model (5), dto (9) | ✅ Completo |
| 9 | **sales** | controller, service, repository (2), model (3), dto (4), event | ✅ Completo |
| 10 | **payment** | controller, service (3), repository (2), model (2), gateway, dto | ✅ Completo |
| 11 | **appointment** | controller, service, repository, model, dto | ✅ Completo |
| 12 | **delivery** | controller (3), service (4), repository (5), model (7), dto, mapper, scheduler | ✅ Completo |
| 13 | **adoption** | controller, service, repository, model, dto | ✅ Completo |
| 14 | **notification** | service, listener (2) | ✅ Completo |
| 15 | **subscription** | controller, service (2), repository (2), model (3), dto (3) | ✅ Completo |
| 16 | **points** | controller (2), service (3), repository (5), model (5), dto (6) | ✅ Completo |
| 17 | **complaint** | controller, service (2), repository, model, dto | ✅ Completo |
| 18 | **teleconsulta** | controller, service, repository (2), model (4), dto (4) | ✅ Completo |
| 19 | **leads** | controller, service, repository, model, dto | ✅ Completo |
| 20 | **referral** | controller, service, repository, model, dto | ✅ Completo |
| 21 | **reminder** | controller, service, repository, model (2), dto | ✅ Completo |
| 22 | **dashboard** | controller, service, dto | ✅ Completo |
| 23 | **admin** | controller, service, dto | ✅ Completo |
| 24 | **ia** | controller, service, repository, model, dto | ⚠️ Básico (1 endpoint) |

---

## 4. Modelo de Dominio (Entidades JPA)

### 4.1 Autenticación y Usuarios

**Usuario** (`usuarios`)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK auto-generated |
| correo | String | CITEXT, unique |
| password | String | BCrypt hash |
| rol | Role enum | CLIENTE, EMPRESA, VETERINARIO, ADMIN, REPARTIDOR |
| estado | boolean | Activo/inactivo |
| emailVerificado | boolean | Verificación por email |
| auth0Sub | String | Unique, Auth0 subject |
| createdAt | Timestamp | |

**AuthToken** (`auth_tokens`) — Tokens JWT hasheados
**RefreshToken** (`refresh_tokens`) — Refresh tokens con revocación

### 4.2 Perfiles

**PerfilCliente** (`perfiles_clientes`) — 1:1 con Usuario
- nombres, apellidos, telefono, direccion, ciudad, pais, fotoPerfilUrl
- ubicacionLat, ubicacionLng (BigDecimal)

**Empresa** (`empresas`) — 1:1 con Usuario (EMPRESA)
- nombreComercial, razonSocial, ruc (unique), descripcion, tipoServicio
- telefonoContacto, emailContacto, direccion, ciudad, pais
- ubicacionLat, ubicacionLng
- logoUrl, bannerUrl
- mpAccessToken, mpPublicKey (enc con CryptoUtil)
- estadoValidacion (VerificationStatus)
- documentosUrl (JSONB Map)

**Veterinario** (`veterinarios`) — 1:1 con Usuario (VETERINARIO)
- nombres, apellidos, especialidad, numeroColegiatura (unique)
- biografia, aniosExperiencia, fotoPerfilUrl
- mpAccessToken, mpPublicKey (enc con CryptoUtil)
- estadoValidacion (VerificationStatus)

**StaffVeterinario** (`staff_veterinario`) — ManyToOne Empresa + ManyToOne Veterinario
- rolInterno, estado (StaffStatus)

**Repartidor** (`repartidores`) — 1:1 con Usuario (REPARTIDOR)
- nombres, apellidos, telefono, dni (unique), fotoPerfil
- tipoVehiculo (VehicleType), placaVehiculo
- estadoValidacion (VerificationStatus), estadoActual (RepartidorStatus)
- ubicacionLat, ubicacionLng, ultimaUbicacionAt
- calificacionPromedio, totalEntregas

### 4.3 Mascotas y Salud

**Mascota** (`mascotas`) — ManyToOne Usuario
- nombre, especie, raza, sexo (Sexo enum), fechaNacimiento
- pesoKg (BigDecimal), fotoUrl, esterilizado (boolean)
- observacionesMedicas, activo (boolean)

**HistoriaClinica** (`historias_clinicas`) — ManyToOne Mascota + ManyToOne Veterinario
- diagnostico, tratamiento, notas, pesoKg, fechaRegistro
- cita (ManyToOne, nullable)

### 4.4 Catálogo

**Categoria** (`categorias`) — Self-referencing (padre/subcategorias)
- nombre (unique), slug (unique), iconoUrl, activo, orden

**Producto** (`productos`) — ManyToOne Empresa + ManyToOne Categoria
- nombre, descripcion, precio (BigDecimal), precioOferta, stock, sku
- estado (EstadoProducto), ofertaInicio, ofertaFin
- activo, visible, version (optimistic locking)
- imagenes (List<String>, JSONB)

**Servicio** (`servicios`) — ManyToOne Empresa (nullable) + ManyToOne Veterinario (nullable)
- nombre, descripcion, precio, duracionMinutos (default 30)
- modalidad (ModalidadServicio: PRESENCIAL/VIRTUAL/DOMICILIO)
- activo, visible, imagenUrl, version

### 4.5 Ventas y Pagos

**Orden** (`ordenes`) — ManyToOne Usuario + ManyToOne Empresa
- codigoOrden (unique, "ORD-2026-0001")
- subtotal, costoEnvio, comisionPlataforma, total, descuento
- estado (EstadoOrden), metodoPago
- mpPreferenceId
- direccionEnvio (JSONB)
- guestEmail, guestNombre (guest checkout)

**DetalleOrden** (`detalles_orden`) — ManyToOne Orden
- producto (ManyToOne, nullable), servicio (ManyToOne, nullable)
- cantidad, precioUnitario, subtotal, metadata (JSONB)

**Pago** (`pagos`) — ManyToOne Orden
- mpPaymentId (unique), monto, metodoPago, estado

**WebhookEvent** (`webhook_events`) — Para retry de webhooks
- paymentId, status, attempts, maxAttempts (5), lastError, nextRetryAt

### 4.6 Citas

**Cita** (`citas`) — ManyToOne Cliente + ManyToOne Empresa + ManyToOne Servicio
- mascota (ManyToOne, nullable), veterinario (ManyToOne, nullable)
- fechaProgramada, horaInicio, horaFin
- estado (AppointmentStatus)
- orden (ManyToOne, nullable)
- notasCliente, notasInternas

### 4.7 Delivery

**Delivery** (`deliveries`) — OneToOne Orden
- repartidor (ManyToOne, nullable), zona (ManyToOne, nullable)
- origenLat/Lng/Direccion, destinoLat/Lng/Direccion/Referencia
- estado (DeliveryStatus)
- distanciaKm, tiempoEstimadoMin, costoDelivery
- timestamps: asignadoAt, enTiendaAt, recogidoAt, entregadoAt
- codigoConfirmacion (OTP hash), codigoExpiraAt
- fotoEntregaUrl
- calificaciones: cliente/Repartidor/Producto
- intentosAsignacion

**TrackingRepartidor** (`tracking_repartidor`) — GPS tracking
- delivery, repartidor, lat, lng, velocidadKmh, registradoAt

**ZonaCobertura** (`zonas_cobertura`) — Cobertura por empresa
- empresa, nombre, radioKm, costoEnvio, activo

### 4.8 Suscripciones

**Plan** (`planes`)
- nombre (unique), descripcion, precioMensual
- limiteMascotas, limiteProductos, limiteServicios, limiteRecordatorios, limiteIaUso
- tipo, activo

**Suscripcion** (`suscripciones`) — empresa/veterinario/usuario (OneToOne nullable)
- plan (ManyToOne), fechaInicio, fechaFin
- estado (EstadoSuscripcion)
- mpPreapprovalId, mpNextPaymentDate

### 4.9 Gamificación

**PuntosCliente** (`puntos_cliente`) — 1:1 PerfilCliente (MapsId)
- puntosTotales

**ConfiguracionPuntos** (`configuracion_puntos`)
- accion (unique), puntosOtorgados, activo, descripcion

**HistorialPuntos** (`historial_puntos`)
- puntosCliente, puntos, tipoAccion, referenciaId, descripcion, fecha

**Recompensa** (`recompensas`) — ManyToOne Empresa
- titulo, descripcion, costoPuntos, tipoDescuento, valorDescuento
- aplicaACiertosProductos, activo, productos (ManyToMany)

**CanjeRecompensa** (`canjes_recompensas`)
- puntosCliente, recompensa, fechaCanje, utilizado, fechaUtilizacion, orden

### 4.10 Otras Entidades

**Adopcion** (`adopciones`) — ManyToOne Mascota + ManyToOne Usuario
- titulo, historia, requisitos, estado (EstadoAdopcion)
- ubicacionCiudad, activo, fechaPublicacion

**SolicitudAdopcion** (`solicitudes_adopcion`)
- adopcion, interesado, mensajePresentacion, estado, motivoRechazo

**Consulta** (`consultas`) — Teleconsulta
- cliente, veterinario, mascota (nullable), estado (ConsultaEstado)
- jitsiRoomId

**ChatMensaje** (`chat_mensajes`)
- consulta, remitente, contenido (TEXT), tipo (MensajeTipo)

**Reclamo** (`reclamos`) — Libro de Reclamaciones
- usuario, 15+ campos personales/dirección
- montoReclamado, tipoReclamo, resumen, detallePedido
- archivoAdjuntoUrl, pdfReclamoUrl
- estado (EstadoReclamo), notasInternas

**IaUsage** (`ia_usage`) — Tracking de uso de IA
- usuario, mascotaId, tokensUsados, modelo, exitoso, fecha

**Lead** (`leads`) — Leads comerciales
- empresa, clienteNombre, clienteEmail, clienteTelefono
- servicioSolicitado, mensaje, estado (LeadEstado)

**Recordatorio** (`recordatorios`) — Recordatorios de mascotas
**Referido** (`referidos`) — Sistema de referidos

---

## 5. Endpoints REST (31 Controllers)

### Auth (`/api/v1/auth`)
| Método | Endpoint | Rol | Descripción |
|--------|----------|-----|-------------|
| POST | /register | Público | Registro con verificación email |
| POST | /login | Público | Login → JWT + refresh |
| POST | /refresh | Público | Renovar access token |
| POST | /logout | Auth | Revocar token |
| POST | /logout-all | Auth | Revocar todos los tokens |
| POST | /change-password | Auth | Cambiar contraseña |
| GET | /verify-email | Público | Verificar email por token |
| POST | /forgot-password | Público | Solicitar reset |
| POST | /reset-password | Público | Reset con token |
| POST | /sync | ADMIN | Sync con Auth0 |

### User (`/api/v1/users`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| GET | /me | Auth |
| GET | /exists/{correo} | Público |
| PATCH | /me/role | Auth |

### Client (`/api/v1/clients`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | /me | CLIENTE |
| GET | /me | CLIENTE |
| PUT | /me | CLIENTE (multipart) |
| PATCH | /me | CLIENTE |
| DELETE | /me | CLIENTE |
| GET | /empresa | EMPRESA |
| GET | /empresa/{id} | EMPRESA |
| GET | / | ADMIN |
| GET | /{id} | ADMIN |

### Company (`/api/v1/companies`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | / | EMPRESA (multipart) |
| PUT | / | EMPRESA (multipart) |
| GET | /me | EMPRESA |
| GET | /public | Público (paginated) |
| GET | /public/{id} | Público |
| GET | /me/patients | EMPRESA |
| PATCH | /mercadopago | EMPRESA |

### Staff (`/api/v1/companies/staff`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| GET | / | EMPRESA |
| POST | /invite | EMPRESA |
| DELETE | /{veterinarioId} | EMPRESA |
| GET | /invitations | EMPRESA |
| PUT | /invitations/{staffId}/accept | VETERINARIO |
| PUT | /invitations/{staffId}/reject | VETERINARIO |

### Veterinarian (`/api/v1/veterinarians`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | /profile | VETERINARIO |
| GET | /me | VETERINARIO |
| GET | / | Público (verified) |
| GET | /me/patients | VETERINARIO |
| PUT | /profile | VETERINARIO |

### Pet (`/api/v1/pets`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | / | CLIENTE (multipart) |
| GET | / | CLIENTE |
| GET | /{id} | CLIENTE |
| PUT | /{id} | CLIENTE (multipart) |
| DELETE | /{id} | CLIENTE |
| GET | /{id}/health-card | CLIENTE (PDF) |

### Medical Records (`/api/v1/medical-records`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | / | VETERINARIO |
| GET | /pet/{mascotaId} | VETERINARIO/EMPRESA |

### Catalog
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | /api/v1/products | EMPRESA (multipart) |
| GET | /api/v1/products/my-products | EMPRESA |
| PATCH | /api/v1/products/{id} | EMPRESA |
| DELETE | /api/v1/products/{id} | EMPRESA |
| GET | /api/v1/public/products | Público (search) |
| GET | /api/v1/public/products/{id} | Público |
| GET | /api/v1/public/products/company/{companyId} | Público |
| GET | /api/v1/services | Público (marketplace) |
| GET | /api/v1/services/{id} | Público |
| GET | /api/v1/services/me | VETERINARIO/EMPRESA |
| POST | /api/v1/services | VETERINARIO/EMPRESA |
| PATCH | /api/v1/services/{id} | VETERINARIO/EMPRESA |
| DELETE | /api/v1/services/{id} | VETERINARIO/EMPRESA |
| GET | /api/v1/categories | Público |
| GET | /api/v1/categories/{padreId}/subcategories | Público |
| POST | /api/v1/categories | ADMIN |
| PATCH | /api/v1/categories/{id} | ADMIN |
| DELETE | /api/v1/categories/{id} | ADMIN |

### Orders (`/api/v1/orders`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | / | CLIENTE |
| POST | /guest | Público |
| GET | /me | CLIENTE |

### Payments (`/api/v1/payments`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | /checkout/{orderId} | CLIENTE |
| POST | /checkout/guest/{orderId} | Público |
| POST | /webhook/{empresaId} | Público (MP) |
| POST | /webhook | Público (MP) |
| GET | /sync | CLIENTE |

### Appointments (`/api/v1/appointments`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | / | CLIENTE |
| GET | /empresa/{empresaId} | EMPRESA |
| GET | /veterinario/{veterinarioId} | VETERINARIO |
| GET | /me | CLIENTE |
| PATCH | /{citaId}/status | EMPRESA/VETERINARIO |

### Delivery (`/api/v1/deliveries`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| GET | /orden/{ordenId} | CLIENTE/EMPRESA |
| POST | /{id}/calificar | CLIENTE |
| POST | /{id}/cancelar | CLIENTE |
| PATCH | /{id}/estado | REPARTIDOR |
| POST | /{id}/confirmar-otp | REPARTIDOR |
| POST | /{id}/confirmar-foto | REPARTIDOR |
| POST | /{id}/intento-fallido | REPARTIDOR |
| POST | /{id}/incidencia | REPARTIDOR |
| POST | /{id}/reintentar | EMPRESA |
| POST | /orden/{ordenId}/reintentar | EMPRESA |
| GET | /disponibles | REPARTIDOR |
| POST | /{id}/aceptar | REPARTIDOR |
| GET | /{id} | Auth |
| GET | /empresa/ratings | EMPRESA |

### Repartidor (`/api/v1/repartidores`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | /me | REPARTIDOR (multipart) |
| PUT | /me | REPARTIDOR (multipart) |
| GET | /me | REPARTIDOR |
| PATCH | /me/disponibilidad | REPARTIDOR |
| PATCH | /me/ubicacion | REPARTIDOR |
| GET | /me/delivery-activo | REPARTIDOR |
| GET | /me/historial | REPARTIDOR |

### WebSocket
| Destino | Tipo | Descripción |
|---------|------|-------------|
| /app/tracking/{deliveryId}/ubicacion | MessageMapping | GPS del repartidor |
| /topic/delivery/{deliveryId}/ubicacion | Subscribe | Broadcast posición |

### Teleconsulta (`/api/v1/teleconsultas`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | / | CLIENTE |
| GET | / | Auth |
| GET | /{consultaId}/mensajes | Auth |
| POST | /{consultaId}/mensajes | Auth |
| PATCH | /{consultaId}/accept | VETERINARIO |
| PATCH | /{consultaId}/start | VETERINARIO |
| PATCH | /{consultaId}/finish | VETERINARIO |
| PATCH | /{consultaId}/cancel | Auth |

### Adoption (`/api/v1/adoptions`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | / | CLIENTE |
| GET | / | Público (paginated) |
| GET | /public/company/{companyId} | Público |
| GET | /{id} | Público |
| GET | /me | CLIENTE |
| GET | /applications/me | CLIENTE |
| POST | /{id}/apply | CLIENTE |
| GET | /{id}/applications | EMPRESA |
| PATCH | /applications/{solicitudId}/response | EMPRESA |

### Complaints (`/api/v1/reclamos`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | / | CLIENTE (multipart) |
| PATCH | /{id}/status | ADMIN |

### Subscriptions (`/api/v1/subscriptions`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| GET | /plans | Público |
| GET | /me | Auth |
| PATCH | /update-plan | Auth |
| GET | /usage/me | Auth |
| POST | /checkout/{planId} | Auth |
| PATCH | /cancel | Auth |

### Gamification
| Método | Endpoint | Rol |
|--------|----------|-----|
| GET | /api/v1/gamification/points/dashboard | CLIENTE |
| GET | /api/v1/gamification/points/config | ADMIN |
| PUT | /api/v1/gamification/points/config/{id} | ADMIN |
| POST | /api/v1/gamification/rewards/company | EMPRESA |
| GET | /api/v1/gamification/rewards/company | EMPRESA |
| PUT | /api/v1/gamification/rewards/company/{id} | EMPRESA |
| DELETE | /api/v1/gamification/rewards/company/{id} | EMPRESA |
| GET | /api/v1/gamification/rewards/business/{idEmpresa} | Público |
| POST | /api/v1/gamification/rewards/{idRecompensa}/redeem | CLIENTE |
| GET | /api/v1/gamification/rewards/my-redeemed | CLIENTE |
| GET | /api/v1/gamification/rewards/checkout/available/{idEmpresa} | CLIENTE |

### Dashboard (`/api/v1/dashboard`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| GET | / | EMPRESA |

### Leads (`/api/v1/leads`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | / | Público |
| GET | / | EMPRESA |
| GET | /count | EMPRESA |
| PATCH | /{leadId}/status | EMPRESA |

### Referrals (`/api/v1/referrals`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| GET | /code | CLIENTE |
| GET | /count | CLIENTE |
| POST | /apply | CLIENTE |

### Reminders (`/api/v1/recordatorios`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | / | CLIENTE |
| GET | / | CLIENTE |
| DELETE | /{recordatorioId} | CLIENTE |

### AI (`/api/v1/ia`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| POST | /health-alerts | CLIENTE/VETERINARIO |

### Admin (`/api/v1/admin`)
| Método | Endpoint | Rol |
|--------|----------|-----|
| GET | /stats | ADMIN |
| GET | /users | ADMIN |
| PATCH | /users/{id}/toggle-status | ADMIN |
| PATCH | /companies/{id}/toggle-status | ADMIN |
| GET | /companies | ADMIN |
| GET | /veterinarios | ADMIN |
| PATCH | /veterinarios/{id}/toggle-status | ADMIN |

---

## 6. Seguridad

### Autenticación Dual
1. **Custom JWT** (JJWT 0.12.5): Login tradicional con email/password
2. **Auth0 OAuth2**: Login social (Google, etc.)

### Filtros de Seguridad
- `JwtAuthenticationFilter`: Extrae y valida JWT del header Authorization
- `RateLimitFilter`: Bucket4j para rate limiting
- `AuditContextFilter`: Setea `app.user_id` en PostgreSQL session para auditoría

### Roles
| Rol | Descripción | Permisos Principales |
|-----|-------------|---------------------|
| CLIENTE | Dueño de mascota | Pets, orders, appointments, teleconsultas, referrals, reclamos |
| EMPRESA | Veterinaria/marketplace | Products, services, company, dashboard, staff, orders |
| VETERINARIO | Profesional veterinario | Profile, services, medical records, teleconsultas |
| ADMIN | Administrador global | Admin panel, categories, reclamo status |
| REPARTIDOR | Delivery | Delivery operations, tracking |

### Endpoints Públicos (sin auth)
- `/api/v1/auth/**`
- `/api/v1/public/**`
- `/api/v1/users/exists/**`
- `/api/v1/companies/public/**`
- `/api/v1/adoptions/public/**`
- `/api/v1/categories/**`
- `/api/v1/services/**`
- `/api/v1/subscriptions/plans`
- `/api/v1/payments/webhook/**`

---

## 7. Integraciones Externas

| Servicio | Config Key | Archivo | Uso |
|----------|------------|---------|-----|
| Cloudinary | `CLOUDINARY_*` | `StorageService.java` | Upload/delete imágenes (magic-byte validation, max 10MB) |
| MercadoPago | `MP_*` | `MercadoPagoGateway.java` | Preferences, pagos, OAuth exchange |
| Resend | `RESEND_API_KEY` | `EmailService.java` | Emails transaccionales |
| SMTP | `MAIL_*` | `EmailService.java` | Fallback/dev emails |
| OpenAI | `OPENAI_API_KEY` | `IaService.java` | Health alerts (gpt-4o-mini) |
| Auth0 | `AUTH0_*` | `Auth0JwtDecoder.java` | OAuth2 JWT validation |
| API Peru | `API_PERU_TOKEN` | `AppProperties` | DNI/RUC (configurado, no expuesto) |

---

## 8. Migraciones Flyway (34)

| # | Archivo | Propósito |
|---|---------|-----------|
| V1 | V1__init_schema.sql | Schema completo inicial |
| V2 | V2__implementar_staff_status_enum.sql | Staff status enum |
| V3 | V3__add_activo_column_to_adopciones.sql | Flag activo en adopciones |
| V4 | V4__add_catalog_module_improvements.sql | Mejoras catálogo |
| V5 | V5__add_pagos_and_mp_credentials.sql | Pagos + credenciales MP |
| V6 | V6__add_dashboard_metrics_index.sql | Índices dashboard |
| V7 | V7__create_subscription_tables.sql | Planes + suscripciones |
| V8 | V8__enable_veterinarian_subscriptions.sql | Suscripciones veterinarios |
| V9 | V9__create_appointments_table.sql | Citas |
| V10 | V10__create_medical_records_table.sql | Historias clínicas |
| V11 | V11__add_imagen_url_to_servicios.sql | Imagen en servicios |
| V12 | V12__add_veterinarian_payment_support.sql | MP tokens en veterinarios |
| V13 | V13__remove_tipo_servicio_check.sql | Remove check constraint |
| V14 | V14__delivery_system.sql | Sistema de delivery completo |
| V15 | V15__add_product_ratings_to_delivery.sql | Calificaciones delivery |
| V16 | V16__create_gamification_points_tables.sql | Gamificación |
| V17 | V17__add_discount_to_orders.sql | Descuentos en órdenes |
| V18 | V18__add_missing_enum_values.sql | Valores enum faltantes |
| V19 | V19__create_reclamos_table.sql | Libro de reclamaciones |
| V20 | V20__add_auth0_sub_to_usuarios.sql | Auth0 subject |
| V21 | V21__make_rol_nullable_in_usuarios.sql | Rol nullable (Auth0) |
| V22 | V22__hash_auth_tokens.sql | Hash de tokens |
| V23 | V23__create_webhook_events_table.sql | Webhook events retry |
| V24 | V24__add_usuario_id_to_reclamos.sql | Link reclamos a usuarios |
| V25 | V25__add_activo_to_perfiles_clientes.sql | Flag activo perfiles |
| V26 | V26__add_estado_to_reclamos.sql | Estado en reclamos |
| V27 | V27__seed_initial_categories.sql | Seed categorías iniciales |
| V28 | V28__guest_checkout.sql | Guest checkout |
| V29 | V29__add_plan_fields_and_seed_mvp_plans.sql | Campos planes + seed |
| V30 | V30__create_teleconsulta_tables.sql | Teleconsulta + chat |
| V31 | V31__create_recordatorios_table.sql | Recordatorios |
| V32 | V32__create_referidos_table.sql | Referidos |
| V33 | V33__create_leads_table.sql | Leads |
| V34 | V34__create_ia_usage_table.sql | Tracking uso IA |

---

## 9. Estado Actual — Lo que Funciona vs Lo que Falta

### ✅ COMPLETADO (Funcionando)

| Área | Estado | Evidencia |
|------|--------|-----------|
| Autenticación | ✅ | Register, login, JWT, refresh, logout, password reset, email verification, Auth0 sync |
| Gestión de Usuarios | ✅ | CRUD, role switching, profiles por rol |
| Perfiles de Empresa | ✅ | CRUD con multipart images, listing público, credenciales MP |
| Staff Management | ✅ | Invite/accept/reject flow |
| Perfiles Veterinario | ✅ | Create/update, verification status |
| Perfiles Cliente | ✅ | Self-service + empresa/admin views |
| Catálogo Productos | ✅ | CRUD con imágenes, categorías, search, soft delete, endpoints públicos |
| Catálogo Servicios | ✅ | CRUD con imágenes, marketplace search, dual ownership |
| Órdenes | ✅ | Create (auth + guest), line items, MercadoPago checkout |
| Pagos | ✅ | MercadoPago preferences, webhooks HMAC, async processing, retry queue |
| Citas | ✅ | CRUD con status management |
| Mascotas | ✅ | CRUD con photo upload, health card PDF |
| Historias Clínicas | ✅ | Vet crea, client/empresa lee con access control |
| Teleconsulta | ✅ | Chat con Jitsi room, message history, state machine |
| Delivery | ✅ | Full lifecycle: assignment, OTP, photo, GPS, ratings, incidents, retry |
| GPS Tracking | ✅ | WebSocket + Redis relay, Haversine proximity |
| Adopciones | ✅ | Publish, apply, approve/reject |
| Reclamos | ✅ | PDF (JasperReports), email, admin status |
| Suscripciones | ✅ | Plans (seeded), MercadoPago checkout, enforcement |
| Gamificación | ✅ | Points, rewards, redemption |
| Referidos | ✅ | Code generation, application |
| Recordatorios | ✅ | CRUD |
| Leads | ✅ | Public creation, empresa viewing |
| Dashboard | ✅ | Metrics aggregation |
| Admin Panel | ✅ | Global stats, user/company/vet management |
| Email Notifications | ✅ | Welcome, verify, reset, order, reclamo, OTP (async Resend) |
| Caching | ✅ | Redis con TTLs por dominio |
| Rate Limiting | ✅ | Bucket4j filter |
| API Docs | ✅ | SpringDoc/Swagger |
| Infrastructure | ✅ | Docker Compose (PG16, Redis7, MailHog), Dockerfile, CI/CD |

### ⚠️ INCOMPLETO / GAPS

| Área | Problema | Detalle |
|------|----------|---------|
| **IA Module** | Solo 1 endpoint | Solo `health-alerts`. No hay chatbot, análisis de imágenes, sugerencias de citas. Usa `RestTemplate` (blocking). |
| **API Peru** | Configurado no expuesto | `AppProperties.ApiPeruProps` configurado pero ningún controller lo usa para validación DNI/RUC |
| **Delivery Scheduler** | Existe, no wiring | `DeliveryScheduler.java` existe pero no es `@Scheduled` visible |
| **Redis GPS Buffer** | Comentado en pom.xml | El comment dice "Redis para buffer GPS" pero TrackingService escribe directo a PostgreSQL |
| **Tests** | Cobertura parcial | Tests para: JwtService, Auth0JwtDecoder, CryptoUtil, StorageService, GlobalExceptionHandler, UserController, CompanyController, OrderService, OrderController, ClienteService, PetService, PetController, AuthServiceSync. Faltan tests para mayoría de servicios. |
| **Webhook Retry** | Implementación básica | `WebhookEventService` guarda eventos pero no hay scheduled retry job visible |
| **Swagger Security** | Falta bearer token | No hay `OpenApiConfig` para JWT bearer authentication en Swagger UI |
| **ZonaCobertura** | Uso limitado | Entity existe pero `DeliveryService` no usa zone-based assignment |
| **Reclamo Workflow** | Sin SLA | Status transitions existen pero sin SLA tracking ni escalación automática |

### 🏗️ Calidad de Arquitectura

- **Organización:** Estructura limpia módulo-per-dominio con capas controller/service/repository/model/dto
- **Dual Auth:** Soporta Auth0 (OAuth2) y JWT custom, buena flexibilidad
- **Multi-tenant:** Cada empresa/vet tiene sus propias credenciales MP (enc con CryptoUtil)
- **Event-driven:** `OrderPaidEvent` + `OrderNotificationListener` + `DeliveryOrderPaidListener`
- **Async:** Thread pools separados para email y webhooks
- **PDF:** JasperReports para health cards y reclamos
- **Soft deletes:** Productos y servicios usan flags `activo`/`visible`
- **Optimistic locking:** `Producto.version`, `Servicio.version`, `Adopcion.version`

---

## 10. Infraestructura

### Docker Compose (dev)
- PostgreSQL 16 Alpine (puerto 5432)
- Redis 7 Alpine (puerto 6379)
- MailHog (puerto 1025 SMTP, 8025 UI)

### Dockerfile
- Multi-stage build (Maven build + JRE runtime)

### CI/CD
- `.github/` directory presente (configuración específica no documentada aquí)

### Variables de Entorno Principales
```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/huella360
DATABASE_USERNAME=...
DATABASE_PASSWORD=...

# JWT
JWT_SECRET=...
JWT_EXPIRATION=3600000
REFRESH_TOKEN_EXPIRATION=604800000

# Auth0
AUTH0_ISSUER=...
AUTH0_AUDIENCE=...

# Cloudinary
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...

# MercadoPago
MP_ACCESS_TOKEN=...
MP_PUBLIC_KEY=...

# Resend
RESEND_API_KEY=...

# OpenAI
OPENAI_API_KEY=...

# API Peru
API_PERU_TOKEN=...

# App
APP_PUBLIC_URL=...
APP_BACKEND_URL=...
APP_FRONTEND_URL=...
```

---

## 11. Decisiones Técnicas Clave

1. **package-by-feature** (no package-by-layer): Cada módulo tiene su propio paquete con controller/service/repository/model/dto
2. **Guest checkout permitido**: Órdenes sin cuenta de usuario obligatoria
3. **Credenciales MP por empresa**: Multi-tenant con encriptación (CryptoUtil)
4. **Auditoría a nivel BD**: Triggers PostgreSQL + AuditContextFilter
5. **Soft deletes**: Flags `activo`/`visible` en vez de DELETE físico
6. **Optimistic locking**: `@Version` en entidades con concurrencia
7. **Event-driven para pagos**: `OrderPaidEvent` para desacoplar notificaciones
8. **WebSocket con Redis relay**: En producción, Redis como STOMP broker
9. **Dual auth**: Auth0 para OAuth social, JWT custom para login tradicional
10. **PDF con JasperReports**: Templates para health cards y reclamos

---

## 12. Glossario

| Término | Definición |
|---------|-----------|
| EMPRESA | Veterinaria o negocio que vende productos/servicios |
| VETERINARIO | Profesional que puede trabajar independiente o en una EMPRESA |
| CLIENTE | Dueño de mascota que compra servicios/productos |
| REPARTIDOR | Persona que realiza entregas a domicilio |
| SaaS | Software as a Service — modelo de suscripción |
| MP | MercadoPago — pasarela de pagos |
| JWT | JSON Web Token — mecanismo de autenticación |
| STOMP | Simple Text Oriented Messaging Protocol — WebSockets |
| JSONB | Binary JSON en PostgreSQL — campos.flexibles |
| Flyway | Herramienta de migraciones de BD |
| MapStruct | Generador de mappers DTO↔Entity |
| Bucket4j | Rate limiting basado en token bucket |
