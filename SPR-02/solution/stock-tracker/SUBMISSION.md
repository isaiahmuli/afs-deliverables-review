# Office stock tracker: submission notes

Generated from start.spring.io (Spring Boot 4.2.0, Java 25, Gradle wrapper 9.9.2; dependencies:
Spring Data JPA, Flyway, PostgreSQL driver, Testcontainers). Resolved versions: Hibernate
7.4.15.Final, HikariCP 7.1.0, Flyway 12.11.0, Testcontainers 2.0.5, JUnit 6.1.3. Tests run on
`postgres:18.7` (the generated project used `postgres:latest`; pinned to meet the PostgreSQL 18 rule).

- **Spec (written first):** `docs/SPEC.md`: goal, operations, data and relationships, rules,
  failure behavior, acceptance checks A1–A13.
- There is no UI or API, as the rules require. Every operation is a service method, shown by tests.

## Run it

Docker must be running.

```bash
./gradlew clean build      # 34 tests, all against PostgreSQL 18 in containers
```

## Evidence

`evidence/01_clean_build_tests.txt` is a clean build from a fresh copy (no `build/` or `.gradle/`):
34 tests passed. Its printed lines:

```text
[rollback] during the failed slip: 1 product update and 1 history insert were sent to PostgreSQL; afterwards pens = 10, pens history rows = 1
[pitfall: plain @Transactional] the slip failed, but pens = 30 and pens history rows = 2
[fixed: rollbackFor = InsufficientStockException] pens = 10, pens history rows = 1
[N+1]  10 products: naive report  11 SQL statements, fixed report 1 SQL statement
[N+1]  50 products: naive report  51 SQL statements, fixed report 1 SQL statement
[N+1] 200 products: naive report 201 SQL statements, fixed report 1 SQL statement
[concurrency] 12 staff took 1 notebook at once: 2 saved, 10 rejected with ConcurrentStockChangeException, notebooks now 23
```

(The concurrency split varies from run to run; the test checks that the count always equals
25 minus the saved changes, and the history has one row per saved change.)

## Schema

| Migration | Creates |
|---|---|
| `V1__catalog.sql` | `product`, `category`, `product_category` (join table) |
| `V2__stock_history_and_version.sql` | `stock_adjustment`; adds `product.version` with default 0, so V1 rows are kept |

`spring.jpa.hibernate.ddl-auto=validate`: Hibernate only checks the mappings against Flyway's schema.

## How each exit criterion is met

| ID | Where |
|---|---|
| D1-1 Mappings and repositories | `Product` ↔ `Category` many-to-many through `product_category`; `Product` → `StockAdjustment` one-to-many. Derived methods (`findByNameContainingIgnoreCaseOrderByNameAsc`, `findByProductIdOrderByAdjustedAtAscIdAsc`, ...) and `@Query` methods (`findInCategory`, `findAllWithCategories`). `RepositoryMappingTest` (`@DataJpaTest`, PostgreSQL 18 container) persists, flushes, clears, and reloads each relationship from both sides. |
| D1-2 Flyway | Two versioned migrations; `ddl-auto=validate`. `MigrationTest` applies all migrations to an empty PostgreSQL 18 database and shows V1 records survive V2. `StockTrackerApplicationTests` checks the history table and the validate setting. |
| D1-3 Transactions | `TransactionRollbackTest`: a slip writes pens (UPDATE + INSERT sent) and then fails on staples; everything rolls back. **Pitfall (checked exception):** the same slip under plain `@Transactional` commits the pens line (pens 30); with `rollbackFor = InsufficientStockException.class` it doesn't (pens 10). The "before" version exists only in the test. |
| D1-4 Optimistic locking | `Product.version` is `@Version`. `OptimisticLockingTest`: two staff read pens = 10 together; Amara's +20 commits, Brian's −5 is rejected (`OptimisticLockingFailureException`), stock stays 30, and his retry gives 25. A stale detached copy is rejected and the newer value kept. 12 concurrent removals never lose a change. |
| D1-5 N+1 | Categories are lazy. `StockReportQueryCountTest` counts statements with Hibernate statistics at 10, 50 and 200 products: the naive report runs 1 + N, the `left join fetch` report runs 1. |
| D1-6 SQL injection | User input only reaches derived methods and bound `@Query` parameters; no string building. Tests: `' OR '1'='1`, `'; DROP TABLE product; --` and `_` return nothing; `%` finds only "100% recycled paper"; `'` finds Painter's tape. |

## Decisions

- **Concurrent changes:** optimistic locking with `@Version`. The second writer gets
  `ConcurrentStockChangeException` ("changed by someone else ... try again"); `StockLedger`
  flushes inside the method so the conflict is reported there rather than at commit.
- **`InsufficientStockException` is checked**, so every method that throws it declares
  `rollbackFor`. That choice is also the D1-3 pitfall.
- **Search** ignores case and matches part of the name; blank input returns `[]`.
- **Unknown category:** `CategoryNotFoundException` for both listing and assigning.
- Rollback and locking tests use `@SpringBootTest`, not `@DataJpaTest`: the slice wraps every test
  in one transaction that is rolled back, so commits, rollbacks and two concurrent transactions
  can't be observed there.
