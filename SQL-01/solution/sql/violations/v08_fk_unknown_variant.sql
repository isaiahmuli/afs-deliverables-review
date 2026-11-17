-- FOREIGN KEY: an order item must point to an existing product option.
-- expect-constraint: order_item_variant_fk
-- EXPECTED: fails with psql exit code 3. Verbose errors print the constraint name.
\set VERBOSITY verbose
INSERT INTO order_item (order_id, variant_id, quantity, unit_price) VALUES (2, 9999, 1, 49.00);
