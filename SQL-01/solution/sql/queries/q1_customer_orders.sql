-- Q1: Amina's orders with product, size, and colour (oldest first). Expected: expected/q1_customer_orders.csv
SET TIME ZONE 'Africa/Nairobi';
SELECT o.order_id,
       to_char(o.ordered_at, 'YYYY-MM-DD HH24:MI') AS ordered_at,
       p.name        AS product,
       s.code        AS size,
       c.name        AS colour,
       oi.quantity,
       oi.unit_price AS price_paid
FROM customer cu
JOIN customer_order  o  ON o.customer_id = cu.customer_id
JOIN order_item      oi ON oi.order_id   = o.order_id
JOIN product_variant pv ON pv.variant_id = oi.variant_id
JOIN product         p  ON p.product_id  = pv.product_id
JOIN size            s  ON s.size_id     = pv.size_id
JOIN colour          c  ON c.colour_id   = pv.colour_id
WHERE cu.email = 'amina@example.com'
ORDER BY o.ordered_at, p.name, s.sort_order, c.name;
