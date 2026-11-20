

BEGIN;

-- Bulk loading validates the same foreign keys after inserting the fixed rows.
-- These changes and the load are one transaction; failure restores the keys.
ALTER TABLE products DROP CONSTRAINT products_category_id_fkey;
ALTER TABLE orders DROP CONSTRAINT orders_customer_id_fkey;
ALTER TABLE order_items DROP CONSTRAINT order_items_order_id_fkey;
ALTER TABLE order_items DROP CONSTRAINT order_items_product_id_fkey;

INSERT INTO customers (customer_id, name, email)
SELECT n, CASE WHEN n = 1 THEN 'Wholesale customer' ELSE 'Customer ' || n END,
       'customer' || n || '@example.com'
FROM generate_series(1, 100000) AS g(n);

INSERT INTO categories (category_id, name)
SELECT n, 'Category ' || n
FROM generate_series(1, 20) AS g(n);

INSERT INTO products (product_id, category_id, name, price)
SELECT n, 1 + n % 20, 'Product ' || n,
       (1000 + (n % 90) * 100 + n % 100)::numeric / 100
FROM generate_series(1, 2000) AS g(n);

-- Each customer gets a fixed number of orders: one-time, occasional, regular, or heavy.
-- Customer 1 is the wholesale account. Dates are scattered over one year.
INSERT INTO orders (order_id, customer_id, status, created_at)
WITH plan AS (
    SELECT c,
           CASE WHEN c = 1 THEN 2000
                WHEN (c * 7919) % 100 < 40 THEN 1
                WHEN (c * 7919) % 100 < 70 THEN 2 + (c * 31) % 4
                WHEN (c * 7919) % 100 < 95 THEN 6 + (c * 17) % 25
                ELSE 40 + (c * 13) % 81 END AS order_count
    FROM generate_series(1::bigint, 100000) AS g(c)
), placed AS (
    SELECT p.c, j,
           CASE WHEN p.c = 1 THEN j * 262
                ELSE ((p.c * 104729 + j * 7907 + j * j * 13) % 365) * 1440
                     + (p.c * 7 + j * 331) % 1440 END AS minute_offset
    FROM plan p
    CROSS JOIN LATERAL generate_series(1::bigint, p.order_count) AS s(j)
), dated AS (
    SELECT c, j, minute_offset / 1440 AS day,
           timestamp '2025-11-01 00:00:00'
               + minute_offset * interval '1 minute' + j * interval '1 second' AS created_at
    FROM placed
)
SELECT row_number() OVER (ORDER BY created_at, c, j),
       c,
       CASE WHEN (c * 31 + j * 17) % 20000 = 0 AND day < 344 THEN 'PENDING'
            WHEN day >= 358 THEN 'PENDING'
            WHEN day >= 344 THEN 'PAID'
            WHEN (c * 7 + j * 13) % 50 = 0 THEN 'CANCELLED'
            ELSE 'SHIPPED' END,
       created_at
FROM dated;

-- One to four lines per order. A few bestsellers take a large share of lines;
-- products 1901-2000 never sell. Some lines were sold at a 20% discount.
INSERT INTO order_items (order_id, item_no, product_id, quantity, unit_price)
WITH lines AS (
    SELECT o.order_id, o.customer_id, l AS item_no,
           CASE WHEN (o.order_id * 13 + l * 7) % 7 < 3
                THEN 1 + ((o.order_id * 37 + l * 101) % 1900) % 100
                ELSE 1 + (o.order_id * 37 + l * 101) % 1900 END AS product_id
    FROM orders o
    CROSS JOIN LATERAL generate_series(1::bigint, 1 + o.order_id % 4) AS s(l)
), priced AS (
    SELECT order_id, customer_id, item_no, product_id,
           1000 + (product_id % 90) * 100 + product_id % 100 AS list_cents
    FROM lines
)
SELECT order_id, item_no, product_id,
       CASE WHEN customer_id = 1 THEN 5 + (order_id + item_no) % 20
            ELSE 1 + (order_id + item_no) % 3 END,
       CASE WHEN (order_id + item_no * 3) % 10 = 0 THEN list_cents * 4 / 5
            ELSE list_cents END::numeric / 100
FROM priced;

ALTER TABLE products ADD CONSTRAINT products_category_id_fkey
    FOREIGN KEY (category_id) REFERENCES categories (category_id);
ALTER TABLE orders ADD CONSTRAINT orders_customer_id_fkey
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id);
ALTER TABLE order_items ADD CONSTRAINT order_items_order_id_fkey
    FOREIGN KEY (order_id) REFERENCES orders (order_id);
ALTER TABLE order_items ADD CONSTRAINT order_items_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products (product_id);

COMMIT;
ANALYZE customers, categories, products, orders, order_items;

SELECT 'customers' AS relation, count(*) FROM customers
UNION ALL SELECT 'categories', count(*) FROM categories
UNION ALL SELECT 'products', count(*) FROM products
UNION ALL SELECT 'orders', count(*) FROM orders
UNION ALL SELECT 'order_items', count(*) FROM order_items;
