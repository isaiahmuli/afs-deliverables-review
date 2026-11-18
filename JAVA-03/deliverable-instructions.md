---
module: JAVA-03
code: JAVA-03
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Fix the transfer fee batch, then make room for bill payments
subtitle: Diagnose failed transfers, preserve working fees, and separate rules from batch processing.
time_budget_min: 90
status: draft
---

## Topic: Deliverable

### Exercise: Repair and separate the transfer fee batch

A money transfer app prices about 40,000 transfers each night. One class reads the file, picks fees, refuses types it does not offer such as `intl`, sends text receipts, flags large transfers, and writes results. Four batches run in parallel because each receipt waits for the SMS service.

Since the bank added Mexican pesos, transfers to Mexico fail with `IllegalStateException`. Logs show the bank's rates fetched more than once a night, and the bank charges for every fetch.

Fix these failures and add bill payments. Separate the fee rules so a future website can reuse them. Support also needs to switch receipts off when the SMS service is down.

:::download ./assets/java03_d01_starter.zip Transfer fee batch starter
A Gradle project with its wrapper, the existing batch, 200 sample transfers, bank rates, and the original fee output.
:::

:::note
**Time limit:** 90 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

| Transfer | Fee in US dollars |
|---|---|
| To a friend in the app | Free |
| To a bank account | $0.50 |
| Bill payment | $0.25 |
| International | 2%, with a $3 minimum: $100 costs $3; $500 costs $10 |

- A $200 transfer to Mexico costs $4. The recipient receives the full $200 converted to pesos at that night's rate.
- Unsupported types are refused with their row ID and type. The rest of the batch still runs.
- The bank's rates are fetched once per night, even with four batches running in parallel.
- Each priced transfer sends its sender a receipt showing the amount, fee, and what the recipient receives.
- Transfers above $5,000 are flagged for review.
- Support can turn receipt delivery off to prevent texts. Pricing and review flags still work.

### Theory: Rules

- Use Java 25, Gradle 9, JUnit 5, ArchUnit 1, SLF4J 2, and Logback 1.5.
- Use `BigDecimal` for money. Round half up to cents.
- Application code must log through SLF4J with placeholders, never `System.out`.
- Fee rules must not depend on batch, file, or receipt code. The website team will reuse them.
- Add a new transfer type without editing existing fee rules.
- Keep a root-cause note for the Mexico failure and an ADR, an Architecture Decision Record, next to the code.
- Databases, the website, real bank downloads, refunds, and currencies absent from the rates file are out of scope.

### Theory: Working with the starter

The README explains commands, file columns, and the simulation. A fresh process represents one night's batch.

The supplied `fees.csv` records current output, including missing Mexico rows. It is evidence for comparison, not the target result. New runs write to `build/output/fees.csv`.

There are no starter tests. Pin working results before restructuring, and retain that evidence as the design changes. For Mexico, retain the reproducing test's failure and its result after the fix.

Names, packages, interfaces, and the receipt-off control are yours.

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Keep the design SOLID | Classes have one job and take interface dependencies by constructor; a test pinning earlier results passes before and after each refactor |
| D1-2 | Implement creational patterns, including a thread-safe Singleton | A Factory returns the right object and rejects invalid input under test; a thread-safe Singleton passes a concurrent test asserting one instance |
| D1-3 | Implement behavioral patterns through composition | A context holds strategies by composition and gains one with no change; tests show subscribed observers notified and unsubscribed ones not |
| D1-4 | Decompose into modules with one-way dependencies | An ArchUnit test asserts packages are free of cycles and the core package depends on no other project package |
| D1-5 | Record a design decision as an ADR | An ADR gives status, context, options considered, decision, and consequences, including when the chosen pattern would be wrong |
| D1-6 | Root-cause a failure in code they did not write | A root-cause note cites the first project stack frame, the cause, and the defect line; a reproducing test passes after the fix |

### Exercise: What to submit

- The Gradle project with source, tests, and sample output from the repaired batch.
- Test output showing preserved results, the reproduced failure and fix, and the evidence named in the criteria.
- The root-cause note and ADR next to the code.
