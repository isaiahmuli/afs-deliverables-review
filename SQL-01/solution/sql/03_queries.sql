-- 03_queries.sql: runs every read-only join query in order, for a readable transcript.
-- Each query is also compared on its own against a hand-written CSV in expected/.
\echo '--- Q1: Amina''s orders with product, size, and colour'
\ir queries/q1_customer_orders.sql
\echo '--- Q2: Product options low on stock (<= 3)'
\ir queries/q2_low_stock.sql
\echo '--- Q3: Customers and their order counts'
\ir queries/q3_customer_order_counts.sql
