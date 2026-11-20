-- Removes the order added by report_arrival.sql so the report can run again.
BEGIN;
DELETE FROM order_items WHERE order_id = 1000001;
DELETE FROM orders WHERE order_id = 1000001;
COMMIT;
