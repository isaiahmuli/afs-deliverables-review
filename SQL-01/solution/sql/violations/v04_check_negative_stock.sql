-- CHECK: stock cannot go below zero.
-- expect-constraint: product_variant_stock_quantity_ck
-- EXPECTED: fails with psql exit code 3. Verbose errors print the constraint name.
\set VERBOSITY verbose
UPDATE product_variant SET stock_quantity = -1 WHERE variant_id = 1;
