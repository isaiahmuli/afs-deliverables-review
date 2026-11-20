# Boutique sale database

## Before you start on Windows

The commands below are written for bash (macOS, Linux, or WSL). In PowerShell,
use these equivalents instead:

```powershell
docker run -d --name boutique-pg -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:18 -c shared_preload_libraries=pg_stat_statements
function bpsql { $input | docker exec -i boutique-pg psql -X -v ON_ERROR_STOP=1 -U postgres -d boutique_sale @args }
Get-Content schema.sql | bpsql          # instead of: bpsql < schema.sql
python -m venv .venv
.venv\Scripts\Activate.ps1               # instead of: . .venv/bin/activate
$env:PGHOST="localhost"; $env:PGPORT="5432"; $env:PGDATABASE="boutique_sale"
$env:PGUSER="postgres"; $env:PGPASSWORD="postgres"
```

Define the `bpsql` function in each PowerShell window you use.

## Setup

Start PostgreSQL 18 in Docker with pg_stat_statements preloaded, then create the
practice database and the extension:

```bash
docker run -d --name boutique-pg -p 5432:5432 -e POSTGRES_PASSWORD=postgres \
  postgres:18 -c shared_preload_libraries=pg_stat_statements
docker exec boutique-pg createdb -U postgres boutique_sale
docker exec boutique-pg psql -U postgres -d boutique_sale \
  -c "CREATE EXTENSION pg_stat_statements"
```

The commands below use this alias for psql inside the container. Define it in
each terminal you use:

```bash
alias bpsql='docker exec -i boutique-pg psql -X -v ON_ERROR_STOP=1 -U postgres -d boutique_sale'
```

Any SQL tool also works: connect to localhost:5432, database `boutique_sale`,
user and password `postgres`, and run the files as they are.

| File | Command | What it shows |
|---|---|---|
| schema.sql | `bpsql < schema.sql` | Creates five empty tables. Run once per empty database. |
| seed.sql | `bpsql < seed.sql` | Loads fixed data and prints row counts. Run once after the schema. |
| slow_queries.sql | `bpsql < slow_queries.sql` | Plans and timings for order history and the packing queue. |
| report.sql | `bpsql < report.sql` | Saves the report header, waits 30 seconds, then prints the rows next to the header. |
| report_arrival.sql | `bpsql < report_arrival.sql` | Commits one arriving order while the report is paused. |
| report_reset.sql | `bpsql < report_reset.sql` | Removes the arriving order so the report can run again. |
| perf_check.py | `python perf_check.py QUERY_FILE TARGET_MS` | Runs one query seven times and fails if the median server execution time is over the target. Uses the dashboard's virtual environment and connection settings. |

The seed holds 100,000 customers, 20 categories, 2,000 products, 995,110 orders,
and 2,487,775 order items, dated across one year from 2025-11-01. Some customers
ordered once and some order often; customer 1 is the wholesale account with
2,000 orders. Most PENDING orders are from the last week, and a few older ones
are stuck. Products 1901–2000 have never sold. Prices paid belong to order
items and some were discounted. All dates and amounts are fixed.

## Dashboard

The supplied Python server is the shop's sale dashboard: shop totals, customer
order history, the packing queue, and the wholesale customer's items. Start it
from the extracted folder after loading the database:

```bash
python3 -m venv .venv
. .venv/bin/activate
python -m pip install -r requirements.txt
export PGHOST=localhost PGPORT=5432 PGDATABASE=boutique_sale PGUSER=postgres PGPASSWORD=postgres
python dashboard.py
```

Open http://127.0.0.1:8000 and use **Reload dashboard** to
repeat the load. Stop the server with Ctrl+C.

Each panel shows its elapsed time and SQL statement count. The tables preview
20 rows, but the server fetches the full query results. There are no
artificial delays or cached results. Timings depend on your machine.

The dashboard's SQL is at the top of dashboard.py; the wholesale panel's
loading code is in load_panel. Index changes affect the dashboard immediately.
Restart the server after editing dashboard.py.

pg_stat_statements keeps counting until you reset it. Run
`SELECT pg_stat_statements_reset();` before each run you want to measure.

## Choosing an index with an agent

One way to find the right index is to let an agent question you until you can
name it yourself. Paste the slow query and its plan where shown:

```text
I am choosing an index for a slow PostgreSQL 18 query. The orders table has
about 1M rows; its only index is the primary key on order_id.

Query:
<paste the query>

EXPLAIN (ANALYZE, BUFFERS) output:
<paste the plan>

Do not suggest an index yet. Ask me one question at a time about this query
and its data: what it filters on, how many rows match, how it sorts, and how
many rows it returns. Wait for each answer. When my answers are enough, ask me
to propose the index and explain the order of its columns, then tell me what
my proposal gets right or wrong, based on the plan.
```

Agents tend to answer their own questions or jump straight to an index; if it
does, ask it to stop and ask the next question. Answer from the plan and the
data, not from the agent's guesses. When you create the index, the new plan
should read about as many rows as the query returns, with no Sort step.

## Planning queries

Write two queries with these result contracts:

- Product ranking: `category_id, product_id, units_sold, sales_rank`.
  Sales count units in PAID or SHIPPED orders. Keep every product, including
  zero-sale products. Rank within each category by units sold, largest first.
  Equal unit counts share a rank, and the following rank has no gap.
  Sort the result by category_id and product_id.
- Quick returners: `customer_id, order_count, shortest_gap_days`.
  Return each customer who placed an order no more than 30 days (by
  created_at) after another of their orders. order_count counts all their
  orders. shortest_gap_days is the smallest difference in calendar dates
  between two such orders. Include all statuses. Sort by customer_id.

The `expected` folder holds the result of each query for the untouched seed.
A well-written version of each query runs in seconds on this data.
To compare, save your result as CSV and diff it, for example:

```bash
bpsql -c "COPY (SELECT ...) TO STDOUT WITH (FORMAT csv, HEADER)" > mine.csv
diff mine.csv expected/product_ranks.csv
```

Or export the result as CSV with headers from your SQL tool.

## Report run

Start report.sql in one session. While it waits, run report_arrival.sql in a
second session against the same database. When the report finishes, compare
the header columns with the columns computed from the rows. Run report_reset.sql
before the next run, and before checking the planning queries against `expected`.

## What to do

The task, the rules, and what to submit are in the deliverable.
