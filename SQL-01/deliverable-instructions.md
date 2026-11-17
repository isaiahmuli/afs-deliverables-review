---
module: SQL-01
code: SQL-01
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Design the database for a boutique's order system
subtitle: Model products and orders, enforce data rules, and prove stock changes are atomic.
time_budget_min: 80
status: draft
---

## Topic: Deliverable

### Exercise: Design a boutique's order database

A small clothing boutique sells products in several sizes and colours. Registered customers place orders, and the shop must never sell more units than it has.

Its order system needs reliable records of customers, product options, prices paid, and stock. A failed order must not leave stock reduced or order items partly recorded.

Design the PostgreSQL schema as SQL scripts and a diagram of its relations. Supply data and query results that demonstrate the design works.

:::note
**Time limit:** 80 minutes. No starter files are supplied.
:::

## Topic: Outcomes

### Theory: The finished work

- An entity-relationship diagram shows every table, its keys, and the links between tables.
- Each product, size and colour option, customer, and order is stored once. The schema is in third normal form, without repeating groups or non-key dependencies.
- Each order belongs to one customer and contains one or more items. Each item identifies a specific size and colour of a product.
- An order retains the price the customer paid when the product's current price changes.
- The database rejects negative stock, quantities or prices of zero or less, and duplicate customer emails.
- Join queries show a customer's orders with product, size, and colour. Another query lists product options low on stock.
- An order transaction records its items and reduces stock together. An order that would oversell leaves no order, items, or stock changes behind.

### Theory: Rules

- Use PostgreSQL 18 and `psql` 18. Run every SQL file with `ON_ERROR_STOP` enabled.
- Money uses `numeric(19,2)`, never a floating-point type.
- Submit SQL scripts and a diagram. Table names, file layout, and design choices are yours.
- Payments, shipping, returns, totals and reports, indexes and query plans, and isolation levels are out of scope.

### Theory: Evidence

Include seed data and the expected rows for your queries, so a reviewer can compare actual results with your expectations.

Show a successful order and an oversell attempt. Capture the stock and order records that prove the successful change is complete and the failed change leaves nothing behind.

Use two database sessions to show transaction visibility before and after commit, and after rollback. Label each session's commands and results.

Include the expected errors from constraint checks. Keep `ON_ERROR_STOP` enabled for these checks too; separate expected failures so each can be observed.

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Model a domain as a schema in third normal form | The schema builds on an empty database with no repeating groups or non-key dependencies |
| D1-2 | Enforce relationships with primary and foreign keys | The diagram matches the schema: every table has a primary key, and every relationship is a foreign key with a chosen ON DELETE action |
| D1-3 | Encode business rules as constraints | A violation script shows named NOT NULL, UNIQUE, CHECK, and foreign key constraints each rejecting a bad row |
| D1-4 | Answer questions with DML and joins | Join queries return the expected rows for the seed data |
| D1-5 | Group dependent writes in one transaction | A second session sees a multi-table write only after its commit |
| D1-6 | Demonstrate atomicity with a rollback | A transaction that fails part-way leaves no partial write visible |

### Exercise: What to submit

- SQL scripts for the schema, seed data, queries, constraint violations, and transaction demonstrations.
- The entity-relationship diagram matching your schema.
- Expected query rows and captured results, including constraint errors and the two-session transaction evidence.
