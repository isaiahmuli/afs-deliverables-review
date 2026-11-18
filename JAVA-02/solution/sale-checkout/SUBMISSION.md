# Sale checkout: submission notes

This is the starter project with the checkout fixed and extended. `README.md` is the starter's
own README, left unchanged.

## Run it

```bash
./gradlew clean build      # 73 tests, JaCoCo report, and the 80% line-coverage check
./gradlew run              # the 200-shopper sale: exactly 60 accepted
```

The coverage report is at `build/reports/jacoco/test/html/index.html`.

## Evidence

| What | File |
|---|---|
| A manifest from the original code (lines run together) | `evidence/00_original_sale_manifest.csv` |
| Concurrent test **failing** on the original code: 25 of 25 repetitions | `evidence/01_concurrent_test_BEFORE.txt` |
| Concurrent tests **passing** on the fixed code: supplied test ×25, file-content test ×10 | `evidence/02_concurrent_tests_AFTER.txt` |
| Clean build from a fresh copy (no `build/` or `.gradle/`) | `evidence/03_clean_build.txt` |
| JaCoCo: **94.0%** line coverage of the main source set | `evidence/04_jacoco_summary.txt`, `evidence/jacoco-html/index.html` |
| Sale simulation output | `evidence/05_sale_simulation.txt` |
| Sample manifest and audit files | `evidence/sample-run/manifest.csv`, `evidence/sample-run/audit.csv` |

**Before, on the original code.** The supplied `ConcurrentSaleTest` was run with `@Disabled`
removed:

```text
25 tests completed, 25 failed
repetition 1 of 25: java.lang.AssertionError: Expected size: 60 but was: 67 ...
repetition 13 of 25: org.mockito.exceptions.verification.TooManyActualInvocations: manifestWriter.append(...) Wanted 60 times ...
```

That's up to 67 jackets sold from 60, and in some runs refused orders reached the manifest. The
original sale's manifest also has run-together lines such as
`order-71,JACKET,1,order-8order-72,JACKET,1,4999`.

**After.** The same test passes all 25 repetitions. `ConcurrentManifestFileTest` (10
repetitions) runs 200 shoppers through an executor against the real files and checks every
line:
- exactly 60 lines;
- each one matches `order-N,JACKET,1,4999`, with no duplicates;
- the IDs are exactly the accepted orders, and no refused order appears;
- the audit log has the same 60 reservations.

## What changed

| Problem | Fix |
|---|---|
| Oversold: stock was read, then saved, in separate steps | `Product.reserve` checks and takes stock in one `synchronized` step and returns `Optional<Order>`. `setStock` is gone. |
| Refused orders in the manifest | Only a successful reservation produces an `Order`, so only those are written. |
| Lines ran together | `AsyncFileWriter` writes each record as one complete line from a single writer thread. |
| Checkout waited for the file | `append` only queues the line. `close()` shuts the executor down, waits for every queued line, then closes the file. |
| Negative quantity added stock | Zero, negative, or unavailable quantities return `false` and change nothing. |
| Mistyped code threw NullPointerException | `findProduct` returns `Optional<Product>`. An unknown code makes checkout return `false`, with no exception. |
| Fixed threshold of 5 | The new constructor `Checkout(products, manifest, audit, lowStockThreshold)` sets it. The original two-argument constructor keeps 5. `lowStock()` is now a stream. The original loop is kept as `originalLowStockLoop()` for the comparison test. |
| Nothing logged | The audit log records `RESERVED` and `PRICE_CHANGED` lines, using the same `AsyncFileWriter` class as the manifest. |

Sample lines:

```text
manifest.csv   order-89,JACKET,1,4999
audit.csv      2026-11-18T19:24:53.545663Z,RESERVED,order-100,JACKET,1,4999
audit.csv      2026-11-18T19:24:53.549394Z,PRICE_CHANGED,JACKET,4999,5499
```

## How each exit criterion is met

| ID | Where |
|---|---|
| D1-1 Generics | `AsyncFileWriter<T>` takes a `Function<? super T, String>`. One `Function<CsvRecord, String>` (`CsvRecord::toCsvLine`) serves both `AsyncFileWriter<Order>` and `AsyncFileWriter<AuditEvent>`, which only compiles because of the wildcard. Tested in `AsyncFileWriterTest.oneWriterClassServesTwoRecordTypesWithOneSupertypeFormatter`. |
| D1-2 Streams and functions | `lowStock()` (stream) vs `originalLowStockLoop()`: same results across 50 random catalogues in one test. `productsMatching(Predicate<? super Product>)` is called with a lambda and with `Product::isSoldOut`. |
| D1-3 Optional | `findProduct` and `Product.reserve` return `Optional`. There are no `Optional` fields or parameters, and `orElseThrow()` is used only after `isEmpty()`. |
| D1-4 Concurrency | The writer runs on a single-thread `ExecutorService` that `close()` shuts down and awaits. The file test runs shoppers on an executor and joins every `Future`. The concurrent test fails before and passes after. |
| D1-5 Testing | Parameterized quantity boundaries (−1, 0, 1, 5 = stock, 6, `Integer.MAX_VALUE`) and unknown codes. Mockito `verify(...)` and `verify(..., never())` check calls made and calls never made. All new assertions use AssertJ. |
| D1-6 Coverage | JaCoCo 0.8.16: 94.0% line coverage (140/149). `./gradlew check` fails below 80%. |

## Rules followed

- **Supplied tests:** they are unchanged apart from removing `@Disabled`. The original
  `Checkout(products, manifest)` constructor and `ManifestWriter` still exist, so no supplied
  call had to change.
- **Production code:** it imports no test library.
- **Temporary files:** every test that writes files uses `@TempDir`.
- **Build file:** `build.gradle` adds the `jacoco` plugin, the report and coverage rule, and
  `testLogging`, so each test and any full failure message show in the console.
