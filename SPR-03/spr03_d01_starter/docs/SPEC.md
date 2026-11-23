# Invoice and payment services: spec

Written with AI before coding. The code, the JUnit tests and the Postman collection are checked
against this file.

## Goal

A small business creates invoices and gets them paid online. Two services, each owning its own
data:

- **invoices** (port 8081, database `invoices`): the invoice and its status.
- **payments** (port 8082, database `payments`): payment links and the payment reports received
  from the provider.

The payments service calls the invoices service over HTTP; the invoices service never calls
payments. Neither reads the other's database (the logins can't).

## Data each service owns

| Service | Table | Columns | Notes |
|---|---|---|---|
| invoices | `invoice` (supplied) | id (from 1001), customer, description, total `numeric(12,2)`, status, created_at | number shown as `INV-<id>` |
| invoices | `idempotency_record` (mine, V2) | idempotency_key (PK), request_hash, response_status, response_body, location, created_at | one row per create made with an `Idempotency-Key` |
| payments | `payment_link` (supplied) | id, invoice_id (unique), checkout_id (unique), url, created_at | one link per invoice |
| payments | `payment_report` (mine, V2) | event_id (PK), request_hash, checkout_id, invoice_id, response_body, received_at | one row per report that was applied |

## Operations

All paths carry the API version: `/api/v1/...`. Money is USD; `total` is a JSON number with at
most two decimals.

### invoices

| Operation | Request | Success | Refusals |
|---|---|---|---|
| Create | `POST /api/v1/invoices`, body `{customer, description, total}`, optional `Idempotency-Key` header | `201`, `Location: /api/v1/invoices/{id}`, the invoice (status `DRAFT`) | `400` invalid, `422` key reused with different content |
| List | `GET /api/v1/invoices` | `200`, invoices by id | — |
| View | `GET /api/v1/invoices/{id}` | `200`, the invoice | `404` |
| Change status | `PUT /api/v1/invoices/{id}/status`, body `{status}` | `200`, the invoice | `400` unknown status, `404`, `409` forbidden change |
| Delete | `DELETE /api/v1/invoices/{id}` | `204` | `404` (including a second delete), `409` not a draft |

Invoice body: `{id, number, customer, description, total, currency, status, createdAt}`.

### payments

| Operation | Request | Success | Refusals |
|---|---|---|---|
| Get a payment link | `POST /api/v1/payment-links`, body `{invoiceId}` | `201` + `Location` the first time, `200` with the same link after that | `400`, `404` unknown invoice, `409` invoice not sent, `502`/`504` invoices or provider failed |
| View a payment link | `GET /api/v1/payment-links/{invoiceId}` | `200` | `404` |
| Payment report (provider) | `POST /api/v1/payment-reports`, body `{eventId, checkoutId, status, amountCents}` | `200` `{eventId, invoiceId, invoiceNumber, outcome: "APPLIED"}` | `400`, `409` invoice cancelled, `422` cannot apply, `502`/`504` invoices failed |

Payment link body: `{invoiceId, invoiceNumber, checkoutId, url, createdAt}`.

## Business rules

1. A new invoice is `DRAFT`. Customer (≤ 200 chars) and description (≤ 2000) are required and not
   blank; total is required, above zero, with at most two decimals.
2. Status changes: `DRAFT → SENT`, `SENT → PAID`, `DRAFT → CANCELLED`, `SENT → CANCELLED`.
   Setting the status an invoice already has succeeds and changes nothing. Anything else is `409`
   (for example cancelling a paid invoice, or `DRAFT → PAID`).
3. Only a draft can be deleted. Deleting it again is `404`: it no longer exists, and nothing
   changes.
4. **Create with an `Idempotency-Key`:** the first request creates the invoice and stores the
   response. The same key with the same content (customer, description, total compared as a
   number) returns the stored `201` response and creates nothing. The same key with different
   content is `422`. A request without a key, or with a new key, is always processed normally,
   even if its details match an earlier invoice.
5. A payment link is only for a `SENT` invoice. Asking again for a sent invoice returns the same
   link (`200`); the provider is called only once per invoice. Draft, cancelled and paid
   invoices are `409`; unknown invoices `404`.
6. The provider checkout uses `reference = INV-<id>` and `amountCents = total × 100`.
7. **Payment report:** applied when the checkout is known, `status` is `paid`, and `amountCents`
   equals the invoice total in cents (full payment only). Applying it sets the invoice to `PAID`
   through the invoices API. The `eventId` is stored with the response; the same report again
   returns the stored response and calls nothing. The same `eventId` with different content is
   `422`. A report that can't be applied (unknown checkout, other status, wrong amount, invoice
   gone) is `422` and is not stored. A report for a cancelled invoice is `409`.

## Calls between services

- payments → invoices: `GET /api/v1/invoices/{id}` and `PUT /api/v1/invoices/{id}/status`.
- payments → provider: `POST /checkouts`.
- Both through `@HttpExchange` interfaces on `RestClient`, with base URL, connect timeout and read
  timeout (3 s) per environment in `application.yaml` (profiles for other environments).
- If a call doesn't answer within 3 seconds, payments returns `504` with its own problem body. If
  it answers with an unexpected error, or can't be reached, payments returns `502`. A `404` or
  `409` from invoices becomes payments' own `404`/`409`/`422` as the operation requires.

## Failure behavior

- Every refusal from either service is `application/problem+json` with `type`, `title`, `status`
  and `detail`; validation problems add `errors: [{field, message}]`. No stack traces, SQL, class
  names or downstream bodies are returned.
- Problem types: `https://billing.example.com/problems/<name>` with names `invalid-request`,
  `not-found`, `invalid-state`, `idempotency-key-reused`, `cannot-apply`, `downstream-failed`,
  `downstream-timeout`, `cors-rejected`, `internal-error`.

## CORS

Allowed origin per environment: `https://billing.example.com`. Methods `GET, POST, PUT, DELETE`;
request headers `Content-Type, Idempotency-Key`; exposed header `Location`. A preflight from any
other origin is `403` (problem+json).

## OpenAPI

Both services serve an OpenAPI **3.1** document at `/v3/api-docs` (springdoc). The invoices tests
validate real responses (invoice, list and problem bodies) against its schemas.

## Acceptance checks

| # | Outcome | Check | Where |
|---|---|---|---|
| A1 | Create | Harbor Bakery, "40 hours of bookkeeping, October", 1200.00 → `201`, `Location`, `DRAFT`, `INV-<id>` | Postman, JUnit |
| A2 | View | get it → `200`; unknown id → `404` problem | Postman, JUnit |
| A3 | Invalid | missing customer, total `0`, total `12.345`, malformed JSON → `400` problem naming the fields | Postman, JUnit |
| A4 | Status | send → `SENT`; send again → `200`, unchanged; cancel paid → `409`; draft → paid `409` | Postman, JUnit |
| A5 | Cancel | cancel a draft and a sent invoice → `CANCELLED` | Postman, JUnit |
| A6 | Delete | delete a draft → `204`; again → `404`; delete a sent invoice → `409` | Postman, JUnit |
| A7 | Idempotent create | same key + same body → same `201` and id, no new invoice; same key + other body → `422`; no key twice → two invoices | Postman, JUnit |
| A8 | Payment link | sent invoice → `201` link; again → `200` same link, one provider call; draft / cancelled / paid → `409`; unknown → `404` | Postman, JUnit |
| A9 | Paid | report for the link's checkout and full amount → `200`; invoice shows `PAID`; same report again → same `200`, no new call | Postman, JUnit |
| A10 | Report refused | wrong amount, unknown checkout → `422`; same eventId, other content → `422` | Postman, JUnit |
| A11 | Downstream | invoices answers → link created; invoices `500` → `502`; invoices slower than 3 s → `504` in about 3 s; provider `500` → `502` | JUnit (WireMock) |
| A12 | Error format | every refusal above is problem+json with type, title, status, detail | Postman, JUnit |
| A13 | OpenAPI | both services serve OpenAPI 3.1; invoices responses match their schemas | Postman, JUnit |
| A14 | CORS | preflight from `https://billing.example.com` → allowed methods and headers; from another origin → `403` | Postman, JUnit |
