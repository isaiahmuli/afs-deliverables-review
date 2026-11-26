---
module: SPR-03
code: SPR-03
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Build invoice and payment services
subtitle: Build two services that bill customers and take payment online, each owning its own data.
time_budget_min: 90
status: draft
---

## Topic: Deliverable

### Exercise: Build the invoice and payment services

A small business sends invoices to its customers and wants them paid online.

Build two services. The invoices service lets the business owner create, view, send, cancel, and delete invoices. The payments service takes a sent invoice and returns a link where the customer can pay. When the payment provider reports that the customer has paid, the payments service notifies the invoices service about the paid invoice.

For example, the business owner creates invoice INV-1001 billing Harbor Bakery $1,200, then marks it as sent and requests a payment link. The bakery pays, the provider reports the payment, and INV-1001 shows as paid.

Each service keeps its own data in its own database. A local stand-in plays the payment provider.

| Invoice | Customer | Description | Amount (USD) |
|---|---|---|---|
| INV-1001 | Harbor Bakery | 40 hours of bookkeeping, October | 1,200.00 |
| INV-1002 | Northside Florist | Quarterly payroll filing | 340.00 |

Example business data, not seed data.

The starter has both Spring Boot projects with their database settings, tables, entities, and repositories. It also has a Compose file for PostgreSQL and the provider stand-in, Postman files, and an OpenAPI test helper. The README explains how to start everything; Docker must be running. The services have no endpoints yet: their APIs, rules, error handling, calls, and tests are yours.

:::download ./assets/spr03_d01_starter.zip Invoice and payment services starter
Two Spring Boot projects, a Compose file, the provider's API description, Postman files, and a README.
:::

:::note
**Time limit:** 90 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

- The business owner can create, view, send, and cancel invoices, and delete drafts.
- An invoice records a customer, a description of what is billed, and a USD total above zero with at most two decimal places.
- Each invoice shows its status; it goes from draft to sent to paid, and can be cancelled while draft or sent.
- An incomplete or invalid invoice is refused with an explanation, and an unknown invoice is reported as not found.
- Deleting an invoice that is not a draft, or cancelling a paid one, is refused with an explanation.
- The business owner gets a payment link for a sent invoice; asking again returns the same link; draft, cancelled, paid, or unknown invoices are refused.
- A customer pays an invoice's full amount in one payment, and when the provider reports it, the invoice shows as paid.
- The business owner can manage invoices and payment links from a billing web app at https://billing.example.com; pages on other websites cannot use the APIs.

### Theory: Rules

- Java 25, Spring Boot 4, springdoc-openapi 3, OpenAPI 3.1, PostgreSQL 18, WireMock 3, JUnit 6.
- Before coding, write a spec with AI covering operations, data each service owns, business rules, failure behavior, and acceptance checks for every outcome.
- Both services use one PostgreSQL 18 server, each with its own database and login.
- Schema and data access for invoices and payment links are supplied; idempotency storage is the learner's design.
- Acceptance checks run as a Postman collection against the running services; OpenAPI validation and downstream-failure tests run in JUnit.
- Setting an invoice to the status it already has succeeds and changes nothing.
- A request replayed or sent twice by mistake has no additional effect; a new request with identical details is processed normally.
- When one service calls another that fails or does not answer within 3 seconds, the caller returns its own error instead of waiting.
- Both services return every refusal in one error format with a clear reason and no internal details.
- Both services serve OpenAPI 3.1 documents; tests must check the invoices service's actual responses against its document; checking the payments service is optional.
- The billing web app is not built and is not part of this work.
- Out of scope: user interface, authentication, invoice line items, emailing invoices, payment report signatures, partial payments, refunds, a real payment provider, and an API gateway.

CORS is the browser rule for which websites may call an API. Without a gateway, each service sets its own; behind one, it is set once.

WireMock appears twice. In Compose it plays the payment provider. In your JUnit tests it stands in for the invoices service.

The README shows how to export Postman's Collection Runner results.

### Theory: Your decisions

Paths, request and response shapes, type names, and idempotency storage are yours. So are how payments answers a report it cannot apply and whether it checks the amount. Base each choice on your spec.

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Model versioned resources with correct methods and status codes | Tests assert 201 with Location on create, 204 on delete, 404 for a missing resource, and 409 for a forbidden state change; the API version is carried in the path |
| D1-2 | Return a consistent problem+json error contract | Both services return application/problem+json with type, title, status, and detail, and tests assert the body for each failure mode |
| D1-3 | Make writes idempotent | Repeated status changes and deletes have no further effect; replaying a create with the same Idempotency-Key returns the original response, the same key with different content is refused, and nothing new is created, under test |
| D1-4 | Publish an OpenAPI 3.1 description and validate against it | springdoc serves an OpenAPI 3.1 document for each service, and tests validate at least one service's real responses against its schemas |
| D1-5 | Split services by data ownership and call across them declaratively | Each service owns its database; one calls the other through an @HttpExchange client on RestClient, with base URL and timeouts in configuration per environment |
| D1-6 | Test a downstream dependency with WireMock | WireMock tests cover a downstream success, an error status, and a timeout, and assert the caller's own response for each |
| D1-7 | Allow browser calls only from a listed origin | Both services answer a preflight from the listed origin with the methods and headers it needs, and tests show a preflight from an unlisted origin is rejected |

### Exercise: What to submit

- Both projects, with their source and tests.
- Your spec, written with AI.
- The Postman collection and its exported Collection Runner results.
- JUnit test output from both services.
