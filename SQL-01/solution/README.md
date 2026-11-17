# Boutique order database (SQL-01)

PostgreSQL 18 schema for a clothing boutique: customers, products in sizes and colours, orders,
and stock. It includes seed data, join queries with expected rows, constraint violations, and
transaction evidence.

## Run everything

Requires Docker. PostgreSQL 18 and **psql 18** run inside the container, because a psql
installed on your machine may be older.

```bash
./scripts/run_all.sh        # fresh DB → every script with ON_ERROR_STOP=1 → results/ + summary
docker compose down         # stop the database afterwards
```

`run_all.sh` drops and recreates the database `boutique`, so the schema is always built on an
empty database. Every captured output goes to `results/`, and the pass/fail list goes to
`results/summary.txt`.

To run a single file by hand:

```bash
docker compose up -d --wait db
docker compose exec -T db psql -X -U postgres -d boutique -v ON_ERROR_STOP=1 -a -f /work/sql/03_queries.sql
```

## Files, in run order

| File | What it does | Expected result |
|---|---|---|
| `sql/01_schema.sql` | Tables, keys, named constraints, "order has items" trigger | exits 0 |
| `sql/02_seed.sql` | 3 customers, 3 products, 3 sizes, 3 colours, 7 options, 3 orders | exits 0; row counts printed |
| `sql/03_queries.sql` → `sql/queries/q*.sql` | Join queries | rows equal `expected/q*.csv` (written by hand from the seed) |
| `sql/violations/v01…v10` | One bad row per file | each exits 3 with the named constraint in the error |
| `sql/04_price_history.sql` | Raises a price; the old order keeps its price paid | exits 0 |
| `sql/05_order_success.sql` | Chloe's 2-item order in one transaction | exits 0; order, items, and stock all changed |
| `sql/06_order_oversell.sql` | Brian's order fails on item 2 after item 1 was written | **exits 3** (expected) |
| `sql/07_verify_after_orders.sql` | New session: success complete, oversell left nothing | exits 0 |
| `scripts/two_sessions.sh` | Two live sessions: visibility before/after COMMIT and after ROLLBACK | transcript in `results/08_two_sessions.out` |
| `erd.md` | Entity-relationship diagram (Mermaid) + 3NF reasoning | matches `results/01_catalog_checks.out` |

## Exit criteria → evidence

| ID | Evidence |
|---|---|
| D1-1 3NF schema | `sql/01_schema.sql` builds on an empty DB (`results/01_schema.out`); 3NF reasoning in `erd.md` |
| D1-2 Keys | Every table has a PK (check in `results/summary.txt`); every FK and its ON DELETE action in `results/01_catalog_checks.out` match the `erd.md` table |
| D1-3 Constraints | `results/v01…v10*.out`: NOT NULL, UNIQUE, CHECK, FOREIGN KEY (insert and delete-restrict), and the order-has-items trigger, each rejecting a bad row by name |
| D1-4 Joins | `results/q*.csv` equal `expected/q*.csv` (`results/q*.diff` are empty) |
| D1-5 Commit visibility | `results/08_two_sessions.out` steps 1–6: B sees the order **and** the stock change only after A's COMMIT |
| D1-6 Rollback atomicity | `results/06_order_oversell.out` (fails part-way) + `results/07_verify_after_orders.out` (no order, no items, stock restored); `results/08_two_sessions.out` steps 7–12 (rolled-back write never visible) |

## Things a reviewer might ask

- **Why does `v01` show the constraint name with a query?** PostgreSQL 18 names NOT NULL
  constraints, but its NOT NULL error message does not print the name; every other constraint
  kind does. The script prints the name from `pg_constraint` before the failing insert.
- **Why is there no order 5?** The rolled-back oversell used identity value 5. Identity and
  sequence values are not rolled back, so gaps are normal.
- **Why does `06` "fail"?** It's supposed to. Its error is the evidence, and `run_all.sh` expects exit code 3.
