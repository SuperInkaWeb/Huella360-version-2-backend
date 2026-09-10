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

## 🔍 Otros Servicios

| Variable | Descripción | Valor Ejemplo |
| :--- | :--- | :--- |
| `API_PERU_TOKEN` | Token para consulta RUC/DNI | `[Token de apiperu.dev]` |

---

> [!IMPORTANT]
> Nunca compartas ni subas archivos que contengan valores reales de estas variables a repositorios públicos.
