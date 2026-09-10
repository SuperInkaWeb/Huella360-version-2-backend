# Guía de Funcionalidades 🚀

**Huella360** es una plataforma integral que abarca múltiples áreas de la gestión veterinaria y el mercado de mascotas. A continuación, se detallan las funcionalidades principales divididas por módulos.

## 🔐 Autenticación y Usuarios (`auth`, `user`)
- Registro de usuarios con diferentes roles (Cliente, Veterinario, Admin de Empresa, SuperAdmin).
- Login seguro con JWT.
- Recuperación de contraseña vía email.
- Gestión de perfiles de usuario y preferencias.

## 🏥 Gestión Veterinaria (`company`, `veterinarian`)
- Registro y configuración de sedes veterinarias.
- Gestión de horarios de atención y especialistas.
- Asignación de roles y permisos dentro de la empresa.
- Dashboard administrativo para dueños de veterinarias.

## 🐶 Gestión de Mascotas (`pet`, `medical_record`)
- Registro completo de mascotas (especie, raza, edad, peso, etc.).
- Historia clínica digitalizada.
- Registro de vacunas, desparasitaciones y cirugías.
- Subida de archivos y exámenes médicos (vía Cloudinary).

## 📅 Citas y Agenda (`appointment`)
- Reserva de citas online para servicios médicos o estéticos.
- Visualización de disponibilidad en tiempo real.
- Notificaciones de recordatorio de citas.
- Gestión de estados de cita (Pendiente, Confirmada, Cancelada, Completada).

## 🛒 Marketplace y Catálogo (`catalog`, `sales`)
- Catálogo de productos (alimentos, accesorios, medicamentos).
- Gestión de inventario y stock.
- Carrito de compras y proceso de Checkout.
- Historial de pedidos para clientes.

## 💳 Pagos y Suscripciones (`payment`, `subscription`)
- Pago de productos y servicios mediante Mercado Pago.
- Suscripciones SaaS para empresas (Planes Mensuales/Anuales).
- Gestión de facturación y recibos.
- Soporte para webhooks de notificación de pagos.

## 🏠 Adopciones (`adoption`)
- Publicación de mascotas para adopción.
- Gestión de solicitudes de adopción.
- Seguimiento de procesos de adopción aprobados.
- Portal público de búsqueda de mascotas.

## 🚚 Delivery y Logística (`delivery`)
- Seguimiento de pedidos en tiempo real.
- Gestión de rutas y estados de entrega.
- Notificaciones al cliente sobre el estado de su envío.

## 🏆 Fidelización (`points`)
- Sistema de acumulación de puntos por compras.
- Canje de puntos por descuentos o productos.
- Historial de movimientos de puntos.

## 🔔 Notificaciones (`notification`)
- Sistema de alertas internas.
- Envío de correos electrónicos transaccionales.
- Notificaciones Push/WebSockets para eventos críticos.

---
