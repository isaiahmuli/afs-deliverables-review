-- NOT NULL: a customer must have an email.
-- expect-constraint: customer_email_nn
-- expect-error: null value in column "email" of relation "customer" violates not-null constraint
-- EXPECTED: fails with psql exit code 3.
-- Note: PostgreSQL 18 stores NOT NULL as a named constraint, but its error message does not print
-- the name (other constraint kinds do). So this script first shows the name from the catalog.
\echo '--- The named NOT NULL constraint on customer.email (from the catalog)'
SELECT conname AS constraint_name, contype AS kind, a.attname AS column_name
FROM pg_constraint k JOIN pg_attribute a ON a.attrelid = k.conrelid AND a.attnum = k.conkey[1]
WHERE k.conrelid = 'customer'::regclass AND k.contype = 'n' AND a.attname = 'email';
\set VERBOSITY verbose
INSERT INTO customer (email, full_name) VALUES (NULL, 'No Email');
