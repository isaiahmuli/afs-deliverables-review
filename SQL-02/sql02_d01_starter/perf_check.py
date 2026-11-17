"""Check that a query's median execution time is under a target.

Runs the query several times with EXPLAIN (ANALYZE, FORMAT JSON) and uses the
server's execution time, so network delay does not count. Exits with status 1
when the median is over the target.

Usage: python perf_check.py QUERY_FILE TARGET_MS [--runs N]
Connection settings come from PGHOST, PGPORT, PGDATABASE, PGUSER, and PGPASSWORD.
"""

import argparse
import statistics
import sys
from pathlib import Path

import psycopg


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("query_file", help="file holding one SELECT statement")
    parser.add_argument("target_ms", type=float, help="the median must not exceed this")
    parser.add_argument("--runs", type=int, default=7)
    args = parser.parse_args()

    sql = Path(args.query_file).read_text().strip().rstrip(";")
    with psycopg.connect() as connection:
        times = []
        for _ in range(args.runs):
            plan = connection.execute("EXPLAIN (ANALYZE, FORMAT JSON) " + sql).fetchone()[0]
            times.append(plan[0]["Execution Time"])

    median = statistics.median(times)
    print(f"{args.query_file}: median {median:.2f} ms over {args.runs} runs, target {args.target_ms:g} ms")
    if median > args.target_ms:
        print("FAIL")
        sys.exit(1)
    print("PASS")


if __name__ == "__main__":
    main()
