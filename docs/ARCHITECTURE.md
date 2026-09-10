# Arquitectura del Sistema 🏗️

El backend de **Huella360** está diseñado bajo un enfoque de **Monolito Modular**, lo que permite una clara separación de responsabilidades por dominio de negocio, facilitando el mantenimiento y una posible transición a microservicios en el futuro.

## 🏗️ Patrón de Capas

Cada módulo dentro de `com.vet_saas.modules` sigue el patrón estándar de capas de Spring Boot:

1.  **Web/Controller**: Expone los endpoints REST y maneja las peticiones HTTP.
2.  **Service/Business**: Contiene la lógica de negocio y las reglas del sistema.
3.  **Persistence/Repository**: Interfaces que extienden `JpaRepository` para la comunicación con la base de datos PostgreSQL.
4.  **Domain/Entity**: Clases que representan las tablas de la base de datos.
5.  **DTO (Data Transfer Objects)**: Objetos para la transferencia de datos entre capas, utilizando **MapStruct** para las conversiones.

## 🧱 Estructura de Paquetes

- `com.vet_saas.config`: Configuraciones globales (CORS, Web, Cloudinary, Mercado Pago, WebSockets).
- `com.vet_saas.core`: Componentes transversales.
    - `exceptions`: Manejo global de excepciones (`GlobalExceptionHandler`).
    - `response`: Estructura estandarizada de respuestas API.
    - `utils`: Utilidades generales y criptografía.
- `com.vet_saas.modules`: El corazón funcional del sistema, dividido por subdominios.
- `com.vet_saas.security`: Implementación de seguridad basada en Spring Security, filtros JWT y proveedores de autenticación.

## 🔄 Flujo de Datos

1. El cliente (Frontend) realiza una petición autenticada mediante un **JWT** en la cabecera `Authorization`.
2. El `JwtAuthenticationFilter` valida el token y establece el contexto de seguridad.
3. El `Controller` recibe la petición, valida los datos de entrada (Bean Validation) y llama al `Service`.
4. El `Service` ejecuta la lógica, interactúa con uno o más `Repositories` y posiblemente con servicios externos (Cloudinary, Mercado Pago).
5. Se utilizan `Mappers` (MapStruct) para transformar las `Entities` en `DTOs` de respuesta.
6. Se devuelve una respuesta estandarizada mediante `ApiResponse`.

## 📡 Comunicación en Tiempo Real

Se utiliza **Spring WebSocket** para funcionalidades que requieren actualizaciones instantáneas, como:
- Notificaciones en tiempo real.
- Tracking de delivery.
- Actualización de estados de citas.

Se emplea **Redis** como buffer temporal para posiciones GPS y datos de alta frecuencia para evitar sobrecargar la base de datos relacional.

## 🗄️ Base de Datos y Migraciones

- **Motor:** PostgreSQL.
- **ORM:** Hibernate (vía Spring Data JPA).
- **Gestión de Versiones:** **Flyway**. Los scripts de migración se encuentran en `src/main/resources/db/migration`. Esto asegura que el esquema sea consistente en todos los entornos.

---
