# Flujo de autenticación Auth0 en Huella360

Documento solicitado por Alexis (2026-09-22) antes de migrar del tenant actual
(`formex-payment.us.auth0.com`) a un tenant propio de Huella360. Cubre ambos
repos (`frontend` y `backend`). **Ningún valor real de credenciales está
incluido en este documento.**

## 1. Diagrama del flujo

```
┌─────────────┐  1. click "Iniciar Sesión"   ┌──────────────────────┐
│  Frontend   │ ───────────────────────────► │  Auth0 (tenant)      │
│ (React SPA) │   loginWithRedirect()         │  Universal Login     │
└─────────────┘                               │  (correo/contraseña) │
      ▲                                       └──────────┬───────────┘
      │ 2. redirect_uri = window.location.origin          │
      │    (Auth0 redirige de vuelta con el resultado)     │
      └─────────────────────────────────────────────────────┘
                              │
                              ▼
      3. Auth0Provider (App.tsx) obtiene sesión de Auth0
         useAuth0().isAuthenticated / user / getAccessTokenSilently()
                              │
                              ▼
      4. AuthContext.tsx:
         - Lee custom claims del "user" (empresaId, nombre) → localStorage
         - Pide access token (getAccessTokenSilently) y llama GET /users/me
         - Interceptor de axios (api.ts) agrega el token a TODAS las
           peticiones protegidas usando ese mismo getAccessTokenSilently()
                              │
                              ▼  Authorization: Bearer <access_token RS256>
                    ┌───────────────────────┐
                    │  Backend Spring Boot   │
                    └───────────┬────────────┘
                                │
      5. Auth0JwtDecoder.decode(token)
         - Lee el "alg" del header: RS256/384/512 → rama Auth0
         - Resuelve la clave pública por "kid" via JWKS:
           {AUTH0_ISSUER_URI}/.well-known/jwks.json (cacheado 1h)
         - Valida firma, expiración, issuer (== AUTH0_ISSUER_URI) y
           audience (== AUTH0_AUDIENCE, claim "aud")
                                │
                                ▼
      6. Auth0JwtAuthenticationConverter.convert(jwt)
         - Busca Usuario por email (claims "https://vet-saas.com/email",
           "https://huella360.com/email" o "email" estándar, en ese orden)
         - Si no lo encuentra por email, busca por auth0_sub (claim "sub",
           solo si empieza con "auth0|")
         - Si NO existe en ninguna BD: lo CREA automáticamente
           (password="[AUTH0]", rol=null, emailVerificado=true)
         - Rol: usa el de la BD (fuente de verdad); si es null, intenta
           extraerlo del JWT ("https://vet-saas.com/roles",
           "https://huella360.com/role" o "role") — si tampoco hay rol,
           autentica igual pero sin authorities, para que el frontend lo
           mande a /register/rol a elegir su tipo de cuenta
                                │
                                ▼
      7. SecurityConfig aplica @PreAuthorize/reglas por rol sobre el
         endpoint solicitado (hasRole/hasAnyRole/permitAll)
```

**Dato clave:** el backend NO valida usuario/contraseña — eso lo hace Auth0.
El backend solo valida que el **token** sea legítimo (firma, issuer,
audience) y luego busca/crea el `Usuario` correspondiente en su propia BD.

**Logout:** `AuthContext.logout()` limpia `localStorage` y llama
`auth0Logout({ logoutParams: { returnTo: window.location.origin } })` —
Auth0 cierra su propia sesión y redirige de vuelta al origin actual.

## 2. Dónde vive cada pieza (código)

| Pieza | Archivo |
|---|---|
| Config del SDK Auth0 (domain/clientId/audience/redirect_uri) | `frontend/src/App.tsx` (`<Auth0Provider>`) |
| Lógica de sesión, sync con backend, inyección de token | `frontend/src/features/auth/context/AuthContext.tsx` |
| Interceptor axios (adjunta el token a cada request protegido) | `frontend/src/shared/http/api.ts` (comentario: "Token injection is handled by AuthContext.tsx") |
| Rutas que NO llevan token (login/register/públicas) | `frontend/src/shared/http/publicEndpoints.ts` |
| Decodificador/validador de JWT (Auth0 + legacy) | `backend/src/main/java/com/vet_saas/security/jwt/Auth0JwtDecoder.java` |
| Mapeo JWT → Usuario + creación automática + rol | `backend/src/main/java/com/vet_saas/security/jwt/Auth0JwtAuthenticationConverter.java` |
| Reglas de autorización por ruta, CORS | `backend/src/main/java/com/vet_saas/security/config/SecurityConfig.java` |
| Binding de las env vars a objetos Java | `backend/src/main/java/com/vet_saas/config/AppProperties.java` (`app.auth0.issuer`, `app.auth0.audience`) |

## 3. Variables/configuración necesarias

| Variable | Repo | Dónde se usa | Valor actual |
|---|---|---|---|
| `VITE_AUTH0_DOMAIN` | frontend `.env` | `Auth0Provider domain=` | placeholder `tu-dominio.auth0.com` |
| `VITE_AUTH0_CLIENT_ID` | frontend `.env` | `Auth0Provider clientId=` | placeholder |
| `VITE_AUTH0_AUDIENCE` | frontend `.env` | `Auth0Provider authorizationParams.audience=` | `https://vet-saas.com` (nombre viejo del proyecto) |
| `AUTH0_ISSUER_URI` | backend `.env` | `Auth0JwtDecoder` (issuer + base de JWKS) | placeholder `https://your-tenant.auth0.com/` |
| `AUTH0_AUDIENCE` | backend `.env` | `Auth0JwtDecoder` (valida claim `aud`) | placeholder |
| `JWT_SECRET` | backend `.env` | Firma de tokens **legacy** (no Auth0), sigue siendo necesaria en paralelo | ya configurado (dev) |
| `ALLOWED_ORIGINS` | backend `.env` | CORS — debe incluir cada origin desde donde corre el frontend | `http://localhost:5173` |

**Configuración que se hace en el dashboard de Auth0 (no en código):**

| Config en Auth0 | Debe incluir |
|---|---|
| Allowed Callback URLs | `http://localhost:5173`, `https://huella360.com`, `https://www.huella360.com`, + URL de QA cuando exista |
| Allowed Logout URLs | mismos orígenes que arriba |
| Allowed Web Origins | mismos orígenes que arriba (necesario para `getAccessTokenSilently`) |
| API (audience) | crear una "API" en Auth0 con un Identifier = el valor que se pondrá en `AUTH0_AUDIENCE`/`VITE_AUTH0_AUDIENCE` (puede ser cualquier URI, no tiene que ser real, ej. `https://api.huella360.com`) |
| Post-Login Action | **crítico** — debe inyectar los custom claims que el backend espera: `https://vet-saas.com/email` (o el namespace que se decida usar) y `https://vet-saas.com/roles`. Sin esto, el login funciona pero el usuario siempre entra "sin rol" y tiene que elegirlo cada vez. |

⚠️ **Inconsistencia encontrada en el código actual**: el `Auth0JwtAuthenticationConverter`
acepta DOS namespaces distintos para los claims (`https://vet-saas.com/...` y
`https://huella360.com/...`), pero `AuthContext.tsx` en el frontend solo lee
`https://vet-saas.com/empresaId` y `https://vet-saas.com/nombre` — nunca los
de `huella360.com`. Al crear la Action del tenant nuevo, hay que elegir **un
solo namespace** (recomendado: `https://huella360.com/...`, ya que es el
dominio real) y limpiar el código para que use uno solo, no ambos.

## 4. Qué pasa con los usuarios que ya existen en `formex-payment`

Auth0 no migra usuarios entre tenants automáticamente — cada tenant tiene su
propia base de usuarios aislada. Dos caminos:

1. **Sin migración (recomendado si son pocos, ej. cuentas de prueba)**: cada
   persona vuelve a registrarse en el tenant nuevo con el mismo correo. El
   backend los reconoce igual porque `Auth0JwtAuthenticationConverter` busca
   primero por **email**, no por `auth0_sub` — en cuanto inicien sesión con
   el mismo correo bajo el tenant nuevo, van a recuperar su mismo `Usuario`,
   rol, empresa, etc. (el campo `auth0_sub` en la BD queda desactualizado,
   pero no se usa si el email matchea, así que no rompe nada funcionalmente).
2. **Migración real (Auth0 Management API, "User Import/Export")**: exporta
   los usuarios de `formex-payment` y los importa al tenant nuevo
   conservando su identidad. Requiere acceso de administrador a ambos
   tenants y trabajo adicional; solo vale la pena si hay usuarios reales (no
   de prueba) que no se pueden simplemente reinscribir.

**No se tocó ni se modificó nada de `formex-payment` para llegar a esta
conclusión** — es un análisis basado únicamente en cómo Auth0 funciona y en
cómo está escrito el código de conversión de JWT.

## 5. Próximos pasos (pendiente de que Alexis cree el tenant)

1. Alexis (o quien tenga acceso) crea el tenant nuevo en Auth0 — esto no lo
   puede hacer Claude directamente (requiere signup con verificación).
2. Crear una Application tipo "Single Page Application" dentro del tenant.
3. Crear una API con un Identifier (será el `AUDIENCE`).
4. Configurar Callback/Logout/Web Origins con las URLs de DEV/QA primero
   (no producción todavía).
5. Crear la Post-Login Action con los custom claims (ver sección 3).
6. Pasar Domain + Client ID + el Identifier de la API — con eso se
   actualizan las variables en el `.env` de DEV/QA (nunca en el repo).
7. Verificar el flujo completo end-to-end en DEV/QA (lista de casos que
   pidió Alexis) antes de tocar producción.
