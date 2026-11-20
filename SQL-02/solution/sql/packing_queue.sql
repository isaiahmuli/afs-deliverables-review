-- Packing queue (unchanged SQL from slow_queries.sql / dashboard.py PACKING_QUEUE).
-- One SELECT only, so perf_check.py can wrap it in EXPLAIN.
SELECT order_id, customer_id, status, created_at
FROM orders
WHERE status = 'PENDING'
ORDER BY created_at, order_id
LIMIT 100;
