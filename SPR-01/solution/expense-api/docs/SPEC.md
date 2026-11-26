# Expense submission API: spec

Written before coding. The code and tests are checked against this file.

## Purpose

Record valid expense claims, refuse invalid ones with every reason at once, and apply the
running team's maximum claim amount. Expenses are kept in memory only.

## Data model

`Expense` (stored, immutable):

| Field | Type | Meaning |
|---|---|---|
| `id` | long | Assigned by the API on save: 1, 2, 3, ... |
| `employee` | text | Who incurred the expense |
| `description` | text | What it was for |
| `expenseDate` | date (`YYYY-MM-DD`) | When it was incurred |
| `receiptTotal` | US dollars | The receipt's total |
| `claimAmount` | US dollars | How much of the receipt is claimed |
| `submittedAt` | timestamp (UTC) | When the API saved it |

The receipt itself is not stored; only its total is.

## API contract

Base path: `/api/expenses`. JSON in and out. Errors use RFC 9457 problem details
(`application/problem+json`).

| Request | Success | Failures |
|---|---|---|
| `POST /api/expenses` with a submission body | `201 Created`, `Location: /api/expenses/{id}`, the saved expense | `400` validation, `400` malformed JSON, `422` over the team limit |
| `GET /api/expenses` | `200`, all expenses in id order (`[]` when none) | — |
| `GET /api/expenses/{id}` | `200`, the expense | `404` unknown id, `400` id not a number |

Submission body:

```json
{
  "employee": "Amina Otieno",
  "description": "Fuel for client visit to Nakuru",
  "expenseDate": "2026-11-18",
  "receiptTotal": 120.00,
  "claimAmount": 50.00
}
```

Validation failure (`400`) lists **every** failing field:

```json
{
  "type": "about:blank",
  "title": "Invalid expense submission",
  "status": 400,
  "detail": "2 fields need correcting.",
  "instance": "/api/expenses",
  "errors": [
    { "field": "claimAmount", "message": "must not be more than the receipt total" },
    { "field": "employee", "message": "must not be blank" }
  ]
}
```

Over the team limit (`422`):

```json
{
  "title": "Claim exceeds team limit",
  "status": 422,
  "detail": "The claim of 150.00 USD exceeds this team's limit of 100.00 USD per claim.",
  "claimAmount": 150.00,
  "maxClaimAmount": 100.00
}
```

## Validation rules

| Field | Rule | Message |
|---|---|---|
| `employee` | required, not blank, at most 100 characters | `must not be blank` / `size must be between 0 and 100` |
| `description` | required, not blank, at most 500 characters | as above |
| `expenseDate` | required, a valid date, not in the future | `must not be null` / `must be a date in the past or in the present` |
| `receiptTotal` | required, greater than 0, at most 2 decimal places (and 7 whole digits) | `must be greater than 0` / `numeric value out of bounds (<7 digits>.<2 digits> expected)` |
| `claimAmount` | same as `receiptTotal` | same |
| `claimAmount` vs `receiptTotal` | **custom constraint** `@ClaimWithinReceipt`: claim ≤ receipt total; reported on `claimAmount` | `must not be more than the receipt total` |
| `claimAmount` vs team limit | claim ≤ `expenses.max-claim-amount`; checked by the service after validation | `422` with the reason |

- A value of the wrong type (for example `"abc"` for an amount, or `"2026-13-40"` for a date)
  stops JSON parsing, so the response names that one field only: `400`, `errors` has that field
  with "is not a valid ...".
- Nothing is saved when any rule fails.

## Configuration

`expenses.max-claim-amount` binds to `ExpenseLimitsProperties` through
`@ConfigurationProperties`, validated (required, greater than 0).

| Source | Value |
|---|---|
| `application.properties` (base) | `500.00` |
| `application-team-b.properties` (profile `team-b`) | `100.00` |

## Operations

- `GET /actuator/health` → `200 {"status":"UP"}`.
- `GET /actuator/metrics/http.server.requests` → request count and total time, after traffic.
- No other Actuator endpoint is exposed over HTTP (`/actuator/env`, `/actuator/configprops`,
  `/actuator/beans` → `404`).

## Layers

`web` (controller, request/response records, error handler) → `service` (rules, limit) →
`repository` (in-memory store). Constructor injection only. The controller has no rules or
storage; the service has no HTTP types.

## Acceptance checks

| # | Check | Expected |
|---|---|---|
| A1 | Submit a valid $50 claim on a $120 receipt | `201`, saved with an id; `GET` by id returns it |
| A2 | List expenses | `200`, every saved expense |
| A3 | `GET` an unknown id | `404` |
| A4 | Submit `{}` | `400` naming all five fields; nothing saved |
| A5 | Claim $80 on a $50 receipt | `400` on `claimAmount` saying it exceeds the receipt total; nothing saved |
| A6 | Amount 10.555, or 0, or -5 | `400` on that field |
| A7 | $150 claim on $150 receipt, base config (limit 500) | `201` |
| A8 | Same, profile `team-b` (limit 100) | `422` with the reason; nothing saved |
| A9 | Health | `UP` |
| A10 | Metrics after API traffic | `http.server.requests` count > 0, filterable by `uri=/api/expenses` |
| A11 | `/actuator/env`, `/actuator/configprops`, `/actuator/beans` | `404` |
