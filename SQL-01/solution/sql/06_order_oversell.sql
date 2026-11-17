-- 06_order_oversell.sql: Brian tries to order 1 x Wrap Dress M Olive (in stock) and
-- 5 x Denim Jacket L Navy (only 4 in stock). The jacket's stock update breaks
-- product_variant_stock_quantity_ck AFTER the order row, the first item, and the first stock
-- change were already written inside the transaction. The error aborts the transaction, and
-- ON_ERROR_STOP ends the script; PostgreSQL then rolls everything back.
-- EXPECTED: this script FAILS (psql exit code 3). 07_verify_after_orders.sql proves nothing remains.

\echo '--- Stock BEFORE'
SELECT pv.variant_id, p.name, s.code AS size, c.name AS colour, pv.stock_quantity
FROM product_variant pv JOIN product p USING (product_id) JOIN size s USING (size_id) JOIN colour c USING (colour_id)
WHERE (p.name, s.code, c.name) IN (('Wrap Dress','M','Olive'), ('Denim Jacket','L','Navy'))
ORDER BY pv.variant_id;

BEGIN;

INSERT INTO customer_order (customer_id, ordered_at)
SELECT customer_id, '2026-11-16 11:05:00+03' FROM customer WHERE email = 'brian@example.com'
RETURNING order_id \gset

-- Item 1 succeeds: 1 x Wrap Dress M Olive
INSERT INTO order_item (order_id, variant_id, quantity, unit_price)
SELECT :order_id, pv.variant_id, 1, p.current_price
FROM product_variant pv JOIN product p USING (product_id) JOIN size s USING (size_id) JOIN colour c USING (colour_id)
WHERE (p.name, s.code, c.name) = ('Wrap Dress', 'M', 'Olive');

UPDATE product_variant pv SET stock_quantity = pv.stock_quantity - 1
FROM product p, size s, colour c
WHERE p.product_id = pv.product_id AND s.size_id = pv.size_id AND c.colour_id = pv.colour_id
  AND (p.name, s.code, c.name) = ('Wrap Dress', 'M', 'Olive');

-- Item 2: 5 x Denim Jacket L Navy, only 4 in stock
INSERT INTO order_item (order_id, variant_id, quantity, unit_price)
SELECT :order_id, pv.variant_id, 5, p.current_price
FROM product_variant pv JOIN product p USING (product_id) JOIN size s USING (size_id) JOIN colour c USING (colour_id)
WHERE (p.name, s.code, c.name) = ('Denim Jacket', 'L', 'Navy');

\echo '--- Inside the transaction, part of the order is already written (visible only to this session):'
SELECT o.order_id, oi.variant_id, oi.quantity FROM customer_order o JOIN order_item oi USING (order_id)
WHERE o.order_id = :order_id ORDER BY oi.variant_id;

\echo '--- Now the oversell: 4 - 5 = -1 breaks product_variant_stock_quantity_ck'
\set VERBOSITY verbose
UPDATE product_variant pv SET stock_quantity = pv.stock_quantity - 5
FROM product p, size s, colour c
WHERE p.product_id = pv.product_id AND s.size_id = pv.size_id AND c.colour_id = pv.colour_id
  AND (p.name, s.code, c.name) = ('Denim Jacket', 'L', 'Navy');

-- Never reached: ON_ERROR_STOP stops the script at the error above.
COMMIT;
