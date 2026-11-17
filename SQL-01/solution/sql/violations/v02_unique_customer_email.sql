-- UNIQUE: two customers cannot share an email.
-- expect-constraint: customer_email_uq
-- EXPECTED: fails with psql exit code 3. Verbose errors print the constraint name.
\set VERBOSITY verbose
INSERT INTO customer (email, full_name) VALUES ('amina@example.com', 'Another Amina');
