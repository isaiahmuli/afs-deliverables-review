---
module: SQL-02
code: SQL-02
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Get the boutique's database ready for the sale
subtitle: Diagnose slow queries and inconsistent reports, then prove your fixes with PostgreSQL evidence.
time_budget_min: 90
status: draft
---

## Topic: Deliverable

### Exercise: Investigate the boutique's database

An online boutique is preparing for a sale. On a copy of its data, order history and the packing queue load slowly. The biggest wholesale customer's order page takes much longer than anyone else's.

The finance report sometimes disagrees with its own rows. Rerunning it does not always show the mismatch, making the results hard to trust.

The sale team also needs two lists before tomorrow. One ranks each category's products by units sold, to choose what to promote. The other finds the customers who reordered within 30 days, to invite them first.

Find the causes, fix them in the database, and prove each fix with PostgreSQL evidence. Write the two planning queries.

:::download ./assets/sql02_d01_starter.zip Boutique sale starter
The archive holds the README, schema, deterministic seed, slow queries, sale dashboard, report scripts, performance check, and answer keys.
:::

:::note
**Time limit:** 90 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

- Two planning queries rank each category's products by units sold and identify customers who ordered again within 30 days. Unsold products remain in the ranking.
- Both queries match the result contracts and answer keys in the starter README.
- Order history and the packing queue each run at least ten times faster, with their results preserved.
- The dashboard's wholesale panel gets its items in one query, preserving the original columns, values, duplicate counts, and ordering.
- The report's totals and rows agree even when another session commits an order while the report is running.
- A findings note connects each symptom to its cause and fix, supported by pasted PostgreSQL output.

### Theory: Rules

- Use PostgreSQL 18 with `pg_stat_statements` enabled. This extension records query call counts and execution statistics.
- Keep the supplied tables and seeded data unchanged. Indexes may be added; query SQL and the dashboard's wholesale panel may be rewritten.
- Submit SQL scripts, the edited `dashboard.py`, and the findings note. Names and file layout are yours.
- Other schema changes, server tuning, functions, triggers, and partitioning are out of scope.

### Theory: Evidence

The starter README lists each file's command and expected behavior. Its answer keys apply to the untouched seed.

Include old and new SQL, before-and-after plans, and the plan lines that support each diagnosis. Use `EXPLAIN (ANALYZE, BUFFERS)` for plan evidence.

Record the dataset size, measurement conditions, and median timings from several runs. Run the supplied `perf_check.py` with a target you choose: it must fail before the fix and pass afterward.

Include workload statistics showing the wholesale panel's call counts, plus a result comparison proving the rewrite preserves its output.

For the report, show the exact statement order and results from two sessions. Demonstrate the mismatch at one isolation level and its prevention at a stricter level.

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Write analytical queries with joins, CTEs, and window functions | The queries use an outer join, a self join, a CTE, and a window function, and return the expected rows |
| D1-2 | Diagnose a slow query from its plan | Each slowdown's cause is tied to a line of plan output, with the after-plan showing the change |
| D1-3 | Design composite and partial indexes for a workload | A composite and a partial index each serve the query they were built for |
| D1-4 | Assert query performance against a target | A repeatable check fails before the fix and passes after, using the median of several runs |
| D1-5 | Detect and fix an N+1 query pattern | pg_stat_statements shows per-order calls before the fix and one call after |
| D1-6 | Demonstrate an isolation anomaly and its prevention | Two sessions reproduce the anomaly at one isolation level and not at a stricter one |

### Exercise: What to submit

- SQL scripts containing the planning queries, fixes, result comparisons, performance check, and two-session report demonstration.
- The edited `dashboard.py` with the wholesale panel fixed.
- The findings note with PostgreSQL evidence for each symptom and its resolution.
