-- 04_price_history.sql: an order keeps the price paid when the product's price changes.
SET TIME ZONE 'Africa/Nairobi';

\echo '--- Before: Denim Jacket current price, and what Brian paid in order 2'
SELECT p.name, p.current_price, oi.order_id, oi.unit_price AS price_paid
FROM product p
JOIN product_variant pv ON pv.product_id = p.product_id
JOIN order_item oi      ON oi.variant_id = pv.variant_id
WHERE p.name = 'Denim Jacket';

\echo '--- Price rise: Denim Jacket 120.00 -> 135.00'
UPDATE product SET current_price = 135.00 WHERE name = 'Denim Jacket';

\echo '--- After: current price changed, price paid did not'
SELECT p.name, p.current_price, oi.order_id, oi.unit_price AS price_paid
FROM product p
JOIN product_variant pv ON pv.product_id = p.product_id
JOIN order_item oi      ON oi.variant_id = pv.variant_id
WHERE p.name = 'Denim Jacket';
