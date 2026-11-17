"""A local, read-only dashboard over the boutique starter database."""

import argparse
import getpass
import json
import os
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from time import perf_counter
from urllib.parse import parse_qs, urlsplit

import psycopg
from psycopg.rows import dict_row


OVERVIEW = """
SELECT (SELECT count(*) FROM orders) AS orders,
       (SELECT count(*) FROM customers) AS customers,
       (SELECT count(*) FROM products) AS products,
       (SELECT count(*) FROM orders WHERE status = 'PENDING') AS pending
"""

ORDER_HISTORY = """
SELECT order_id, customer_id, status, created_at
FROM orders
WHERE customer_id = %s
ORDER BY created_at DESC, order_id DESC
LIMIT 20
"""

PACKING_QUEUE = """
SELECT order_id, customer_id, status, created_at
FROM orders
WHERE status = 'PENDING'
ORDER BY created_at, order_id
LIMIT 100
"""

# Wholesale panel: one query instead of one per order (the old N+1 pattern).
# The LEFT JOIN keeps every wholesale order, so order_count is still the number of
# orders even if one had no items; such an order adds no item row in load_panel.
# Ordering is the same as before: orders by order_id, then items by item_no.
WHOLESALE_ITEMS = """
SELECT o.order_id AS wholesale_order_id,
       i.order_id, i.item_no, i.product_id, i.quantity, i.unit_price
FROM orders o
LEFT JOIN order_items i ON i.order_id = o.order_id
WHERE o.customer_id = 1
ORDER BY o.order_id, i.item_no
"""


def load_panel(panel, password, customer_id=1):
    started = perf_counter()
    query_count = 0
    query_ms = 0

    with psycopg.connect(
        password=password,
        row_factory=dict_row,
        application_name="boutique_dashboard",
        connect_timeout=5,
    ) as connection:
        connection.read_only = True

        def query(sql, params=None):
            nonlocal query_count, query_ms
            tick = perf_counter()
            rows = connection.execute(sql, params).fetchall()
            query_ms += (perf_counter() - tick) * 1000
            query_count += 1
            return rows

        if panel == "overview":
            result = {"stats": query(OVERVIEW)[0]}
        elif panel == "history":
            result = {"rows": query(ORDER_HISTORY, (customer_id,))}
        elif panel == "packing":
            result = {"rows": query(PACKING_QUEUE)}
        elif panel == "wholesale":
            rows = query(WHOLESALE_ITEMS)
            order_count = len({row["wholesale_order_id"] for row in rows})
            items = [
                {key: row[key] for key in ("order_id", "item_no", "product_id", "quantity", "unit_price")}
                for row in rows
                if row["order_id"] is not None
            ]
            result = {"rows": items, "order_count": order_count}
        else:
            raise ValueError("Unknown panel")

    # Keep all fetched rows in the response, even though the page previews 20.
    # The timings include real query execution, fetching, and database round trips.
    result.update(
        elapsed_ms=round((perf_counter() - started) * 1000, 2),
        query_ms=round(query_ms, 2),
        query_count=query_count,
    )
    return result


class DashboardHandler(BaseHTTPRequestHandler):
    def respond(self, status, body, content_type):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        try:
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def json_response(self, status, value):
        self.respond(status, json.dumps(value, default=str).encode(), "application/json")

    def do_GET(self):
        route = urlsplit(self.path)
        if route.path == "/":
            page = Path(__file__).with_name("dashboard.html").read_bytes()
            self.respond(200, page, "text/html; charset=utf-8")
            return
        if route.path == "/favicon.ico":
            self.respond(204, b"", "image/x-icon")
            return
        if route.path not in {"/api/overview", "/api/history", "/api/packing", "/api/wholesale"}:
            self.json_response(404, {"error": "Not found"})
            return
        try:
            raw_id = parse_qs(route.query).get("customer", ["1"])[0]
            customer_id = int(raw_id)
            if not 1 <= customer_id <= 9223372036854775807:
                raise ValueError
        except ValueError:
            self.json_response(400, {"error": "Customer must be a positive whole number."})
            return
        try:
            data = load_panel(route.path.rsplit("/", 1)[-1], self.server.db_password, customer_id)
            self.json_response(200, data)
        except psycopg.Error as error:
            print(f"Database error: {error.diag.message_primary or type(error).__name__}", file=sys.stderr)
            self.json_response(503, {"error": "Database request failed. Check the server terminal and connection settings."})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()
    password = os.environ.get("PGPASSWORD")
    if password is None:
        password = getpass.getpass("Database password (blank for local authentication): ") or None
    server = ThreadingHTTPServer(("127.0.0.1", args.port), DashboardHandler)
    server.db_password = password
    print(f"Boutique dashboard: http://127.0.0.1:{args.port}", flush=True)
    print("Uses PGHOST, PGPORT, PGDATABASE and PGUSER. Press Ctrl+C to stop.", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nDashboard stopped.")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
