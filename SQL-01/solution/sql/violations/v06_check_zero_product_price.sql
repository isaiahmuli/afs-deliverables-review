-- CHECK: a product price must be greater than zero.
-- expect-constraint: product_current_price_ck
-- EXPECTED: fails with psql exit code 3. Verbose errors print the constraint name.
\set VERBOSITY verbose
INSERT INTO product (name, current_price) VALUES ('Free Scarf', 0.00);
