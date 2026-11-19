-- 02_seed.sql: small, deterministic data set. Run once, right after 01_schema.sql.
-- Timestamps are fixed (no now()) so query results are repeatable.

BEGIN;

INSERT INTO customer (email, full_name) VALUES
    ('amina@example.com', 'Amina Wanjiru'),
    ('brian@example.com', 'Brian Otieno'),
    ('chloe@example.com', 'Chloe Mwangi');      -- has no orders yet

-- current_price is today's price. Linen Shirt was 45.00 when order 1 was placed (see below).
INSERT INTO product (name, current_price) VALUES
    ('Linen Shirt',  49.00),
    ('Denim Jacket', 120.00),
    ('Wrap Dress',   75.50);

INSERT INTO size (code, sort_order) VALUES ('S', 1), ('M', 2), ('L', 3);

INSERT INTO colour (name) VALUES ('White'), ('Navy'), ('Olive');

INSERT INTO product_variant (product_id, size_id, colour_id, stock_quantity)
SELECT p.product_id, s.size_id, c.colour_id, v.stock
FROM (VALUES
        ('Linen Shirt',  'S', 'White', 10),
        ('Linen Shirt',  'M', 'White',  2),
        ('Linen Shirt',  'M', 'Navy',   6),
        ('Denim Jacket', 'M', 'Navy',   1),
        ('Denim Jacket', 'L', 'Navy',   4),
        ('Wrap Dress',   'S', 'Olive',  3),
        ('Wrap Dress',   'M', 'Olive',  8)
     ) AS v(product, size_code, colour, stock)
JOIN product p ON p.name = v.product
JOIN size    s ON s.code = v.size_code
JOIN colour  c ON c.name = v.colour
ORDER BY p.product_id, s.sort_order, c.colour_id;

-- Historical orders. Stock above is the stock AFTER these orders, so they do not change it here.
-- Each order and its items are inserted in one statement; the "has items" rule is checked at COMMIT.

-- Order 1: Amina, paid the OLD Linen Shirt price of 45.00.
WITH o AS (
    INSERT INTO customer_order (customer_id, ordered_at)
    SELECT customer_id, '2026-10-01 10:15:00+03' FROM customer WHERE email = 'amina@example.com'
    RETURNING order_id
)
INSERT INTO order_item (order_id, variant_id, quantity, unit_price)
SELECT o.order_id, pv.variant_id, i.qty, i.price
FROM o
CROSS JOIN (VALUES ('Linen Shirt', 'S', 'White', 2, 45.00),
                   ('Wrap Dress',  'S', 'Olive', 1, 75.50)) AS i(product, size_code, colour, qty, price)
JOIN product p          ON p.name = i.product
JOIN size s             ON s.code = i.size_code
JOIN colour c           ON c.name = i.colour
JOIN product_variant pv ON (pv.product_id, pv.size_id, pv.colour_id) = (p.product_id, s.size_id, c.colour_id);

-- Order 2: Brian.
WITH o AS (
    INSERT INTO customer_order (customer_id, ordered_at)
    SELECT customer_id, '2026-10-05 14:00:00+03' FROM customer WHERE email = 'brian@example.com'
    RETURNING order_id
)
INSERT INTO order_item (order_id, variant_id, quantity, unit_price)
SELECT o.order_id, pv.variant_id, 1, 120.00
FROM o
JOIN product p          ON p.name = 'Denim Jacket'
JOIN size s             ON s.code = 'M'
JOIN colour c           ON c.name = 'Navy'
JOIN product_variant pv ON (pv.product_id, pv.size_id, pv.colour_id) = (p.product_id, s.size_id, c.colour_id);

-- Order 3: Amina again, at the NEW Linen Shirt price of 49.00.
WITH o AS (
    INSERT INTO customer_order (customer_id, ordered_at)
    SELECT customer_id, '2026-11-02 09:30:00+03' FROM customer WHERE email = 'amina@example.com'
    RETURNING order_id
)
INSERT INTO order_item (order_id, variant_id, quantity, unit_price)
SELECT o.order_id, pv.variant_id, 1, 49.00
FROM o
JOIN product p          ON p.name = 'Linen Shirt'
JOIN size s             ON s.code = 'M'
JOIN colour c           ON c.name = 'Navy'
JOIN product_variant pv ON (pv.product_id, pv.size_id, pv.colour_id) = (p.product_id, s.size_id, c.colour_id);

COMMIT;

-- Row counts, for the reviewer.
SELECT 'customer' AS table_name, count(*) AS rows FROM customer
UNION ALL SELECT 'product', count(*) FROM product
UNION ALL SELECT 'size', count(*) FROM size
UNION ALL SELECT 'colour', count(*) FROM colour
UNION ALL SELECT 'product_variant', count(*) FROM product_variant
UNION ALL SELECT 'customer_order', count(*) FROM customer_order
UNION ALL SELECT 'order_item', count(*) FROM order_item;
