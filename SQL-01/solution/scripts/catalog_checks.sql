-- Catalog evidence for D1-1/D1-2/D1-3: keys, foreign keys with ON DELETE actions, named constraints.
\echo '--- Tables without a primary key (must be 0)'
SELECT count(*) AS tables_without_pk
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind = 'r'
  AND NOT EXISTS (SELECT 1 FROM pg_constraint k WHERE k.conrelid = c.oid AND k.contype = 'p');

\echo '--- Foreign keys and their ON DELETE action (compare with erd.md)'
SELECT conrelid::regclass AS child_table, conname AS constraint_name, confrelid::regclass AS parent_table,
       CASE confdeltype WHEN 'a' THEN 'NO ACTION' WHEN 'r' THEN 'RESTRICT' WHEN 'c' THEN 'CASCADE'
                        WHEN 'n' THEN 'SET NULL' WHEN 'd' THEN 'SET DEFAULT' END AS on_delete
FROM pg_constraint WHERE contype = 'f' ORDER BY 1, 2;

\echo '--- All named constraints by kind (p=primary key, f=foreign key, u=unique, c=check, n=not null, t=trigger)'
SELECT conrelid::regclass AS table_name, contype AS kind, conname AS constraint_name
FROM pg_constraint WHERE connamespace = 'public'::regnamespace ORDER BY 1, 2, 3;
