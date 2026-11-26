-- Runs once, when the PostgreSQL container starts with an empty data directory.
-- Each service gets its own login and its own database, and cannot connect to the other's.

CREATE ROLE invoices_app LOGIN PASSWORD 'invoices_pw';
CREATE ROLE payments_app LOGIN PASSWORD 'payments_pw';

CREATE DATABASE invoices OWNER invoices_app;
CREATE DATABASE payments OWNER payments_app;

REVOKE CONNECT ON DATABASE invoices FROM PUBLIC;
REVOKE CONNECT ON DATABASE payments FROM PUBLIC;
