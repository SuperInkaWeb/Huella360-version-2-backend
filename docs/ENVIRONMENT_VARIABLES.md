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

| Variable | Descripción | Valor Ejemplo |
| :--- | :--- | :--- |
| `MP_ACCESS_TOKEN` | Access Token de Mercado Pago | `APP_USR-xxxxxx...` |
| `MP_CLIENT_ID` | Client ID de la aplicación | `123456789` |
| `MP_CLIENT_SECRET` | Client Secret de la aplicación | `xxxxxxxxxxxxxxxx` |
| `MP_SANDBOX` | Activar modo sandbox | `true` |
| `MP_SANDBOX_BUYER_EMAIL` | Email para pruebas en sandbox | `test_user_123@testuser.com` |

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
> (`PlaceholderResolutionException`), no solo el módulo de IA. Esto no es exclusivo de Groq: `AUTH0_ISSUER_URI`,
> `AUTH0_AUDIENCE`, `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET`, `RESEND_API_KEY`, `JWT_SECRET` y `APP_ENCRYPTION_SECRET`
> tienen el mismo patrón (`${VAR}` sin default) y romperían el arranque igual si faltaran — GROQ es solo el que más
> visiblemente lo hizo porque no estaba documentado en `.env.example` hasta ahora.
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
| `VITE_AUTH0_DOMAIN` | Dominio del tenant Auth0 (no bloquea V1) |
| `VITE_AUTH0_CLIENT_ID` | Client ID de Auth0 (no bloquea V1) |
| `VITE_AUTH0_AUDIENCE` | Audience de Auth0 (no bloquea V1) |
| `VITE_MP_PUBLIC_KEY` / `VITE_MP_CLIENT_ID` | Presentes en `.env.example` pero sin referencias en el código fuente actual (`grep` no encontró usos) — la Public Key de MercadoPago parece venir del backend por empresa, no de una variable de build. Pendiente de limpiar o confirmar uso real. |

## 📋 Estado por entorno (DEV/QA → PRODUCCIÓN)

Formato acordado: `VARIABLE | SERVICIO | DEV/QA | PRODUCCIÓN | OBLIGATORIA | FUNCIÓN`. Sin valores reales, solo estado.

| Variable | Servicio | DEV/QA | Producción | Obligatoria | Función |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `DB_NAME` / `DB_PORT` | PostgreSQL | Configurada (local) | Pendiente (Neon prod) | Sí | Conexión a base de datos |
| `JWT_SECRET` | Auth interno | Configurada DEV | Pendiente PROD (rotar) | Sí | Firma de JWT propios |
| `APP_ENCRYPTION_SECRET` | Auth interno | Configurada DEV | Pendiente PROD (rotar) | Sí | Cifrado de credenciales MP por empresa |
| `ALLOWED_ORIGINS` | CORS | Configurada (localhost:5173) | Pendiente (dominio real) | Sí | Origenes permitidos |
| `APP_PUBLIC_URL` / `APP_BACKEND_URL` / `APP_FRONTEND_URL` | Config interna | Configurada (localhost) | Pendiente (dominios reales) | Sí | URLs base / callbacks |
| `AUTH0_ISSUER_URI` / `AUTH0_AUDIENCE` | Auth0 | Placeholder, no usado | Pendiente | No para V1 (login social, diferido a V2) | OAuth2 social login |
| `MP_ACCESS_TOKEN` / `MP_CLIENT_ID` / `MP_CLIENT_SECRET` / `MP_WEBHOOK_SECRET` | Mercado Pago | Pendiente crear sandbox | Pendiente PROD | No para V1 (cobro real); sí para validar flujo en sandbox | Pagos/suscripciones |
| `MP_SANDBOX` | Mercado Pago | `true` (default del código) | `false` en PROD | Sí | Evita cobros reales en dev/QA |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | Cloudinary | Pendiente crear cuenta dev | Pendiente PROD | Sí | Imágenes (logos, productos, servicios) |
| `RESEND_API_KEY` / `RESEND_FROM` | Resend | Pendiente crear cuenta dev | Pendiente PROD | Sí | Emails (recuperación de contraseña, notificaciones) |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` | SMTP (MailHog en dev) | Configurada (MailHog local) | Pendiente (o vía Resend SMTP) | Sí | Envío de correo transaccional |
| `GROQ_API_KEY` / `GROQ_MODEL` | Groq | Pendiente crear key dev | Pendiente PROD | Actualmente sí (bloquea arranque, ver arriba) | Asistente de IA |
| `OPENAI_API_KEY` / `OPENAI_MODEL` | OpenAI | Vacía (opcional) | Vacía (opcional) | No | Fallback de IA |
| `API_PERU_TOKEN` | API Perú | Pendiente | Pendiente PROD | Sí (para validar RUC/DNI) | Verificación RUC/DNI |
| `ADMIN_EMAIL` | Notificaciones internas | Configurada (default) | Pendiente PROD | No | Destinatario de alertas admin |
| `COMMISSION_PERCENTAGE` / `DEFAULT_COUNTRY` / `DEFAULT_CURRENCY` / `DEFAULT_PLAN_NAME` | Reglas de negocio | Configurada (default) | Revisar antes de PROD | No | Config comercial |
| `WS_CLIENT_LOGIN` / `WS_CLIENT_PASSCODE` / `WS_SYSTEM_LOGIN` / `WS_SYSTEM_PASSCODE` | Broker STOMP interno | `guest` (default) | Rotar en PROD | No para V1 | Credenciales del broker WebSocket |
| `VITE_API_URL` / `VITE_WS_URL` | Frontend → Backend | Configurada (localhost) | Pendiente (dominios reales) | Sí | Frontend apunta al backend correcto |
| `VITE_SENTRY_DSN` | Sentry | Vacía (opcional) | Pendiente PROD | No | Monitoreo de errores frontend |
| `VITE_AUTH0_DOMAIN` / `VITE_AUTH0_CLIENT_ID` / `VITE_AUTH0_AUDIENCE` | Auth0 | Placeholder, no usado | Pendiente | No para V1 | Login social (V2) |

---

> [!IMPORTANT]
> Nunca compartas ni subas archivos que contengan valores reales de estas variables a repositorios públicos.
