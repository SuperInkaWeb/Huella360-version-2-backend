# Referencia de la API 🔌

La API de **Huella360** sigue los principios REST y utiliza JSON como formato principal para el intercambio de datos.

## 📍 Base URL
Todas las peticiones deben dirigirse a:
`{BACKEND_URL}/api/v1`

## 🔐 Autenticación
La mayoría de los endpoints requieren un token JWT en la cabecera:
`Authorization: Bearer <tu_token>`

### Endpoints de Autenticación (`/auth`)
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `POST` | `/auth/register` | Registro de nuevo usuario. |
| `POST` | `/auth/login` | Inicio de sesión para obtener el token. |
| `POST` | `/auth/refresh` | Refrescar el token de acceso. |
| `POST` | `/auth/forgot-password` | Solicitar recuperación de clave. |

## 🐾 Mascotas (`/pets`)
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/pets` | Listar mis mascotas. |
| `POST` | `/pets` | Registrar una nueva mascota. |
| `GET` | `/pets/{id}` | Obtener detalles de una mascota. |
| `PUT` | `/pets/{id}` | Actualizar datos de una mascota. |
| `DELETE` | `/pets/{id}` | Eliminar registro de mascota. |

## 🛒 Marketplace y Catálogo (`/products`, `/categories`)
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/categories` | Listar categorías de productos. |
| `GET` | `/public/products` | Catálogo público de productos. |
| `GET` | `/products/{id}` | Detalle de un producto específico. |
| `POST` | `/products` | Crear producto (Requiere rol ADMIN/VET). |

## 📅 Citas (`/appointments`)
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/appointments` | Listar citas del usuario. |
| `POST` | `/appointments` | Agendar una nueva cita. |
| `PATCH` | `/appointments/{id}/status` | Cambiar estado de la cita. |

## 🏢 Empresas (`/companies`)
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/companies/public` | Listar veterinarias registradas. |
| `GET` | `/companies/{id}` | Detalle de una veterinaria. |

## 📦 Estructura de Respuesta
La API devuelve las respuestas envueltas en un objeto estándar:

```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": { ... },
  "timestamp": "2026-05-02T12:00:00Z"
}
```

## ⚠️ Errores Comunes
- `401 Unauthorized`: Token ausente o inválido.
- `403 Forbidden`: No tienes los permisos necesarios (rol insuficiente).
- `404 Not Found`: El recurso solicitado no existe.
- `400 Bad Request`: Error de validación en los datos enviados.

---
