#!/usr/bin/env bash
# Two live psql sessions (A writes, B reads) against the same database, interleaved step by step.
# Shows: B cannot see A's uncommitted multi-table write; B sees it right after COMMIT;
#        B never sees a write that A rolls back.
# Each session is a long-lived psql 18 process with ON_ERROR_STOP=1, fed through a named pipe.
set -euo pipefail
cd "$(dirname "$0")/.."
TMP=$(mktemp -d); trap 'exec 3>&- 4>&- 2>/dev/null; rm -rf "$TMP"' EXIT
mkfifo "$TMP/a.in" "$TMP/b.in"; : > "$TMP/a.out"; : > "$TMP/b.out"

start() { docker compose exec -T db psql -X -U postgres -d boutique -v ON_ERROR_STOP=1 -a <"$TMP/$1.in" >"$TMP/$1.out" 2>&1 & }
start a; start b
exec 3>"$TMP/a.in" 4>"$TMP/b.in"
seen_a=0; seen_b=0; n=0   # lines already printed per session (bash 3.2 compatible)

# step <a|b> <label> <sql...>: send SQL, wait until the session has processed it, print its new output.
step() {
  local s=$1 label=$2; shift 2; n=$((n+1)); local fd=3; [ "$s" = b ] && fd=4
  printf '%s\n' "$@" "\\echo __done_${n}__" >&$fd
  until grep -q "^__done_${n}__$" "$TMP/$s.out"; do sleep 0.1; done
  local S; S=$(echo "$s" | tr a-z A-Z)
  local seen; eval "seen=\$seen_$s"
  echo; echo "[$n] Session $S: $label"
  tail -n +"$((seen+1))" "$TMP/$s.out" | grep -v "__done_" | sed "s/^/    $S| /"
  eval "seen_$s=$(( $(wc -l < "$TMP/$s.out") ))"
}

B_VIEW="SELECT (SELECT count(*) FROM customer_order WHERE ordered_at = '2026-11-16 12:30:00+03') AS amina_orders_1230, (SELECT count(*) FROM customer_order WHERE ordered_at = '2026-11-16 12:45:00+03') AS brian_orders_1245, (SELECT stock_quantity FROM product_variant pv JOIN product p USING (product_id) JOIN size s USING (size_id) JOIN colour c USING (colour_id) WHERE (p.name, s.code, c.name) = ('Linen Shirt','S','White')) AS shirt_s_white_stock, (SELECT stock_quantity FROM product_variant pv JOIN product p USING (product_id) JOIN size s USING (size_id) JOIN colour c USING (colour_id) WHERE (p.name, s.code, c.name) = ('Wrap Dress','S','Olive')) AS dress_s_olive_stock;"

echo "=== Part 1: visibility before and after COMMIT ==="
step b "baseline" "$B_VIEW"
step a "BEGIN an order: Amina, 1 x Linen Shirt S White (writes customer_order, order_item, product_variant)" \
  "BEGIN;" \
  "INSERT INTO customer_order (customer_id, ordered_at) SELECT customer_id, '2026-11-16 12:30:00+03' FROM customer WHERE email = 'amina@example.com' RETURNING order_id AS a_order \\gset" \
  "INSERT INTO order_item (order_id, variant_id, quantity, unit_price) SELECT :a_order, pv.variant_id, 1, p.current_price FROM product_variant pv JOIN product p USING (product_id) JOIN size s USING (size_id) JOIN colour c USING (colour_id) WHERE (p.name, s.code, c.name) = ('Linen Shirt','S','White');" \
  "UPDATE product_variant pv SET stock_quantity = pv.stock_quantity - 1 FROM product p, size s, colour c WHERE p.product_id = pv.product_id AND s.size_id = pv.size_id AND c.colour_id = pv.colour_id AND (p.name, s.code, c.name) = ('Linen Shirt','S','White');"
step a "A sees its own uncommitted write" "$B_VIEW"
step b "B BEFORE A commits: must still show the baseline (no order, old stock)" "$B_VIEW"
step a "COMMIT" "COMMIT;"
step b "B AFTER A commits: order visible and stock reduced, all at once" "$B_VIEW"

echo; echo "=== Part 2: a rolled-back write is never visible ==="
step a "BEGIN an order: Brian, 1 x Wrap Dress S Olive" \
  "BEGIN;" \
  "INSERT INTO customer_order (customer_id, ordered_at) SELECT customer_id, '2026-11-16 12:45:00+03' FROM customer WHERE email = 'brian@example.com' RETURNING order_id AS b_order \\gset" \
  "INSERT INTO order_item (order_id, variant_id, quantity, unit_price) SELECT :b_order, pv.variant_id, 1, p.current_price FROM product_variant pv JOIN product p USING (product_id) JOIN size s USING (size_id) JOIN colour c USING (colour_id) WHERE (p.name, s.code, c.name) = ('Wrap Dress','S','Olive');" \
  "UPDATE product_variant pv SET stock_quantity = pv.stock_quantity - 1 FROM product p, size s, colour c WHERE p.product_id = pv.product_id AND s.size_id = pv.size_id AND c.colour_id = pv.colour_id AND (p.name, s.code, c.name) = ('Wrap Dress','S','Olive');"
step a "A sees its own uncommitted write" "$B_VIEW"
step b "B while A's transaction is open: not visible" "$B_VIEW"
step a "ROLLBACK" "ROLLBACK;"
step b "B AFTER A rolls back: nothing changed" "$B_VIEW"
step a "A after ROLLBACK: its own write is gone too" "$B_VIEW"

printf '\\q\n' >&3; printf '\\q\n' >&4; wait
