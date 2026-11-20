-- Order history
EXPLAIN (ANALYZE, BUFFERS)
SELECT order_id, customer_id, status, created_at
FROM orders
WHERE customer_id = 1
ORDER BY created_at DESC, order_id DESC
LIMIT 20;

-- Packing queue
EXPLAIN (ANALYZE, BUFFERS)
SELECT order_id, customer_id, status, created_at
FROM orders
WHERE status = 'PENDING'
ORDER BY created_at, order_id
LIMIT 100;
