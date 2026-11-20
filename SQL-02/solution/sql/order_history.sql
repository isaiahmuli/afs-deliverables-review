-- Order history (unchanged SQL from slow_queries.sql / dashboard.py ORDER_HISTORY).
-- One SELECT only, so perf_check.py can wrap it in EXPLAIN.
SELECT order_id, customer_id, status, created_at
FROM orders
WHERE customer_id = 1
ORDER BY created_at DESC, order_id DESC
LIMIT 20;
