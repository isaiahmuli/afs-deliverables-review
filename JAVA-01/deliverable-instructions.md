---
module: JAVA-01
code: JAVA-01
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Untangle the shopping basket
subtitle: Preserve checkout behavior while making the basket safe to use and easier to extend.
time_budget_min: 90
status: draft
---

## Topic: Deliverable

### Exercise: Restructure and extend the shopping basket

A small online store has a shopping basket that adds up the price of everything in it at checkout. Today a shopper chooses standard delivery ($5) or express ($12). The code works and its tests pass, but it is hard to change.

To apply a discount, the basket checks which kind of discount it has, one kind after another. Any other class can change the basket's items directly, because its fields are public. If a shopper adds the same shirt twice, the basket shows two shirt lines instead of one line with quantity 2.

The store now wants a new discount and a pickup option for delivery. Staff also type product codes inconsistently, so `ab-12` and `AB-12 ` show up as two lines. Restructure the basket's code, then add the new discount and pickup.

:::download ./assets/java01_d01_starter.zip Shopping basket starter
A Gradle project with its wrapper, working basket code, passing JUnit tests, and a sample receipt.
:::

:::note
**Time limit:** 90 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

- The existing tests still pass after restructuring, and checkout totals for the existing cases remain unchanged.
- A further discount can be added as one new class without changing the basket or the existing discounts.
- Add one new discount. Its business rule is your choice; describe its behavior with examples and tests.
- Pickup is available as a delivery option. State and test its charge.
- The compiler rejects delivery pricing that omits a permitted delivery option. Adding pickup must expose every missing case before the code can compile.
- Adding the same shirt twice produces one line with quantity 2. Lines remain in the order the shopper first added each product.
- Product codes `ab-12` and `AB-12 ` identify the same product. Adding both produces one line with quantity 2.
- An unknown product code produces a recoverable error that the caller must handle. Non-positive quantities and negative prices are rejected immediately.
- Callers reading basket lines cannot add, remove, or change them, including a line's quantity.

### Theory: Rules

- Use Java 25, Gradle 9, and JUnit 5. The starter pins its build tool and Java toolchain.
- Store money as whole cents in a `long`. Percentage discounts round to the nearest cent, half up.
- Keep the supplied regression tests passing with their expected amounts.
- If a new type changes how a test calls the basket, update that call.
- Add tests for the required behavior and failure paths.
- Names, class structure, supporting types, and collection choices are yours. Keep collections suited to the operations they serve.
- Tax, several discounts on one basket, persistence, a user interface, and concurrency are out of scope.

### Theory: Evidence

The README lists commands and the starter receipt. `./gradlew run` should show the shirt as one line with quantity 2 after your changes.

For that sample, the shirt line becomes $50.00 and checkout stays $66.20. Keep the other existing checkout calculations covered by the supplied tests.

Include compiler output showing that an unhandled delivery case prevents compilation. Your completed version must build successfully.

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Encapsulate state behind a validating API | State is private, and each rejected input has an assertThrows test |
| D1-2 | Use inheritance and polymorphism without type checks | A supertype has two overriding implementations called through it, and no instanceof chain selects behavior |
| D1-3 | Apply modern Java: records, sealed types, pattern-matching switch | A record serves as a value type, and a pattern switch over a sealed type compiles with no default branch |
| D1-4 | Choose collections by their semantics | Each collection matches its role: ordering, uniqueness, keyed lookup, or FIFO processing |
| D1-5 | Honor the equals/hashCode contract | A non-record class overrides equals and hashCode on the same fields, and a test shows equal instances act as one set element or map key |
| D1-6 | Separate checked from unchecked exceptions and prove behavior with JUnit 5 | A checked exception covers a recoverable failure and an unchecked one a caller error, both tested, and the build passes on a clean clone |

### Exercise: What to submit

- The Gradle project with your source changes, supplied regression tests, and added JUnit tests.
- Output from a clean build and the sample receipt.
- The compiler evidence for the missing delivery case, and the new discount's rule and examples.
