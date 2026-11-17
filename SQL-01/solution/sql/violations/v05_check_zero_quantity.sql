-- CHECK: an order item quantity must be greater than zero.
-- expect-constraint: order_item_quantity_ck
-- EXPECTED: fails with psql exit code 3. Verbose errors print the constraint name.
\set VERBOSITY verbose
INSERT INTO order_item (order_id, variant_id, quantity, unit_price) VALUES (2, 1, 0, 49.00);
