-- Final checks: seeded data unchanged (README row counts) and only indexes were added.
SELECT 'customers' AS relation, count(*) FROM customers
UNION ALL SELECT 'categories', count(*) FROM categories
UNION ALL SELECT 'products', count(*) FROM products
UNION ALL SELECT 'orders', count(*) FROM orders
UNION ALL SELECT 'order_items', count(*) FROM order_items;

SELECT count(*) AS arrival_orders_left FROM orders WHERE order_id = 1000001;

SELECT c.relname AS table_name, i.relname AS index_name, pg_get_indexdef(i.oid) AS definition
FROM pg_index x
JOIN pg_class c ON c.oid = x.indrelid
JOIN pg_class i ON i.oid = x.indexrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
ORDER BY 1, 2;
