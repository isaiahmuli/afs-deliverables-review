#!/usr/bin/env bash
# Sends each request and prints it with the full response. Usage: requests.sh <base-url> <case...>
BASE=$1; shift
show() { # method path [json]
  echo "### $1 $2"; [ -n "$3" ] && echo "$3"
  echo "--- response"
  if [ -n "$3" ]; then curl -s -i -X "$1" "$BASE$2" -H 'Content-Type: application/json' -d "$3"
  else curl -s -i -X "$1" "$BASE$2"; fi | grep -vE '^(Date|Keep-Alive|Connection|Transfer-Encoding):' | tr -d '\r'
  echo; echo
}
for c in "$@"; do case $c in
  valid)    show POST /api/expenses '{"employee":"Amina Otieno","description":"Fuel for client visit (business share of $120 receipt)","expenseDate":"2026-11-18","receiptTotal":120.00,"claimAmount":50.00}' ;;
  list)     show GET /api/expenses ;;
  get1)     show GET /api/expenses/1 ;;
  unknown)  show GET /api/expenses/999 ;;
  empty)    show POST /api/expenses '{}' ;;
  several)  show POST /api/expenses '{"employee":"  ","expenseDate":"2999-01-01","receiptTotal":12.345,"claimAmount":-5}' ;;
  over)     show POST /api/expenses '{"employee":"Brian Kamau","description":"Taxi","expenseDate":"2026-11-17","receiptTotal":50.00,"claimAmount":80.00}' ;;
  badtype)  show POST /api/expenses '{"employee":"Brian Kamau","description":"Taxi","expenseDate":"2026-11-17","receiptTotal":"fifty","claimAmount":50.00}' ;;
  limit150) show POST /api/expenses '{"employee":"Carol Wanjiru","description":"Hotel night","expenseDate":"2026-11-16","receiptTotal":150.00,"claimAmount":150.00}' ;;
  health)   show GET /actuator/health ;;
  actuator) show GET /actuator ;;
  metrics)  show GET '/actuator/metrics/http.server.requests?tag=uri:/api/expenses' ;;
  hidden)   for e in env configprops beans; do show GET /actuator/$e; done ;;
esac; done
