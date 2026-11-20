---
module: JAVA-02
code: JAVA-02
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Fix the checkout that oversold the sale
subtitle: Make reservations and file output safe under concurrent checkout, and prove the behavior with tests.
time_budget_min: 90
status: draft
---

## Topic: Deliverable

### Exercise: Repair checkout before the next sale

A small store's checkout was built in a hurry. It works, and its unit tests pass. Checkout reads the stock count, subtracts the units, saves the new count, then appends the order to `manifest.csv`, the file the warehouse packs from.

During last week's sale, hundreds of shoppers checked out at once. The store had 60 of one jacket and sold 63. Manifest lines ran together, refused orders were listed, and every checkout waited for the file write.

There are smaller problems too. A negative quantity adds stock back. A mistyped product code makes checkout throw a NullPointerException. The low-stock threshold is fixed at 5, and nothing is logged.

The store wants checkout to stay correct however many shoppers check out at once. Each successful order should get one complete manifest line, without shoppers waiting for the write. The owner wants an audit log of reservations and price changes, written by the same code as the manifest. The store also wants to choose its low-stock threshold.

:::download ./assets/java02_d01_starter.zip Sale checkout starter
A Gradle project with its wrapper, checkout code, passing unit tests, a disabled concurrent test, and a sale simulation.
:::

:::note
**Time limit:** 90 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

- When 200 shoppers reserve the last 60 jackets together, exactly 60 succeed.
- Every successful order appears exactly once in the manifest, on its own complete line. Refused orders never appear there.
- Checkout returns without waiting for the manifest write. Every pending line reaches its file before the program exits.
- A second file records reservations and price changes, written by the same writer code as the manifest.
- Zero, negative, or unavailable quantities are refused without changing stock.
- Looking up an unknown product returns an empty `Optional`, never `null`.
- Checking out a product code the store does not sell is refused like any other failed reservation. No exception reaches the caller.
- The store can choose the low-stock threshold. With threshold 5, the result matches the original list, including product order.

### Theory: Rules

- Use Java 25, Gradle 9, JUnit 5, Mockito 5, AssertJ 3, and JaCoCo 0.8.
- Production code does not depend on test libraries. Shut down executors or join futures.
- Tests that write files must use a temporary directory.
- Keep what the supplied tests check.
- If a new type changes how a supplied test calls the code, update that call.
- Names, supporting types, generic bounds, function types, and concurrency design are yours.
- Databases, a real warehouse system, back orders, reservation expiry, and file formats beyond one line per record are out of scope.

### Theory: Evidence

Show the concurrent test failing on the original code and passing on yours. Check the manifest's contents, not only the counts.

Your tests must show that checkout returns before its file write finishes, and that shutdown leaves nothing unwritten.

Keep the original low-stock loop so a test can compare it with your stream at threshold 5.

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Write generic code with bounded type parameters | Generic code with a bound or wildcard it relies on is tested with two different type arguments |
| D1-2 | Replace loops with streams and pass behavior as functions | A loop and its stream replacement return the same results under one test, and a method taking a java.util.function type is called with a lambda and a method reference |
| D1-3 | Model absent values with Optional | Lookups that may find nothing return Optional, with no unchecked get(), Optional field, or Optional parameter |
| D1-4 | Run work concurrently and prove it thread-safe | Work runs through an executor or futures that are shut down or joined, and a concurrent test passes on the fixed code and fails on the original |
| D1-5 | Test with parameterized tests, Mockito, and AssertJ | A parameterized test covers boundary and invalid inputs, Mockito verify checks calls made and calls never made, and assertions use AssertJ |
| D1-6 | Measure test coverage with JaCoCo | The JaCoCo report shows at least 80% line coverage of the main source set |

### Exercise: What to submit

- The Gradle project with your fixes, extensions, tests, and coverage configuration.
- Before-and-after concurrent test output, the JaCoCo report, and sample manifest and audit files.
