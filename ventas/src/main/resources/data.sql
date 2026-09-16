-- =============================================================================
-- 1. CATALOGO DE PRODUCTOS
-- =============================================================================
INSERT INTO productos (id, local_id, nombre, descripcion, precio, stock, categoria, activo, fecha_actualizacion) VALUES
(101, 1, 'Hamburguesa Clásica Doble', 'Doble carne, queso cheddar y salsa de la casa', 8500.0, 40, 'Platos', true, '2026-09-07 08:00:00'),
(102, 1, 'Ensalada César Pollo', 'Lechuga, pollo grillado, crotones y aderezo césar', 6500.0, 25, 'Platos', true, '2026-09-07 08:00:00'),
(103, 1, 'Pizza Familiar Pepperoni', 'Masa artesanal, mozzarella y pepperoni', 12990.0, 15, 'Platos', true, '2026-09-07 08:00:00'),
(104, 1, 'Sándwich Mechada Queso', 'Mechada, queso derretido y pan amasado', 7800.0, 20, 'Platos', true, '2026-09-07 08:00:00'),
(105, 1, 'Papas Fritas Grandes', 'Porción grande de papas fritas', 3200.0, 60, 'Acompañamientos', true, '2026-09-07 08:00:00'),
(106, 1, 'Aros de Cebolla', 'Porción de aros de cebolla apanados', 2900.0, 45, 'Acompañamientos', true, '2026-09-07 08:00:00'),
(201, 1, 'Bebida 500ml Zero', 'Bebida sin azúcar 500ml', 1800.0, 100, 'Bebestibles', true, '2026-09-07 08:00:00'),
(202, 1, 'Cerveza Artesanal IPA', 'Cerveza artesanal estilo IPA 500ml', 3500.0, 30, 'Bebestibles', true, '2026-09-07 08:00:00'),
(301, 2, 'Pizza Familiar Pepperoni', 'Masa artesanal, mozzarella y pepperoni', 12990.0, 18, 'Platos', true, '2026-09-07 08:00:00'),
(302, 2, 'Bebida 500ml Zero', 'Bebida sin azúcar 500ml', 1800.0, 80, 'Bebestibles', true, '2026-09-07 08:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 2. VENTAS DE PRUEBA
-- =============================================================================
INSERT INTO ventas (id, local_id, pedido_id, vendedor_email, total, fecha_venta) VALUES
(1, 1, 5, 'admin.local1@tudominio.com', 6500.0, '2026-09-06 18:45:00'),
(2, 1, NULL, 'admin.local1@tudominio.com', 15400.0, '2026-09-07 09:10:00'),
(3, 2, NULL, 'admin.local2@tudominio.com', 21990.0, '2026-09-07 10:20:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 3. ITEMS DE CADA VENTA
-- =============================================================================
INSERT INTO items_venta (id, venta_id, producto_id, nombre_producto, cantidad, precio_unitario) VALUES
(1, 1, 102, 'Ensalada César Pollo', 1, 6500.0),
(2, 2, 105, 'Papas Fritas Grandes', 2, 3200.0),
(3, 2, 201, 'Bebida 500ml Zero', 5, 1800.0),
(4, 3, 302, 'Bebida 500ml Zero', 5, 1800.0),
(5, 3, 301, 'Pizza Familiar Pepperoni', 1, 12990.0)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 4. AJUSTE DE SECUENCIAS (PostgreSQL)
-- =============================================================================
SELECT setval('productos_id_seq', (SELECT COALESCE(MAX(id), 1) FROM productos));
SELECT setval('ventas_id_seq', (SELECT COALESCE(MAX(id), 1) FROM ventas));
SELECT setval('items_venta_id_seq', (SELECT COALESCE(MAX(id), 1) FROM items_venta));