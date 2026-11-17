-- CHECK: emails are stored in lower case, so a case-variant duplicate cannot sneak past the UNIQUE rule.
-- expect-constraint: customer_email_lower_ck
-- EXPECTED: fails with psql exit code 3. Verbose errors print the constraint name.
\set VERBOSITY verbose
INSERT INTO customer (email, full_name) VALUES ('Amina@Example.com', 'Amina Again');
