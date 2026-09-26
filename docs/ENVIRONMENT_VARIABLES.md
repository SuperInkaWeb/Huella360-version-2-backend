# Variables de Entorno 🔑

Este documento lista todas las variables de entorno necesarias para configurar y ejecutar el backend de **Huella360**.

## 🏗️ Configuración del Sistema

| Variable | Descripción | Valor Ejemplo |
| :--- | :--- | :--- |
| `DB_URL` | URL de conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/vet_saas` |
| `DB_USERNAME` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de la base de datos | `password` |
| `JWT_SECRET` | Clave secreta para firmar tokens JWT | `[Cadena de 64 caracteres]` |
| `APP_ENCRYPTION_SECRET` | Clave para cifrado de datos sensibles | `[Cadena secreta]` |
| `ALLOWED_ORIGINS` | Orígenes permitidos por CORS | `http://localhost:5173,https://huella360.com` |
| `APP_TIMEZONE` | Zona horaria del negocio. Opcional, default `America/Lima`. Se aplica a la JVM al arrancar: horas guardadas y mostradas, `LocalDateTime.now()` y jobs `@Scheduled` | `America/Lima` |

## 🚀 URLs de la Aplicación

| Variable | Descripción | Valor Ejemplo |
| :--- | :--- | :--- |
| `APP_PUBLIC_URL` | URL pública de la aplicación | `https://api.huella360.com` |
| `APP_BACKEND_URL` | URL base del backend | `https://api.huella360.com` |
| `APP_FRONTEND_URL` | URL base del frontend | `https://huella360.com` |

## 📧 Configuración de Correo (SMTP / Resend)

| Variable | Descripción | Valor Ejemplo |
| :--- | :--- | :--- |
| `MAIL_HOST` | Host SMTP | `smtp.resend.com` |
| `MAIL_PORT` | Puerto SMTP | `587` |
| `MAIL_USERNAME` | Usuario SMTP | `resend` |
| `MAIL_PASSWORD` | Contraseña SMTP (API Key) | `re_123456789` |
| `MAIL_FROM` | Dirección de envío | `hola@huella360.com` |
| `RESEND_API_KEY` | API Key para el SDK de Resend | `re_xxxxxxxxxxxx` |
| `RESEND_FROM` | Email verificado en Resend | `onboarding@resend.dev` |

## ☁️ Cloudinary (Multimedia)

| Variable | Descripción | Valor Ejemplo |
| :--- | :--- | :--- |
| `CLOUDINARY_CLOUD_NAME` | Nombre de la nube | `huella360-cloud` |
| `CLOUDINARY_API_KEY` | API Key de Cloudinary | `123456789012345` |
| `CLOUDINARY_API_SECRET` | API Secret de Cloudinary | `xxxxxxxxxxxxxxxxxxxxxxxxxxx` |

## 💳 Mercado Pago (Pagos)

Estas variables son a **nivel plataforma**: alimentan el SDK global de Mercado Pago (`MyMercadoPagoConfig`) y el flujo de **suscripciones** de Huella360 (`SubscriptionService`), donde el cobro lo recibe la propia plataforma, no una empresa individual.

| Variable | Descripción | Valor Ejemplo (sandbox) |
| :--- | :--- | :--- |
| `MP_ACCESS_TOKEN` | Access Token de la app de Mercado Pago. En modo prueba empieza con `TEST-` | `TEST-xxxxxxxx-xxxxxx-xxxxxxxxxxxxxxxx-xxxxxxxxx` |
| `MP_CLIENT_ID` | Client ID de la app OAuth — permite que cada Empresa conecte su propia cuenta MP desde su panel | `123456789` |
| `MP_CLIENT_SECRET` | Client Secret de la misma app OAuth | `xxxxxxxxxxxxxxxx` |
| `MP_SANDBOX` | Activa modo sandbox (`true`/`false`); actualmente `true` por defecto en `application.yaml` | `true` |
| `MP_SANDBOX_BUYER_EMAIL` | Cuenta de comprador de prueba para simular pagos en sandbox | `test_user_123@testuser.com` |
| `MP_WEBHOOK_SECRET` | Valida la firma de las notificaciones webhook entrantes de Mercado Pago | `xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` |

**Importante — credenciales por empresa (NO son variables de entorno):** cada Empresa que vende en el marketplace conecta su **propia** cuenta de Mercado Pago vía OAuth desde su panel (usando `MP_CLIENT_ID`/`MP_CLIENT_SECRET` de arriba para la negociación). El resultado (`access_token`, `public_key`) se guarda por empresa en la tabla `empresas` (`mpAccessToken` cifrado con `CryptoUtil`, `mpPublicKey` en claro), nunca en `.env`. No existe una variable `MP_PUBLIC_KEY` a nivel plataforma en el código actual: el checkout de suscripciones usa Checkout Pro (redirección con `init_point`), que no requiere Public Key en el frontend; el marketplace sí expone el `mpPublicKey` de cada empresa vía API para detectar si sus credenciales están en modo sandbox (prefijo `TEST-`).

## 🤖 IA (Groq / OpenAI)

| Variable | Descripción | Valor Ejemplo |
| :--- | :--- | :--- |
| `GROQ_API_KEY` | API Key de Groq, usada por el asistente de IA | `gsk_xxxxxxxxxxxxxxxxxxxx` |
| `GROQ_MODEL` | Modelo de Groq a usar | `llama-3.3-70b-versatile` |
| `OPENAI_API_KEY` | API Key de OpenAI (fallback/alternativa) | *(opcional)* |
| `OPENAI_MODEL` | Modelo de OpenAI a usar | `gpt-4o-mini` |

> [!WARNING]
> **`GROQ_API_KEY` no tiene valor por defecto en `application.yaml`** (`${GROQ_API_KEY}`, sin `:fallback`). Como todas las
> propiedades bajo el prefijo `app.*` (incluyendo `app.ia.groq-api-key`) se enlazan en un único bean `AppProperties` al
> arrancar el contexto de Spring, si la variable de entorno no existe **el arranque completo del backend falla**
> (`PlaceholderResolutionException`), no solo el módulo de IA. Esto no es exclusivo de Groq: `DB_URL`, `DB_USERNAME`,
> `DB_PASSWORD`, `JWT_SECRET`, `APP_ENCRYPTION_SECRET`, `AUTH0_ISSUER_URI`, `AUTH0_AUDIENCE`,
> `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET` y `RESEND_API_KEY` tienen el mismo patrón (`${VAR}` sin default) y
> romperían el arranque igual si faltaran — GROQ es solo el que más visiblemente lo hizo porque no estaba documentado
> en `.env.example` hasta ahora. La lista completa y su contraparte (las que **sí** tienen default y por tanto no
> impiden iniciar) está en [Estado por entorno](#-estado-por-entorno-devqa--producción).
>
> **Para desacoplar la IA de la disponibilidad del resto de la plataforma** (pedido explícito: que una caída/vencimiento
> de Groq no tumbe todo Huella360), el cambio de fondo sería darle un default vacío (`${GROQ_API_KEY:}`) y que
> `IaService`/`AppProperties` traten una key vacía como "IA deshabilitada" (fallback ya existe en `IaService` para
> respuestas nulas de OpenAI, pero no cubre la ausencia total de la key). No se ha aplicado este cambio todavía porque
> toca el arranque de la app entera; lo dejo propuesto para decidir si entra en esta semana o se documenta para V2.

## 🔍 Otros Servicios

| Variable | Descripción | Valor Ejemplo |
| :--- | :--- | :--- |
| `API_PERU_TOKEN` | Token para consulta RUC/DNI | `[Token de apiperu.dev]` |

## 🌐 Frontend (repo `Huella360-version-2-frontend`, prefijo `VITE_`)

| Variable | Descripción |
| :--- | :--- |
| `VITE_API_URL` | Base URL del backend (`/api/v1`) |
| `VITE_WS_URL` | Base URL del WebSocket (STOMP/SockJS) |
| `VITE_SENTRY_DSN` | DSN de Sentry (opcional) |
| `VITE_AUTH0_DOMAIN` | Dominio del tenant Auth0 — **es el único método de login del frontend**, ver nota abajo |
| `VITE_AUTH0_CLIENT_ID` | Client ID de Auth0 |
| `VITE_AUTH0_AUDIENCE` | Audience de Auth0 (debe coincidir con el Identifier de la API creada en el dashboard) |
| `VITE_MP_PUBLIC_KEY` / `VITE_MP_CLIENT_ID` | Presentes en `.env.example` pero sin referencias en el código fuente actual (`grep` no encontró usos) — la Public Key de MercadoPago parece venir del backend por empresa, no de una variable de build. Pendiente de limpiar o confirmar uso real. |

> [!IMPORTANT]
> **Actualización 2026-09-22**: se determinó que **el frontend no tiene ningún formulario propio de correo/contraseña**
> (`Login.tsx`/`Register.tsx` solo tienen el botón de Auth0) — el login por API que sí funciona en el backend está
> huérfano en la UI. Por eso Auth0 dejó de ser "no bloquea V1, diferido a V2": **es el único método de login que existe
> hoy en el producto**. Se creó y validó un tenant de desarrollo propio de Huella360 (`dev-axull8vzu88qqbmq.us.auth0.com`,
> reemplazando el tenant compartido `formex-payment.us.auth0.com` que usa producción). Flujo completo documentado en
> [`AUTH0_FLOW.md`](./AUTH0_FLOW.md).

## 📋 Estado por entorno (DEV/QA → PRODUCCIÓN)

Formato acordado: `VARIABLE | SERVICIO | DEV/QA | PRODUCCIÓN | OBLIGATORIA | FUNCIÓN`. Sin valores reales, solo estado.

### Qué significa "Obligatoria"

La columna distingue tres niveles, verificados uno por uno contra los placeholders `${...}` de
`src/main/resources/application.yaml`:

| Nivel | Significado | Cómo se reconoce en `application.yaml` |
| :--- | :--- | :--- |
| **Arranque** | Si falta, **el backend no inicia** (`PlaceholderResolutionException` al enlazar `AppProperties`) | `${VAR}` — sin `:` ni valor por defecto |
| **Funcional** | El backend **sí arranca**, pero la funcionalidad asociada no opera o queda en modo degradado hasta configurarla | `${VAR:}` o `${VAR:valor}` — tiene default |
| **Opcional** | Tiene default razonable y no requiere tocarse para V1 | `${VAR:valor}` |

**Las 12 variables de nivel "Arranque" son exactamente estas** (únicas sin default en el YAML):
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `APP_ENCRYPTION_SECRET`, `AUTH0_ISSUER_URI`,
`AUTH0_AUDIENCE`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`,
`RESEND_API_KEY`, `GROQ_API_KEY`. Cualquier otra variable de este documento tiene default y **no**
impide iniciar el backend.

### Tabla de estado

| Variable | Servicio | DEV/QA | Producción | Obligatoria | Función |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL | Configurada (local) | Pendiente (Neon prod) | **Arranque** | Conexión a base de datos |
| `DB_NAME` / `DB_PORT` | PostgreSQL | Configurada | Configurada | Opcional — solo las lee `docker-compose.yml` (con default `vet_saas` / `5432`), la app no las usa | Levantar el contenedor de Postgres local |
| `DB_POOL_MAX` / `DB_POOL_MIN` | HikariCP | Default (`3` / `0`) | Revisar antes de PROD | Opcional | Tamaño del pool de conexiones |
| `JWT_SECRET` | Auth interno | Configurada DEV | Pendiente PROD (rotar) | **Arranque** | Firma de JWT propios |
| `JWT_EXPIRATION` / `JWT_REFRESH_EXPIRATION` | Auth interno | Default (1 h / 7 días) | Revisar antes de PROD | Opcional | Vigencia de los tokens |
| `APP_ENCRYPTION_SECRET` | Auth interno | Configurada DEV | Pendiente PROD (rotar) | **Arranque** | Cifrado de credenciales MP por empresa |
| `ALLOWED_ORIGINS` | CORS | Default (`http://localhost:5173`) | Pendiente (dominio real) | Funcional — **imprescindible en PROD**: con el default, el navegador bloquea al frontend real por CORS | Orígenes permitidos |
| `APP_PUBLIC_URL` / `APP_BACKEND_URL` / `APP_FRONTEND_URL` | Config interna | Default (`localhost:8080` / `:5173`) | Pendiente (dominios reales) | Funcional — **imprescindible en PROD**: los callbacks y enlaces de correo apuntarían a `localhost` | URLs base / callbacks |
| `AUTH0_ISSUER_URI` / `AUTH0_AUDIENCE` | Auth0 | Configurada DEV (tenant propio `dev-axull8vzu88qqbmq`, validado de punta a punta) | Pendiente (sigue en `formex-payment`, migración pendiente de aprobar) | **Arranque** — y además es el único método de login del producto | Autenticación (login/registro) |
| `MP_ACCESS_TOKEN` / `MP_CLIENT_ID` / `MP_CLIENT_SECRET` / `MP_WEBHOOK_SECRET` / `MP_SANDBOX_BUYER_EMAIL` | Mercado Pago | Pendiente crear sandbox | Pendiente PROD | Funcional — sin ellas el backend arranca pero no se puede generar ninguna preferencia de pago | Pagos/suscripciones |
| `MP_SANDBOX` | Mercado Pago | Default `true` | **Poner `false` en PROD** | Funcional (default `true`) | Evita cobros reales en dev/QA |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | Cloudinary | Configurada (cuenta dev) | Pendiente PROD | **Arranque** | Imágenes (logos, productos, servicios) |
| `RESEND_API_KEY` | Resend | Configurada (cuenta dev) | Pendiente PROD | **Arranque** | Emails (recuperación de contraseña, notificaciones) |
| `RESEND_FROM` | Resend | Default (`onboarding@resend.dev`) | Pendiente (dominio verificado) | Funcional — con el default solo se puede enviar a la casilla dueña de la cuenta Resend | Remitente de los correos |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` | SMTP (MailHog en dev) | Default vacío | Pendiente (o vía Resend SMTP) | Funcional — vacías, el envío SMTP directo queda inactivo; el backend arranca igual | Envío de correo transaccional por SMTP |
| `MAIL_PORT` | SMTP | Default (`587`) | Revisar según proveedor | Opcional | Puerto SMTP |
| `GROQ_API_KEY` | Groq | Configurada (key dev) | Pendiente PROD | **Arranque** (ver aviso más arriba: es el caso que más visiblemente rompió el inicio) | Asistente de IA |
| `GROQ_MODEL` | Groq | Default (`llama-3.3-70b-versatile`) | Revisar antes de PROD | Opcional | Modelo de IA a usar |
| `OPENAI_API_KEY` / `OPENAI_MODEL` | OpenAI | Default vacío / `gpt-4o-mini` | Igual | Funcional (fallback opcional) | Fallback de IA |
| `API_PERU_TOKEN` | API Perú | Default vacío | Pendiente PROD | Funcional — sin ella la validación de RUC/DNI no responde, pero el backend arranca | Verificación RUC/DNI |
| `ADMIN_EMAIL` | Notificaciones internas | Default (`hola@huella360.com`) | Pendiente PROD | Opcional | Destinatario de alertas admin |
| `COMMISSION_PERCENTAGE` / `DEFAULT_COUNTRY` / `DEFAULT_CURRENCY` / `DEFAULT_PLAN_NAME` | Reglas de negocio | Default (`0.05` / `Peru` / `PEN` / `Basico`) | Revisar antes de PROD | Opcional | Config comercial |
| `WS_CLIENT_LOGIN` / `WS_CLIENT_PASSCODE` / `WS_SYSTEM_LOGIN` / `WS_SYSTEM_PASSCODE` | Broker STOMP interno | Default (`guest`) | Rotar en PROD | Opcional | Credenciales del broker WebSocket |

> [!NOTE]
> Las variables `VITE_*` viven en el repo del frontend y son **de build** (Vite las inyecta al compilar),
> así que la clasificación "Arranque/Funcional" del backend no les aplica: si faltan, el bundle compila
> igual pero queda apuntando a `undefined` en tiempo de ejecución.

| Variable (frontend) | Servicio | DEV/QA | Producción | Necesaria para | Función |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `VITE_API_URL` / `VITE_WS_URL` | Frontend → Backend | Configurada (localhost) | Pendiente (dominios reales) | Que el frontend encuentre al backend | Base URL de API y WebSocket |
| `VITE_AUTH0_DOMAIN` / `VITE_AUTH0_CLIENT_ID` / `VITE_AUTH0_AUDIENCE` | Auth0 | Configurada DEV (mismo tenant propio) | Pendiente (sigue en `formex-payment`) | Poder iniciar sesión (único método de login) | Autenticación (login/registro) |
| `VITE_SENTRY_DSN` | Sentry | Vacía (opcional) | Pendiente PROD | Nada — opcional | Monitoreo de errores frontend |

---

> [!IMPORTANT]
> Nunca compartas ni subas archivos que contengan valores reales de estas variables a repositorios públicos.
