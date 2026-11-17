#!/usr/bin/env bash
# Rebuilds the boutique database from empty and captures every piece of evidence into results/.
# Every SQL file runs with ON_ERROR_STOP=1. Expected failures are run one file at a time and
# must exit with psql code 3 AND name the expected constraint.
set -uo pipefail
cd "$(dirname "$0")/.."
DB=boutique
RES=results
mkdir -p "$RES"; rm -f "$RES"/*.out "$RES"/*.csv "$RES"/summary.txt

docker compose up -d --wait db >/dev/null
psql_db() { docker compose exec -T db psql -X -U postgres -d "$DB" -v ON_ERROR_STOP=1 "$@"; }

pass=0; fail=0
check() { # name, condition-result(0/1), detail
  if [ "$2" -eq 0 ]; then echo "PASS  $1" | tee -a "$RES/summary.txt"; pass=$((pass+1));
  else echo "FAIL  $1  ($3)" | tee -a "$RES/summary.txt"; fail=$((fail+1)); fi; }

# run <step-name> <sql-file> <expected-exit>
run() {
  local name=$1 file=$2 want=$3
  psql_db -a -f "/work/$file" > "$RES/$name.out" 2>&1; local got=$?
  [ "$got" -eq "$want" ]; check "$name exits $want" $? "got $got, see $RES/$name.out"
}

echo "== Fresh database"
docker compose exec -T db psql -X -U postgres -v ON_ERROR_STOP=1 -q \
  -c "DROP DATABASE IF EXISTS $DB WITH (FORCE)" -c "CREATE DATABASE $DB" >/dev/null
docker compose exec -T db psql --version | tee "$RES/00_versions.out"
psql_db -Atc "SHOW server_version" | sed 's/^/server /' | tee -a "$RES/00_versions.out"

echo "== Schema and seed (D1-1, D1-2)"
run 01_schema sql/01_schema.sql 0
run 02_seed   sql/02_seed.sql   0
psql_db -c '\d+ customer' -c '\d+ product' -c '\d+ size' -c '\d+ colour' -c '\d+ product_variant' \
        -c '\d+ customer_order' -c '\d+ order_item' > "$RES/01_schema_describe.out" 2>&1
psql_db -f /work/scripts/catalog_checks.sql > "$RES/01_catalog_checks.out" 2>&1
psql_db -Atc "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'public' AND c.relkind = 'r' AND NOT EXISTS (SELECT 1 FROM pg_constraint k WHERE k.conrelid = c.oid AND k.contype = 'p')" | grep -qx 0
check "every table has a primary key" $? "see $RES/01_catalog_checks.out"

echo "== Join queries vs hand-written expected rows (D1-4)"
run 03_queries sql/03_queries.sql 0
for q in sql/queries/*.sql; do
  n=$(basename "$q" .sql)
  psql_db --csv -f "/work/$q" 2>&1 | grep -v '^SET$' > "$RES/$n.csv"
  diff -u "expected/$n.csv" "$RES/$n.csv" > "$RES/$n.diff"; check "$n matches expected/$n.csv" $? "see $RES/$n.diff"
done

echo "== Constraint violations, one file each (D1-3)"
for v in sql/violations/*.sql; do
  n=$(basename "$v" .sql); want=$(sed -n 's/^-- expect-constraint: //p' "$v")
  err=$(sed -n 's/^-- expect-error: //p' "$v")
  psql_db -a -f "/work/$v" > "$RES/$n.out" 2>&1; got=$?
  # Look only at the server's error report, never at echoed script lines (they start with "--").
  report=$(sed -n '/ERROR:/,$p' "$RES/$n.out")
  if [ -n "$err" ]; then   # NOT NULL: error has no constraint name; match message + catalog name
    { [ "$got" -eq 3 ] && grep -qF "$err" <<<"$report" && grep -qE "^ *$want *\|" "$RES/$n.out"; }
  else
    { [ "$got" -eq 3 ] && grep -qE "CONSTRAINT NAME: +$want$|constraint \"$want\"" <<<"$report"; }
  fi
  check "$n rejected by $want" $? "exit $got"
done

echo "== Price history"
run 04_price_history sql/04_price_history.sql 0

echo "== Successful order and oversell (D1-6)"
run 05_order_success      sql/05_order_success.sql      0
run 06_order_oversell     sql/06_order_oversell.sql     3
sed -n '/ERROR:/,$p' "$RES/06_order_oversell.out" | grep -qE 'CONSTRAINT NAME: +product_variant_stock_quantity_ck$'; check "06 oversell rejected by product_variant_stock_quantity_ck" $? ""
run 07_verify_after_orders sql/07_verify_after_orders.sql 0

echo "== Two sessions: visibility before/after COMMIT and after ROLLBACK (D1-5, D1-6)"
scripts/two_sessions.sh > "$RES/08_two_sessions.out" 2>&1; check "08 two-session demo ran" $? "see $RES/08_two_sessions.out"

echo
echo "$pass passed, $fail failed" | tee -a "$RES/summary.txt"
[ "$fail" -eq 0 ]
