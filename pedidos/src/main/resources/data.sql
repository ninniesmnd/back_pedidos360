-- =============================================================================
-- 1. USUARIOS ASOCIADOS A LOCALES (Operadores de cocina, AdminLocal, etc.)
-- =============================================================================
INSERT INTO usuarios_local (id, email, local_id) VALUES
(1, 'prueba@caso360.onmicrosoft.com', 1),
(2, 'cocina.local2@tudominio.com', 2),
(3, 'admin.local1@tudominio.com', 1),
(4, 'admin.local2@tudominio.com', 2);

-- =============================================================================
-- 2. PEDIDOS DE PRUEBA
-- =============================================================================
-- ID 1: Para /api/pedidos/cocina (Local 1, RECIBIDO)
INSERT INTO pedidos (id, local_id, cliente_email, estado, tipo_despacho, fecha_creacion, fecha_actualizacion) 
VALUES (1, 1, 'prueba@caso360.onmicrosoft.com', 'RECIBIDO', 'DELIVERY', '2026-09-07 10:00:00', '2026-09-07 10:00:00');

-- ID 2: Para /api/pedidos/cocina (Local 1, EN_PREPARACION)
INSERT INTO pedidos (id, local_id, cliente_email, estado, tipo_despacho, fecha_creacion, fecha_actualizacion) 
VALUES (2, 1, 'prueba@caso360.onmicrosoft.com', 'EN_PREPARACION', 'RETIRO_EN_TIENDA', '2026-09-07 10:05:00', '2026-09-07 10:15:00');

-- ID 3: Para /api/pedidos/despacho (LISTO_PARA_DESPACHO + DELIVERY)
INSERT INTO pedidos (id, local_id, cliente_email, estado, tipo_despacho, fecha_creacion, fecha_actualizacion) 
VALUES (3, 1, 'prueba@caso360.onmicrosoft.com', 'LISTO_PARA_DESPACHO', 'DELIVERY', '2026-09-07 09:30:00', '2026-09-07 09:50:00');

-- ID 4: En reparto (DELIVERY)
INSERT INTO pedidos (id, local_id, cliente_email, estado, tipo_despacho, fecha_creacion, fecha_actualizacion) 
VALUES (4, 1, 'prueba@caso360.onmicrosoft.com', 'EN_REPARTO', 'DELIVERY', '2026-09-07 09:00:00', '2026-09-07 09:40:00');

-- ID 5: Entregado (Historial para /mis-pedidos y /admin)
INSERT INTO pedidos (id, local_id, cliente_email, estado, tipo_despacho, fecha_creacion, fecha_actualizacion) 
VALUES (5, 1, 'prueba@caso360.onmicrosoft.com', 'ENTREGADO', 'RETIRO_EN_TIENDA', '2026-09-06 18:00:00', '2026-09-06 18:45:00');

-- ID 6: Para probar aislamiento de Local 2 en cocina y admin
INSERT INTO pedidos (id, local_id, cliente_email, estado, tipo_despacho, fecha_creacion, fecha_actualizacion) 
VALUES (6, 2, 'prueba@caso360.onmicrosoft.com', 'RECIBIDO', 'DELIVERY', '2026-09-07 10:10:00', '2026-09-07 10:10:00');

-- =============================================================================
-- 3. ITEMS DE CADA PEDIDO
-- =============================================================================
-- Items para Pedido 1
INSERT INTO items_pedido (id, pedido_id, producto_id, nombre_producto, cantidad, precio_unitario) VALUES
(1, 1, 101, 'Hamburguesa Clásica Doble', 2, 8500.0),
(2, 1, 105, 'Papas Fritas Grandes', 1, 3200.0),
(3, 1, 201, 'Bebida 500ml Zero', 2, 1800.0);

-- Items para Pedido 2
INSERT INTO items_pedido (id, pedido_id, producto_id, nombre_producto, cantidad, precio_unitario) VALUES
(4, 2, 103, 'Pizza Familiar Pepperoni', 1, 12990.0),
(5, 2, 202, 'Cerveza Artesanal IPA', 2, 3500.0);

-- Items para Pedido 3
INSERT INTO items_pedido (id, pedido_id, producto_id, nombre_producto, cantidad, precio_unitario) VALUES
(6, 3, 104, 'Sándwich Mechada Queso', 1, 7800.0),
(7, 3, 201, 'Bebida 500ml Zero', 1, 1800.0);

-- Items para Pedido 4
INSERT INTO items_pedido (id, pedido_id, producto_id, nombre_producto, cantidad, precio_unitario) VALUES
(8, 4, 101, 'Hamburguesa Clásica Doble', 1, 8500.0),
(9, 4, 106, 'Aros de Cebolla', 1, 2900.0);

-- Items para Pedido 5
INSERT INTO items_pedido (id, pedido_id, producto_id, nombre_producto, cantidad, precio_unitario) VALUES
(10, 5, 102, 'Ensalada César Pollo', 1, 6500.0);

-- Items para Pedido 6
INSERT INTO items_pedido (id, pedido_id, producto_id, nombre_producto, cantidad, precio_unitario) VALUES
(11, 6, 103, 'Pizza Familiar Pepperoni', 2, 12990.0);

-- =============================================================================
-- 4. AJUSTE DE SECUENCIAS (PostgreSQL)
-- Evita errores 'duplicate key value violates unique constraint' en nuevos INSERT
-- =============================================================================
SELECT setval('usuarios_local_id_seq', (SELECT COALESCE(MAX(id), 1) FROM usuarios_local));
SELECT setval('pedidos_id_seq', (SELECT COALESCE(MAX(id), 1) FROM pedidos));
SELECT setval('items_pedido_id_seq', (SELECT COALESCE(MAX(id), 1) FROM items_pedido));