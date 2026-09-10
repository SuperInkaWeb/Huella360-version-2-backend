-- Index para acelerar Dashboard Metrics
CREATE INDEX idx_orden_empresa_estado_fecha 
ON ordenes(empresa_id, estado, created_at);
