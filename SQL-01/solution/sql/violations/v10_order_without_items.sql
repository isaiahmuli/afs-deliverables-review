-- CONSTRAINT TRIGGER (deferred): an order must contain at least one item; checked at COMMIT.
-- expect-constraint: customer_order_has_items_ck
-- EXPECTED: fails with psql exit code 3. Verbose errors print the constraint name.
\set VERBOSITY verbose
BEGIN;
INSERT INTO customer_order (customer_id, ordered_at) VALUES (3, '2026-11-16 12:00:00+03');
COMMIT;
