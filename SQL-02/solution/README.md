# SQL-02 submission: boutique sale database

Start with `FINDINGS.md`. It covers each symptom, its cause and fix, and the PostgreSQL output
that proves it. The supplied starter is in `../sql02_d01_starter/` and was not modified.

## Files

| File | What it is |
|---|---|
| `FINDINGS.md` | Findings note (required) |
| `dashboard.py` | Edited dashboard: the wholesale panel now uses one query (required) |
| `dashboard.html` | Unchanged copy from the starter, so the edited dashboard can run from this folder |
| `sql/order_history.sql`, `sql/packing_queue.sql` | The two slow queries, one SELECT per file, as `perf_check.py` needs |
| `sql/10_indexes.sql` | The fixes: one composite index and one partial index |
| `sql/20_product_ranks.sql`, `sql/21_quick_returners.sql` | Planning queries |
| `sql/30_wholesale_stats.sql` | pg_stat_statements query for the wholesale panel's call counts |
| `sql/40_report_repeatable_read.sql` | Fixed report: `report.sql` at REPEATABLE READ |
| `sql/50_final_checks.sql` | Row counts and index list, showing the data is unchanged |
| `two_sessions.sh` | Runs a report file in session A and `report_arrival.sql` in session B 5 s later, then resets |
| `compare_wholesale.py` | Compares two saved `/api/wholesale` responses, ignoring timings |
| `results/` | Planning query output and empty diffs against `expected/` |
| `evidence/` | Every captured output referenced in `FINDINGS.md` |

## Reproduce

These steps follow the starter README. The only change is port **55433**, because 5432 was
already in use on this machine.

```bash
docker run -d --name boutique-pg -p 55433:5432 -e POSTGRES_PASSWORD=postgres \
  postgres:18 -c shared_preload_libraries=pg_stat_statements
docker exec boutique-pg createdb -U postgres boutique_sale
docker exec boutique-pg psql -U postgres -d boutique_sale -c "CREATE EXTENSION pg_stat_statements"
alias bpsql='docker exec -i boutique-pg psql -X -v ON_ERROR_STOP=1 -U postgres -d boutique_sale'
bpsql < ../sql02_d01_starter/schema.sql && bpsql < ../sql02_d01_starter/seed.sql

python3 -m venv .venv && . .venv/bin/activate && pip install -r ../sql02_d01_starter/requirements.txt
export PGHOST=localhost PGPORT=55433 PGDATABASE=boutique_sale PGUSER=postgres PGPASSWORD=postgres

python ../sql02_d01_starter/perf_check.py sql/order_history.sql 1.9   # FAIL before the fix
python ../sql02_d01_starter/perf_check.py sql/packing_queue.sql 1.9   # FAIL before the fix
bpsql < sql/10_indexes.sql
python ../sql02_d01_starter/perf_check.py sql/order_history.sql 1.9   # PASS
python ../sql02_d01_starter/perf_check.py sql/packing_queue.sql 1.9   # PASS

./two_sessions.sh ../sql02_d01_starter/report.sql out_rc.txt          # header and rows DISAGREE
./two_sessions.sh sql/40_report_repeatable_read.sql out_rr.txt        # header and rows AGREE

python dashboard.py      # http://127.0.0.1:8000, wholesale panel: 1 statement
```
