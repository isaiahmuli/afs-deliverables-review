---
module: SPR-02
code: SPR-02
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Build a small office stock tracker
subtitle: Keep reliable stock counts and histories in PostgreSQL, even when two staff change the same supply at once.
time_budget_min: 90
status: draft
---

## Topic: Deliverable

### Exercise: Build the office stock tracker

A small office needs to track supplies such as pens, staples, and notebooks. Several staff members handle supplies. The office needs reliable records of what is available and why quantities changed.

Today the counts live in a shared spreadsheet. When two people update the same count at once, one change overwrites the other. Nobody can explain why the shelf doesn't match.

Build a stock tracker that lets staff record supplies received or used, and review stock and its history. Receiving 20 pens raises their stock from 10 to 30; taking 5 then leaves 25. Both changes appear in the pens' history with their reasons.

Products can belong to several categories to help staff find what they need. A notebook belongs to both Stationery and Meeting Supplies, and each category holds other products too.

| Product | Available quantity | Categories |
|---|---|---|
| Ballpoint pens | 25 | Stationery |
| Staples | 12 | Stationery |
| Notebooks | 25 | Stationery, Meeting Supplies |
| Painter's tape | 8 | Maintenance |

This table is example business data. It is not a table design or a seed script.

No starter is supplied. You generate the project yourself.

:::note
**Time limit:** 90 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

- Staff can find products by part of their name, including names such as Painter's tape; searches return only matching products, or an empty list if none match.
- Staff can record supplies received or used, with a reason; the stock count and adjustment history reflect the change, and an adjustment for an unknown product is reported as not found.
- An adjustment that would make stock negative is refused with an explanation and leaves stock and history unchanged.
- When two staff change the same supply at the same time, neither change is silently lost.
- Staff can assign a product to several categories and find all products belonging to a selected category.
- Staff can retrieve a stock report of products with their quantities and categories efficiently as the catalog grows.

### Theory: Rules

- Java 25, Spring Boot 4, Hibernate 7, HikariCP 7, Flyway 12, PostgreSQL 18, Testcontainers 2, JUnit 6.
- Before coding, write a short spec: goal, operations with inputs and outputs, data and relationships, business rules, failure behavior, and acceptance checks for every outcome.
- Stock is counted in whole units; each adjustment must add or remove a non-zero quantity.
- Persistence tests use PostgreSQL 18 through Testcontainers.
- Database changes must be incremental, preserving existing records.
- Build the domain logic, services, repositories, and data model; demonstrate them through automated tests, with no presentation layer, API controllers, or authentication.

Testcontainers is a library that starts a real database in Docker for the tests, so Docker must be running.

### Theory: Your decisions

Type names, operation signatures, the schema, and test fixtures are yours. So is how you handle two staff changing the same supply. Base each choice on your spec.

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Map entities, relationships, and repositories | Mappings cover one-to-many and join-table many-to-many relationships; repositories include derived and @Query methods; @DataJpaTest tests on PostgreSQL containers persist and reload each relationship |
| D1-2 | Own the schema with Flyway migrations | Versioned Flyway migrations create the schema; Hibernate only validates it, and a test applies all migrations to an empty PostgreSQL database |
| D1-3 | Apply @Transactional correctly and show one of its pitfalls | Tests prove a failed operation rolls back every write and demonstrate a checked-exception or self-invocation pitfall before and after its fix |
| D1-4 | Prevent lost updates with optimistic locking | An entity uses @Version; a stale-update test proves the conflict is rejected and the successful update is preserved |
| D1-5 | Detect and fix the N+1 problem in JPA | Lazy associations and SQL-count tests show N+1 before the fix and no extra query per entity after |
| D1-6 | Prevent SQL injection in persistence code | Queries take user input through derived methods, bind parameters, or Criteria, never string building, and a test shows a payload matching literally |

### Exercise: What to submit

- The project, with its build files, source, migrations, and tests.
- Your spec.
- Passing test output. It includes a rollback after a write has happened, and SQL counts at two or more catalog sizes.
