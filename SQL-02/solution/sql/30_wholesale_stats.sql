-- pg_stat_statements for the dashboard's wholesale panel.
-- Run SELECT pg_stat_statements_reset(); first, load the panel once, then run this.
-- Notes:
--   * pg_stat_statements replaces constants with $1, so match on text, not on "= 1".
--   * psycopg sends small order ids as int2 and larger ones as int4, so the same
--     per-order query can appear as TWO rows; the total below adds them up.
WITH panel AS (
    SELECT calls, rows, total_exec_time, mean_exec_time,
           regexp_replace(query, '\s+', ' ', 'g') AS query
    FROM pg_stat_statements
    WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
      AND (query ILIKE '%order_items%' OR query ILIKE '%customer_id = $1%')
      AND query NOT ILIKE '%pg_stat_statements%'
)
SELECT calls, rows,
       round(total_exec_time::numeric, 2) AS total_exec_ms,
       round(mean_exec_time::numeric, 3) AS mean_exec_ms,
       query
FROM panel
UNION ALL
SELECT sum(calls), sum(rows), round(sum(total_exec_time)::numeric, 2), NULL,
       '== TOTAL statements executed for the panel =='
FROM panel
ORDER BY 1 DESC, 5;
