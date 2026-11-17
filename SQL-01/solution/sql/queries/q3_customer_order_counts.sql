-- Q3: Every customer with their order count; LEFT JOIN keeps customers with no orders.
-- Expected: expected/q3_customer_order_counts.csv
SELECT cu.full_name, cu.email, count(o.order_id) AS orders
FROM customer cu
LEFT JOIN customer_order o ON o.customer_id = cu.customer_id
GROUP BY cu.customer_id
ORDER BY cu.full_name;
