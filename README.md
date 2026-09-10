# Huella360 - Marketplace Veterinaria Backend 🐾

Bienvenido al backend de **Huella360**, una plataforma SaaS integral para la gestión de veterinarias, adopciones, servicios y marketplace de productos para mascotas.

Este proyecto está construido con **Spring Boot 3.5** y sigue una arquitectura modular y escalable diseñada para soportar múltiples empresas (SaaS) y diversos perfiles de usuario.

## 🚀 Tecnologías Principales

- **Lenguaje:** Java 17
- **Framework:** Spring Boot 3.5.10
- **Base de Datos:** PostgreSQL
- **Migraciones:** Flyway
- **Seguridad:** Spring Security + JWT
- **Comunicación en Tiempo Real:** WebSockets
- **Caché/Buffer:** Redis
- **Integraciones:** 
  - Mercado Pago (Pagos)
  - Cloudinary (Almacenamiento de imágenes)
  - Resend (Email Marketing)
  - API Perú (Consulta RUC/DNI)

## 📂 Estructura del Proyecto

La aplicación utiliza un enfoque modular por dominio dentro del paquete `com.vet_saas.modules`:

- `auth`: Gestión de autenticación y tokens.
- `user`: Gestión de perfiles de usuario y roles.
- `company`: Configuración de veterinarias/empresas.
- `pet`: Gestión de mascotas y pacientes.
- `medical_record`: Historias clínicas y registros médicos.
- `catalog`: Catálogo de productos y servicios.
- `sales`: Procesamiento de ventas y facturación.
- `payment`: Integración con pasarelas de pago.
- `appointment`: Sistema de reservas y citas.
- `adoption`: Portal de adopciones y seguimiento.
- `delivery`: Gestión de estados de envío.
- `notification`: Sistema de alertas y avisos.
- `subscription`: Planes y suscripciones SaaS.
- `points`: Programa de lealtad y puntos.

## 🛠️ Configuración y Despliegue

### Requisitos Previos
- Java 17 o superior.
- Maven 3.8+.
- Instancia de PostgreSQL.
- Instancia de Redis (opcional para desarrollo local, recomendado para producción).

### Variables de Entorno
Crea un archivo `.env` o configura las variables en tu IDE basado en los campos requeridos en `src/main/resources/application.yaml`. Consulta la [Guía de Variables de Entorno](docs/ENVIRONMENT_VARIABLES.md) para más detalles.

### Ejecución Local
```bash
./mvnw spring-boot:run
```

## 📖 Documentación Detallada

Para profundizar en el sistema, consulta los siguientes documentos:

1. [Arquitectura del Sistema](docs/ARCHITECTURE.md) - Diseño técnico y flujo de datos.
2. [Guía de Funcionalidades](docs/FEATURES.md) - Detalle de cada módulo.
3. [Stack Tecnológico](docs/TECHNICAL_STACK.md) - Librerías y versiones.
4. [Referencia de la API](docs/API_REFERENCE.md) - Endpoints principales.
5. [Variables de Entorno](docs/ENVIRONMENT_VARIABLES.md) - Configuración necesaria.

---
© 2026 Huella360 - Todos los derechos reservados.
