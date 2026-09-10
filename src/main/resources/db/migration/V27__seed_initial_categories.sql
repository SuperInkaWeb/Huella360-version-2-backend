-- V27: Seed initial categories for veterinary marketplace
-- Run this migration to populate the categorias table with default veterinary categories

-- Parent categories (ON CONFLICT ignored in case production already has these slugs)
INSERT INTO categorias (nombre, slug, activo, orden) VALUES
('Alimentos', 'alimentos', true, 1),
('Medicamentos', 'medicamentos', true, 2),
('Accesorios', 'accesorios', true, 3),
('Juguetes', 'juguetes', true, 4),
('Higiene y Belleza', 'higiene-y-belleza', true, 5),
('Servicios Veterinarios', 'servicios-veterinarios', true, 6),
('Servicios de Estética', 'servicios-de-estetica', true, 7),
('Mascotas y Adopciones', 'mascotas-y-adopciones', true, 8)
ON CONFLICT (slug) DO NOTHING;

-- Subcategories: Alimentos
INSERT INTO categorias (nombre, slug, padre_id, activo, orden) VALUES
('Perros', 'alimentos-perros', (SELECT id_categoria FROM categorias WHERE slug = 'alimentos'), true, 1),
('Gatos', 'alimentos-gatos', (SELECT id_categoria FROM categorias WHERE slug = 'alimentos'), true, 2),
('Aves', 'alimentos-aves', (SELECT id_categoria FROM categorias WHERE slug = 'alimentos'), true, 3),
('Peces', 'alimentos-peces', (SELECT id_categoria FROM categorias WHERE slug = 'alimentos'), true, 4),
('Exóticos', 'alimentos-exoticos', (SELECT id_categoria FROM categorias WHERE slug = 'alimentos'), true, 5)
ON CONFLICT (slug) DO NOTHING;

-- Subcategories: Medicamentos
INSERT INTO categorias (nombre, slug, padre_id, activo, orden) VALUES
('Antiparasitarios', 'antiparasitarios', (SELECT id_categoria FROM categorias WHERE slug = 'medicamentos'), true, 1),
('Antibióticos', 'antibioticos', (SELECT id_categoria FROM categorias WHERE slug = 'medicamentos'), true, 2),
('Vitaminas y Suplementos', 'vitaminas-suplementos', (SELECT id_categoria FROM categorias WHERE slug = 'medicamentos'), true, 3),
('Antiinflamatorios', 'antiinflamatorios', (SELECT id_categoria FROM categorias WHERE slug = 'medicamentos'), true, 4),
('Dermatología', 'dermatologia', (SELECT id_categoria FROM categorias WHERE slug = 'medicamentos'), true, 5),
('Control de Conducta', 'control-conducta', (SELECT id_categoria FROM categorias WHERE slug = 'medicamentos'), true, 6)
ON CONFLICT (slug) DO NOTHING;

-- Subcategories: Accesorios
INSERT INTO categorias (nombre, slug, padre_id, activo, orden) VALUES
('Correas y Arneses', 'correas-arneses', (SELECT id_categoria FROM categorias WHERE slug = 'accesorios'), true, 1),
('Camas y Descanso', 'camas-descanso', (SELECT id_categoria FROM categorias WHERE slug = 'accesorios'), true, 2),
('Transportadoras', 'transportadoras', (SELECT id_categoria FROM categorias WHERE slug = 'accesorios'), true, 3),
('Comederos y Bebederos', 'comederos-bebederos', (SELECT id_categoria FROM categorias WHERE slug = 'accesorios'), true, 4),
('Ropa para Mascotas', 'ropa-mascotas', (SELECT id_categoria FROM categorias WHERE slug = 'accesorios'), true, 5),
('Collares y Placas', 'collares-placas', (SELECT id_categoria FROM categorias WHERE slug = 'accesorios'), true, 6)
ON CONFLICT (slug) DO NOTHING;

-- Subcategories: Juguetes
INSERT INTO categorias (nombre, slug, padre_id, activo, orden) VALUES
('Juguetes para Perros', 'juguetes-perros', (SELECT id_categoria FROM categorias WHERE slug = 'juguetes'), true, 1),
('Juguetes para Gatos', 'juguetes-gatos', (SELECT id_categoria FROM categorias WHERE slug = 'juguetes'), true, 2),
('Juguetes Interactivos', 'juguetes-interactivos', (SELECT id_categoria FROM categorias WHERE slug = 'juguetes'), true, 3),
('Juguetes para Aves', 'juguetes-aves', (SELECT id_categoria FROM categorias WHERE slug = 'juguetes'), true, 4)
ON CONFLICT (slug) DO NOTHING;

-- Subcategories: Higiene y Belleza
INSERT INTO categorias (nombre, slug, padre_id, activo, orden) VALUES
('Shampús', 'shampus', (SELECT id_categoria FROM categorias WHERE slug = 'higiene-y-belleza'), true, 1),
('Acondicionadores', 'acondicionadores', (SELECT id_categoria FROM categorias WHERE slug = 'higiene-y-belleza'), true, 2),
('Cepillos y Peines', 'cepillos-peines', (SELECT id_categoria FROM categorias WHERE slug = 'higiene-y-belleza'), true, 3),
('Cortaúñas', 'cortauñas', (SELECT id_categoria FROM categorias WHERE slug = 'higiene-y-belleza'), true, 4),
('Higiene Dental', 'higiene-dental', (SELECT id_categoria FROM categorias WHERE slug = 'higiene-y-belleza'), true, 5),
('Perfumes y Colonias', 'perfumes-colonias', (SELECT id_categoria FROM categorias WHERE slug = 'higiene-y-belleza'), true, 6),
('Toallitas Húmedas', 'toallitas-humedas', (SELECT id_categoria FROM categorias WHERE slug = 'higiene-y-belleza'), true, 7)
ON CONFLICT (slug) DO NOTHING;

-- Subcategories: Servicios Veterinarios
INSERT INTO categorias (nombre, slug, padre_id, activo, orden) VALUES
('Consulta General', 'consulta-general', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-veterinarios'), true, 1),
('Vacunación', 'vacunacion', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-veterinarios'), true, 2),
('Cirugía', 'cirugia', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-veterinarios'), true, 3),
('Laboratorio y Análisis', 'laboratorio-analisis', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-veterinarios'), true, 4),
('Emergencias', 'emergencias', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-veterinarios'), true, 5),
('Desparasitación', 'desparasitacion', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-veterinarios'), true, 6),
('Odontología Veterinaria', 'odontologia-veterinaria', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-veterinarios'), true, 7),
('Imagenología', 'imagenologia', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-veterinarios'), true, 8)
ON CONFLICT (slug) DO NOTHING;

-- Subcategories: Servicios de Estética
INSERT INTO categorias (nombre, slug, padre_id, activo, orden) VALUES
('Peluquería Canina', 'peluqueria-canina', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-de-estetica'), true, 1),
('Peluquería Felina', 'peluqueria-felina', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-de-estetica'), true, 2),
('Baño y Secado', 'bano-secado', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-de-estetica'), true, 3),
('Spa y Relajación', 'spa-relajacion', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-de-estetica'), true, 4),
('Corte de Uñas', 'corte-de-unas', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-de-estetica'), true, 5),
('Limpieza de Oídos', 'limpieza-oidos', (SELECT id_categoria FROM categorias WHERE slug = 'servicios-de-estetica'), true, 6)
ON CONFLICT (slug) DO NOTHING;

-- Subcategories: Mascotas y Adopciones
INSERT INTO categorias (nombre, slug, padre_id, activo, orden) VALUES
('Perros en Adopción', 'perros-adopcion', (SELECT id_categoria FROM categorias WHERE slug = 'mascotas-y-adopciones'), true, 1),
('Gatos en Adopción', 'gatos-adopcion', (SELECT id_categoria FROM categorias WHERE slug = 'mascotas-y-adopciones'), true, 2),
('Aves en Adopción', 'aves-adopcion', (SELECT id_categoria FROM categorias WHERE slug = 'mascotas-y-adopciones'), true, 3),
('Otros Animales en Adopción', 'otros-adopcion', (SELECT id_categoria FROM categorias WHERE slug = 'mascotas-y-adopciones'), true, 4)
ON CONFLICT (slug) DO NOTHING;
