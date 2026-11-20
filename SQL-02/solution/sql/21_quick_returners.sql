-- Planning query 2: customers who ordered again within 30 days.
-- Contract (starter README): customer_id, order_count, shortest_gap_days.
--   * a customer qualifies when one order's created_at is no more than 30 days after
--     another of their orders' created_at (all statuses);
--   * order_count counts ALL their orders;
--   * shortest_gap_days = smallest difference in calendar dates between two such orders;
--   * sorted by customer_id.
-- Uses: a SELF join (orders a JOIN orders b, same customer) and CTEs.
WITH close_pairs AS (
    SELECT a.customer_id,
           b.created_at::date - a.created_at::date AS gap_days
    FROM orders a
    JOIN orders b
      ON b.customer_id = a.customer_id
     AND b.order_id <> a.order_id
     AND b.created_at >= a.created_at
     AND b.created_at <= a.created_at + interval '30 days'
), returners AS (
    SELECT customer_id, min(gap_days) AS shortest_gap_days
    FROM close_pairs
    GROUP BY customer_id
), order_counts AS (
    SELECT customer_id, count(*) AS order_count
    FROM orders
    GROUP BY customer_id
)
SELECT r.customer_id, c.order_count, r.shortest_gap_days
FROM returners r
JOIN order_counts c ON c.customer_id = r.customer_id
ORDER BY r.customer_id
