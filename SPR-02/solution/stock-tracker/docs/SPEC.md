# Office stock tracker: spec

Written before coding. The code and tests are checked against this file.

## Goal

Keep reliable stock counts for office supplies in PostgreSQL, with a history that explains every
change, and never lose a change silently when two staff update the same supply at once. There is
no user interface or API: the operations are service methods, demonstrated by automated tests.

## Data and relationships

| Table | Columns | Notes |
|---|---|---|
| `product` | `id`, `name` (unique, case-insensitive), `quantity` (int, `>= 0`), `version` (bigint) | `version` is the optimistic lock |
| `category` | `id`, `name` (unique, case-insensitive) | |
| `product_category` | `product_id`, `category_id` (primary key on both) | join table: **many-to-many** product ↔ category |
| `stock_adjustment` | `id`, `product_id`, `quantity_change` (int, `<> 0`), `quantity_after` (int, `>= 0`), `reason` (not blank), `adjusted_at` (timestamptz) | **one-to-many** product → adjustments |

- A product can be in any number of categories, and a category holds any number of products.
- An adjustment is never edited or deleted; it is the product's history.
- Schema owned by Flyway; Hibernate only validates it (`ddl-auto=validate`).
  - `V1__catalog.sql`: product, category, product_category.
  - `V2__stock_history_and_version.sql`: stock_adjustment, and `product.version` added with
    default 0 so existing products are kept.

## Operations

| Operation | Input | Output | Failures |
|---|---|---|---|
| `CatalogService.addProduct` | name | product id (quantity 0) | blank or duplicate name |
| `CatalogService.addCategory` | name | category id | blank or duplicate name |
| `CatalogService.assignCategories` | product id, category names | — (replaces the product's categories) | `ProductNotFoundException`, `CategoryNotFoundException` |
| `CatalogService.search` | part of a name | matching products, by name (`[]` if none) | — |
| `CatalogService.productsInCategory` | category name | products in it, by name | `CategoryNotFoundException` |
| `StockService.receive` | product id, quantity > 0, reason | the adjustment | `ProductNotFoundException` |
| `StockService.use` | product id, quantity > 0, reason | the adjustment | `ProductNotFoundException`, `InsufficientStockException` |
| `StockService.applySlip` | several lines (product id, signed non-zero change, reason) | the adjustments | as above; **all or nothing** |
| `StockService.history` | product id | adjustments, oldest first | `ProductNotFoundException` |
| `StockReportService.report` | — | every product with quantity and category names, by name | — |

## Business rules

1. Stock is whole units and never negative.
2. Every adjustment adds or removes a non-zero quantity and has a reason.
3. Each change to a product's quantity writes exactly one history row, in the same transaction.
4. Search matches part of the name, ignoring case. The input is always a bound value, so `'`, `%`
   and `_` match themselves (`Painter's` finds Painter's tape; `%` finds nothing).
5. Two staff changing the same product at once: the first commit wins; the second is rejected
   with `ConcurrentStockChangeException` ("changed by someone else, try again"). Nothing is
   overwritten. The rejected change can be retried against the new count.
6. The stock report uses a fixed number of queries however many products there are.

## Failure behavior

- `InsufficientStockException` is a **checked** exception: callers must decide what to tell the
  person. Because Spring does not roll back on checked exceptions by default, every method that
  throws it declares `rollbackFor`.
- Any failure leaves stock and history exactly as they were (including earlier lines of a slip).
- Invalid input (zero or negative quantity, blank reason) is `IllegalArgumentException`.

## Acceptance checks

| # | Outcome | Check |
|---|---|---|
| A1 | Search | `pen` finds Ballpoint pens; `PAINTER'S` finds Painter's tape; `xyz` gives `[]` |
| A2 | Injection-safe search | `' OR '1'='1` and `%` give `[]`; a product named `100% recycled` is found by `%` only literally |
| A3 | Adjust | pens 10 → receive 20 → 30 → use 5 → 25; history has both rows with reasons, oldest first |
| A4 | Unknown product | `receive(999, ...)` → `ProductNotFoundException`, nothing written |
| A5 | No negative stock | tape 8, use 9 → `InsufficientStockException` with the reason; stock 8, history unchanged |
| A6 | Rollback after a write | slip "receive 20 pens, use 50 staples" → staples fail → pens still 10, no history rows |
| A7 | Pitfall | the same slip in a method with plain `@Transactional` (no `rollbackFor`) **keeps** the pens write; with the fix it doesn't |
| A8 | Concurrent change | two staff load pens (10) together; one commits +20, the other's −5 is rejected; stock 30, one history row; a retry gives 25 |
| A9 | Stale update | an update based on an old version is rejected; the newer value is preserved |
| A10 | Categories | notebook in Stationery and Meeting Supplies; Stationery lists pens, staples, notebooks |
| A11 | Report N+1 | naive report: 1 + N queries at 10, 50 and 200 products; fixed report: 1 query at every size |
| A12 | Migrations | all migrations apply to an empty PostgreSQL 18; V1 data survives V2 |
| A13 | Mappings | each relationship persists and reloads from the database |
