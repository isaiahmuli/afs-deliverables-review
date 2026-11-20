

BEGIN;

CREATE TABLE customers (
    customer_id bigint PRIMARY KEY,
    name text,
    email text
);

CREATE TABLE categories (
    category_id integer PRIMARY KEY,
    name text
);

CREATE TABLE products (
    product_id integer PRIMARY KEY,
    category_id integer REFERENCES categories (category_id),
    name text,
    price numeric(19,2)
);

CREATE TABLE orders (
    order_id bigint PRIMARY KEY,
    customer_id bigint REFERENCES customers (customer_id),
    status text,
    created_at timestamp
);

CREATE TABLE order_items (
    order_id bigint REFERENCES orders (order_id),
    item_no integer,
    product_id integer REFERENCES products (product_id),
    quantity integer,
    unit_price numeric(19,2),
    PRIMARY KEY (order_id, item_no)
);

COMMIT;

SELECT name, installed_version
FROM pg_available_extensions
WHERE name = 'pg_stat_statements';
