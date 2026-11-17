#!/usr/bin/env bash
# Two-session demonstration for the finance report.
#   Session A: a report file (header -> 30 s pause -> rows), run in the background.
#   Session B: report_arrival.sql, started 5 s after A, commits one wholesale order.
# Prints a timestamped statement order, then A's header-vs-rows totals, then resets.
# Usage: ./two_sessions.sh REPORT_FILE OUTPUT_FILE
set -euo pipefail
report=$1; out=$2
starter=../sql02_d01_starter
bpsql() { docker exec -i boutique-pg psql -X -v ON_ERROR_STOP=1 -U postgres -d boutique_sale "$@"; }
stamp() { date '+%H:%M:%S'; }
a_out=$(mktemp)
{
  echo "== Report file: $report"
  echo "== Isolation line: $(grep -m1 'BEGIN ISOLATION' "$report")"
  echo "$(stamp)  A: BEGIN; CREATE TEMP TABLE report_header ... (header totals read now); pg_sleep(30) starts"
  bpsql < "$report" > "$a_out" 2>&1 &
  a_pid=$!
  sleep 5
  echo "$(stamp)  B: report_arrival.sql (BEGIN; INSERT order 1000001 + 1 item; COMMIT)"
  bpsql < "$starter/report_arrival.sql" 2>&1 | sed 's/^/      B| /'
  echo "$(stamp)  B: committed. A is still paused inside its transaction."
  wait "$a_pid"
  echo "$(stamp)  A: pg_sleep ends; SELECT rows next to the header; COMMIT"
  echo
  echo "== Session A output (column header, first row, last row, footer)"
  grep -m1 -B0 -A2 ' order_id ' "$a_out" | sed 's/^/      A| /'
  echo "      A| ..."
  tail -n 5 "$a_out" | sed 's/^/      A| /'
  echo
  echo "== Header vs rows (first data row of A's output; every row carries the same totals)"
  awk -F'|' '$1 ~ /^ *[0-9]+ *$/ && NF == 6 { for (i = 1; i <= 6; i++) gsub(/ /, "", $i);
        printf "orders_in_rows=%s  orders_in_header=%s\nrevenue_in_rows=%s  revenue_in_header=%s\n", $3, $5, $4, $6;
        print (($3 == $5 && $4 == $6) ? "RESULT: header and rows AGREE" : "RESULT: header and rows DISAGREE (anomaly)"); exit }' "$a_out"
  echo
  echo "$(stamp)  reset: report_reset.sql"
  bpsql < "$starter/report_reset.sql" 2>&1 | sed 's/^/      reset| /'
} | tee "$out"
rm -f "$a_out"
