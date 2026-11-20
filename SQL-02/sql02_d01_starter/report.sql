-- Wholesale finance report: a header with totals, then one row per order.
-- Start this file, then run report_arrival.sql in a second session within 30 seconds.

BEGIN ISOLATION LEVEL READ COMMITTED;

CREATE TEMP TABLE report_header ON COMMIT DROP AS
SELECT count(DISTINCT o.order_id) AS order_count,
       sum(i.quantity * i.unit_price) AS revenue
FROM orders o
JOIN order_items i ON i.order_id = o.order_id
WHERE o.customer_id = 1;

SELECT pg_sleep(30);

SELECT o.order_id, sum(i.quantity * i.unit_price) AS order_revenue,
       count(*) OVER () AS orders_in_rows,
       sum(sum(i.quantity * i.unit_price)) OVER () AS revenue_in_rows,
       h.order_count AS orders_in_header,
       h.revenue AS revenue_in_header
FROM orders o
JOIN order_items i ON i.order_id = o.order_id
CROSS JOIN report_header h
WHERE o.customer_id = 1
GROUP BY o.order_id, h.order_count, h.revenue
ORDER BY o.order_id;

COMMIT;
