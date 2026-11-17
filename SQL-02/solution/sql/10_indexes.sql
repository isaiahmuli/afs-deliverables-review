-- Fixes for the two slow queries. Indexes only: no table, data, or query changes.

-- Order history: WHERE customer_id = ? ORDER BY created_at DESC, order_id DESC LIMIT 20.
-- Composite index: the equality column first, then the sort columns in the query's order.
-- PostgreSQL reads it backwards for DESC, so it returns the newest 20 rows already sorted
-- and stops: no Seq Scan over 995,110 orders and no Sort step.
CREATE INDEX orders_customer_created_idx
    ON orders (customer_id, created_at, order_id);

-- Packing queue: WHERE status = 'PENDING' ORDER BY created_at, order_id LIMIT 100.
-- Partial index: holds only PENDING orders (about 19k of 995k rows), already in
-- queue order, so the query reads the first 100 entries and stops.
CREATE INDEX orders_pending_created_idx
    ON orders (created_at, order_id)
    WHERE status = 'PENDING';

ANALYZE orders;

SELECT indexrelid::regclass AS index_name,
       pg_size_pretty(pg_relation_size(indexrelid)) AS size,
       pg_get_indexdef(indexrelid) AS definition
FROM pg_index
WHERE indrelid = 'orders'::regclass
ORDER BY 1;
