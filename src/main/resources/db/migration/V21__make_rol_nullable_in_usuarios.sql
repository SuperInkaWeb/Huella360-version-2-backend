-- V21: Hacer nullable la columna 'rol' en usuarios para soportar flujo Auth0
-- Auth0 crea el usuario sin rol -> frontend redirige a selección de rol -> POST /auth/sync asigna el rol
ALTER TABLE usuarios ALTER COLUMN rol DROP NOT NULL;
