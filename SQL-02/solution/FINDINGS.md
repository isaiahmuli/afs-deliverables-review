# Findings: boutique database before the sale

Each symptom is tied to its cause, the fix, and pasted PostgreSQL 18 output. Full outputs are
in `evidence/`; every SQL file is in `sql/`.

## Dataset and measurement conditions

| | |
|---|---|
| Server | PostgreSQL 18.7 (`postgres:18` Docker image, aarch64), `shared_preload_libraries=pg_stat_statements`, extension 1.12 |
| Settings | Defaults: `shared_buffers` 128MB, `work_mem` 4MB, 2 parallel workers per gather, JIT on. No server tuning. |
| Machine | Apple M3 Pro (11 cores, 18 GB). Docker VM: 11 CPUs, 7.65 GiB. Idle apart from this work. |
| Data | Untouched starter seed: 100,000 customers, 20 categories, 2,000 products, 995,110 orders (19,213 PENDING, 29 of them stuck older ones), 2,487,775 order items. Database 354 MB. Customer 1 has 2,000 orders. |
| Connection | Port 55433 instead of 5432, because a local PostgreSQL already used 5432. `PGPORT=55433` for `perf_check.py` and the dashboard. |
| Query timing | `perf_check.py`: server execution time from `EXPLAIN (ANALYZE, FORMAT JSON)`, median of 7 runs, on a warm cache (each check was run more than once; the first plans read from disk). |
| Panel timing | Dashboard `elapsed_ms` from 5 loads of `/api/wholesale`; median reported. Includes network round trips. |

## 1. Order history is slow

**Symptom.** The order history query (newest 20 orders of one customer) took about 20 ms.

**Cause.** There is no index on `customer_id`, so PostgreSQL reads every order and sorts the
matches. Plan lines (`evidence/01_before_plans.out`):

```
->  Sort  (... rows=14.67 loops=3)
      Sort Key: created_at DESC, order_id DESC
      ->  Parallel Seq Scan on orders  (... rows=666.67 loops=3)
            Filter: (customer_id = 1)
            Rows Removed by Filter: 331037
            Buffers: shared hit=376 read=6969
Execution Time: 23.981 ms
```

It scans all 995,110 rows (`Rows Removed by Filter: 331037` per process × 3) and reads all
7,345 table pages to return 20 rows.

**Fix.** Composite index, with the equality column first and then the sort columns
(`sql/10_indexes.sql`):

```sql
CREATE INDEX orders_customer_created_idx ON orders (customer_id, created_at, order_id);
```

**After** (`evidence/04_after_plans.out`): the index is read backwards for the DESC order, it
stops after 20 rows, and there is no Sort step.

```
Limit  (... rows=20.00 loops=1)
  Buffers: shared hit=23 read=3
  ->  Index Scan Backward using orders_customer_created_idx on orders  (... rows=20.00 loops=1)
        Index Cond: (customer_id = 1)
Execution Time: 0.123 ms
```

The query SQL is unchanged.

## 2. Packing queue is slow

**Symptom.** The packing queue (oldest 100 PENDING orders) took about 20 ms.

**Cause.** The same problem: a full scan and a sort to find 19,213 PENDING rows among 995,110.

```
->  Sort  (... rows=87.00 loops=3)
      Sort Key: created_at, order_id
      ->  Parallel Seq Scan on orders  (... rows=6404.33 loops=3)
            Filter: (status = 'PENDING'::text)
            Rows Removed by Filter: 325299
Execution Time: 25.866 ms
```

**Fix.** Partial index holding only PENDING orders, in queue order:

```sql
CREATE INDEX orders_pending_created_idx ON orders (created_at, order_id) WHERE status = 'PENDING';
```

It is 608 kB, against 39 MB for the composite index above, because only 2% of orders are
PENDING.

**After:**

```
Limit  (... rows=100.00 loops=1)
  Buffers: shared hit=31 read=2
  ->  Index Scan using orders_pending_created_idx on orders  (... rows=100.00 loops=1)
Execution Time: 0.203 ms
```

The query reads 100 index entries and returns 100 rows, with no Sort and no Filter line.

## 3. Performance check: at least 10× faster, results preserved

The target is **1.9 ms**: the lowest baseline median (19.02 ms) divided by 10. Passing it
proves the "at least ten times faster" outcome.

```
# BEFORE indexes (evidence/02_perf_check_before.out)
sql/order_history.sql: median 16.03 ms over 7 runs, target 1.9 ms
FAIL                                  exit code: 1
sql/packing_queue.sql: median 20.46 ms over 7 runs, target 1.9 ms
FAIL                                  exit code: 1

# AFTER indexes (evidence/05_perf_check_after.out)
sql/order_history.sql: median 0.03 ms over 7 runs, target 1.9 ms
PASS                                  exit code: 0
sql/packing_queue.sql: median 0.04 ms over 7 runs, target 1.9 ms
PASS                                  exit code: 0
```

Baseline medians across four before-runs were 16.03–22.01 ms (order history) and
18.98–23.11 ms (packing queue). The gain is over 500×.

**Results preserved.** Each query was exported to CSV with index scans disabled
(`SET enable_indexscan = off` and the related settings, which gives the old plan) and again
with the indexes. The files are byte-identical (`evidence/06_results_preserved.out`):

```
order_history: 21 vs 21 lines; diff:
  identical
packing_queue: 101 vs 101 lines; diff:
  identical
```

## 4. The wholesale customer's order page is slow (N+1)

**Symptom.** The dashboard's wholesale panel took a median of **762 ms** per load. Every other
panel was fast.

**Cause.** `load_panel` ran one query for customer 1's orders and then **one more query per
order** for its items: 1 + 2,000 = 2,001 statements per load. `pg_stat_statements`, after
`pg_stat_statements_reset()` and one panel load (`evidence/07_wholesale_before_stats.out`):

```
 calls | rows | total_exec_ms | mean_exec_ms | query
-------+------+---------------+--------------+---------------------------------------------------------------
  2001 | 6897 |         16.67 |              | == TOTAL statements executed for the panel ==
  1934 | 4724 |         15.68 |        0.008 | SELECT order_id, item_no, ... FROM order_items WHERE order_id = $1 ORDER BY order_id, item_no
    66 |  173 |          0.59 |        0.009 | SELECT order_id, item_no, ... FROM order_items WHERE order_id = $1 ORDER BY order_id, item_no
     1 | 2000 |          0.39 |        0.394 | SELECT order_id FROM orders WHERE customer_id = $1 ORDER BY order_id
```

The per-order query appears twice (1,934 + 66 = 2,000 calls). psycopg sends order ids below
32,768 as `smallint` and the rest as `integer`, and `pg_stat_statements` counts the two forms
separately. The database spent only about 17 ms in total. The rest of the 762 ms is 2,001 round
trips.

**Fix.** `dashboard.py` now loads the panel with one query (`WHOLESALE_ITEMS`):

```sql
SELECT o.order_id AS wholesale_order_id,
       i.order_id, i.item_no, i.product_id, i.quantity, i.unit_price
FROM orders o
LEFT JOIN order_items i ON i.order_id = o.order_id
WHERE o.customer_id = 1
ORDER BY o.order_id, i.item_no
```

- The `LEFT JOIN` keeps every order, so `order_count` is still the number of orders even if an
  order had no items.
- Python drops the helper column, so each item row has the original five columns in the
  original order.
- The ordering matches the old loop: orders by `order_id`, then items by `item_no`.

**After** (`evidence/08_wholesale_after_stats.out`): one call.

```
 calls | rows | total_exec_ms | mean_exec_ms | query
-------+------+---------------+--------------+---------------------------------------------------------------
     1 | 4897 |          9.81 |        9.813 | SELECT o.order_id AS wholesale_order_id, i.order_id, ... FROM orders o LEFT JOIN order_items i ...
     1 | 4897 |          9.81 |              | == TOTAL statements executed for the panel ==
```

Panel time fell from a median of 762 ms to **31 ms** (`evidence/07_*_timings.out` and
`evidence/08_*_timings.out`; 5 loads each; the first load after a restart was 103 ms).

**Output preserved.** The full JSON responses before and after were compared field by field,
ignoring only the timing fields (`compare_wholesale.py`, `evidence/09_wholesale_compare.out`):

```
order_count: before=2000 after=2000 same
rows: before=4897 after=4897
columns (in order): before=['order_id', 'item_no', 'product_id', 'quantity', 'unit_price'] after=[same]
same row count: yes
same columns in same order on every row: yes
same values in same row order: yes
query_count: before 2001 after 1
RESULT: IDENTICAL
```

## 5. The finance report disagrees with its own rows

**Symptom.** Sometimes the report's header totals do not match the rows printed under them.
Rerunning it often looks fine.

**Cause.** `report.sql` runs at **READ COMMITTED**, where every statement takes a new snapshot.
The header is computed first. The rows are computed 30 seconds later, by a statement that also
sees any order committed in between. The mismatch appears only when an order arrives during
that window, which is why reruns usually look correct.

**Two sessions, READ COMMITTED** (`evidence/10_report_read_committed.out`):

```
21:38:55  A: BEGIN ISOLATION LEVEL READ COMMITTED; CREATE TEMP TABLE report_header ... ; pg_sleep(30) starts
21:39:00  B: BEGIN; INSERT order 1000001 (customer 1, PAID) + 1 item of 25.00; COMMIT
21:39:26  A: pg_sleep ends; SELECT rows next to the header; COMMIT

 order_id | order_revenue | orders_in_rows | revenue_in_rows | orders_in_header | revenue_in_header
      483 |       3585.34 |           2001 |      3707669.61 |             2000 |        3707644.61
  1000001 |         25.00 |           2001 |      3707669.61 |             2000 |        3707644.61
(2001 rows)
RESULT: header and rows DISAGREE (anomaly)
```

This is a non-repeatable read inside one transaction. The rows include order 1000001 and
25.00 more revenue than the header.

**Control.** The same file with no second session agrees at 2000 and 2000
(`evidence/12_report_control_no_arrival.out`). This confirms the problem depends on timing.

**Fix.** Run the report at **REPEATABLE READ** (`sql/40_report_repeatable_read.sql`; only the
`BEGIN` line changes). The transaction takes one snapshot at its first statement, and every
later statement reads that same snapshot.

**Two sessions, REPEATABLE READ** (`evidence/11_report_repeatable_read.out`), with the same
statement order:

```
21:39:26  A: BEGIN ISOLATION LEVEL REPEATABLE READ; CREATE TEMP TABLE report_header ... ; pg_sleep(30) starts
21:39:31  B: BEGIN; INSERT order 1000001 + 1 item; COMMIT
21:39:56  A: pg_sleep ends; SELECT rows next to the header; COMMIT

 order_id | order_revenue | orders_in_rows | revenue_in_rows | orders_in_header | revenue_in_header
      483 |       3585.34 |           2000 |      3707644.61 |             2000 |        3707644.61
(2000 rows)
RESULT: header and rows AGREE
```

Order 1000001 was still committed. The report does not see it because it arrived after the
report's snapshot. It appears in the next report. `report_reset.sql` was run after each
demonstration.

## 6. Planning queries

Both queries were exported with `COPY ... TO STDOUT WITH (FORMAT csv, HEADER)` on the untouched
seed and diffed against the answer keys (`evidence/14_final_checks.out`):

```
product_ranks: identical to expected/product_ranks.csv (2001 lines)
quick_returners: identical to expected/quick_returners.csv (36894 lines)
```

- **Product ranking** (`sql/20_product_ranks.sql`, 0.8 s):
  - A **CTE** totals units in PAID or SHIPPED orders.
  - `products` **LEFT JOIN** that CTE keeps the 100 products that never sold, with
    `units_sold = 0`.
  - The **window function** `dense_rank()` ranks products within each category; ties share a
    rank, with no gap after them.
  - The status filter sits inside the CTE. Putting it in a `WHERE` after the outer join would
    drop the unsold products.
- **Quick returners** (`sql/21_quick_returners.sql`, 1.5 s):
  - A **self join** of `orders` pairs each order with the customer's later orders placed no
    more than 30 days after it.
  - `min(b.created_at::date - a.created_at::date)` gives the shortest gap in calendar days.
  - A second **CTE** counts all orders per customer.

## 7. What changed, and what did not

- **Added:** the two indexes in `sql/10_indexes.sql`.
- **Rewritten:** the wholesale panel in `dashboard.py`.
- **Added:** a REPEATABLE READ copy of the report.
- **Unchanged:** no tables, columns, constraints, or seeded rows were changed, and there was no
  server tuning. `sql/50_final_checks.sql` still returns the README row counts, the arrival
  order is gone, and the only non-primary-key indexes are the two above.
