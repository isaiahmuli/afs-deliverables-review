-- CHECK: the price paid must be greater than zero.
-- expect-constraint: order_item_unit_price_ck
-- EXPECTED: fails with psql exit code 3. Verbose errors print the constraint name.
\set VERBOSITY verbose
INSERT INTO order_item (order_id, variant_id, quantity, unit_price) VALUES (2, 1, 1, -5.00);
