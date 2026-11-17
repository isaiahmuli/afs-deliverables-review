-- FOREIGN KEY (ON DELETE RESTRICT): a customer with orders cannot be deleted.
-- expect-constraint: customer_order_customer_fk
-- EXPECTED: fails with psql exit code 3. Verbose errors print the constraint name.
\set VERBOSITY verbose
DELETE FROM customer WHERE email = 'brian@example.com';
