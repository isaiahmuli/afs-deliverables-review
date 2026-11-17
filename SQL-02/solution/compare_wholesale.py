"""Compare two saved /api/wholesale responses, ignoring the timing fields.

Usage: python compare_wholesale.py BEFORE.json AFTER.json
Checks the order count, then every item row: same columns in the same order, same
values, same number of rows (so duplicates are preserved), and the same row order.
"""
import json
import sys

TIMING = {"elapsed_ms", "query_ms", "query_count"}

before, after = (json.load(open(path)) for path in sys.argv[1:3])
ok = True
for key in sorted((before.keys() | after.keys()) - TIMING - {"rows"}):
    same = before.get(key) == after.get(key)
    ok &= same
    print(f"{key}: before={before.get(key)} after={after.get(key)} {'same' if same else 'DIFFERENT'}")
rows_b, rows_a = before["rows"], after["rows"]
cols_b = [list(r) for r in rows_b]
cols_a = [list(r) for r in rows_a]
print(f"rows: before={len(rows_b)} after={len(rows_a)}")
print(f"columns (in order): before={cols_b[0]} after={cols_a[0]}")
checks = {
    "same row count": len(rows_b) == len(rows_a),
    "same columns in same order on every row": cols_b == cols_a,
    "same values in same row order": rows_b == rows_a,
}
for name, passed in checks.items():
    ok &= passed
    print(f"{name}: {'yes' if passed else 'NO'}")
print("query_count: before", before["query_count"], "after", after["query_count"])
print("RESULT:", "IDENTICAL" if ok else "DIFFERENT")
sys.exit(0 if ok else 1)
