BEGIN;
INSERT INTO orders (order_id, customer_id, status, created_at)
VALUES (1000001, 1, 'PAID', timestamp '2026-12-20 12:00:00');

INSERT INTO order_items (order_id, item_no, product_id, quantity, unit_price)
VALUES (1000001, 1, 1, 1, 25.00);
COMMIT;

SELECT order_id, customer_id, status, created_at
FROM orders WHERE order_id = 1000001;
