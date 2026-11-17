# Entity-relationship diagram: boutique order system

Renders on GitHub (Mermaid). Every table, its primary key (PK), foreign keys (FK), unique keys
(UK), and every relationship with its ON DELETE action. Generated from `sql/01_schema.sql` and
checked against the database catalog (`results/01_catalog_checks.out`).

```mermaid
erDiagram
    customer ||--o{ customer_order : "places (FK ON DELETE RESTRICT)"
    customer_order ||--|{ order_item : "contains 1+ (FK ON DELETE CASCADE)"
    product_variant ||--o{ order_item : "is sold as (FK ON DELETE RESTRICT)"
    product ||--o{ product_variant : "offered as (FK ON DELETE RESTRICT)"
    size ||--o{ product_variant : "sizes (FK ON DELETE RESTRICT)"
    colour ||--o{ product_variant : "colours (FK ON DELETE RESTRICT)"

    customer {
        integer customer_id PK "identity"
        text email UK "NOT NULL, lower case"
        text full_name "NOT NULL, not blank"
    }
    product {
        integer product_id PK "identity"
        text name UK "NOT NULL"
        numeric_19_2 current_price "NOT NULL, > 0"
    }
    size {
        integer size_id PK "identity"
        text code UK "NOT NULL, e.g. S M L"
        smallint sort_order UK "NOT NULL"
    }
    colour {
        integer colour_id PK "identity"
        text name UK "NOT NULL"
    }
    product_variant {
        integer variant_id PK "identity"
        integer product_id FK,UK "NOT NULL; UK = (product_id, size_id, colour_id)"
        integer size_id FK,UK "NOT NULL"
        integer colour_id FK,UK "NOT NULL"
        integer stock_quantity "NOT NULL, >= 0"
    }
    customer_order {
        integer order_id PK "identity"
        integer customer_id FK "NOT NULL"
        timestamptz ordered_at "NOT NULL"
    }
    order_item {
        integer order_id PK,FK "PK = (order_id, variant_id)"
        integer variant_id PK,FK "NOT NULL"
        integer quantity "NOT NULL, > 0"
        numeric_19_2 unit_price "NOT NULL, > 0, price PAID"
    }
```

## Relationships and ON DELETE choices

| Child → parent | Cardinality | Constraint | ON DELETE | Why |
|---|---|---|---|---|
| `customer_order` → `customer` | a customer places 0..n orders; an order has exactly 1 customer | `customer_order_customer_fk` | RESTRICT | Orders are business records; a customer with orders cannot be removed |
| `order_item` → `customer_order` | an order contains **1**..n items | `order_item_order_fk` | CASCADE | Items are part of their order; removing an order removes its lines |
| `order_item` → `product_variant` | an option appears in 0..n order items | `order_item_variant_fk` | RESTRICT | Sold options must remain for order history |
| `product_variant` → `product` | a product has 0..n options | `product_variant_product_fk` | RESTRICT | Catalogue rows in use cannot vanish |
| `product_variant` → `size` | a size is used by 0..n options | `product_variant_size_fk` | RESTRICT | Same |
| `product_variant` → `colour` | a colour is used by 0..n options | `product_variant_colour_fk` | RESTRICT | Same |

"An order has at least one item" (the `|{` end above) cannot be enforced by a foreign key. It is
enforced by the deferred constraint trigger `customer_order_has_items_ck`, which is checked at COMMIT.

## Why this is in third normal form

- **No repeating groups (1NF):** each order line is its own `order_item` row, and each
  size/colour option is its own `product_variant` row. There are no `item1`, `item2`, or
  comma-separated sizes.
- **No partial dependencies (2NF):** in `order_item`, with key (`order_id`, `variant_id`),
  `quantity` and `unit_price` depend on the whole key, meaning *this* option on *this* order.
- **No transitive dependencies (3NF):** product name and price live only in `product`; size codes
  only in `size`; colour names only in `colour`; customer details only in `customer`.
  `product_variant` holds only keys and `stock_quantity`, which depends on the whole option.
- **`order_item.unit_price` is not duplicated data.** It records the price *paid*, a fact about the
  order line. `product.current_price` is today's price. They differ once prices change
  (`sql/04_price_history.sql`).
