# Payment provider API (local stand-in)

The payment provider is not yours to change. A WireMock container plays it at
`http://localhost:8089`. This contract is fixed; your own services' APIs are your design.

## Create a checkout

`POST /checkouts` with a JSON body:

| Field | Type | Example | Meaning |
|---|---|---|---|
| `reference` | string | `"INV-1001"` | Your identifier for what is being paid |
| `amountCents` | integer | `120000` | Amount in cents; $1,200.00 is `120000` |
| `currency` | string | `"USD"` | Always `USD` here |

Responses:

- `201 Created` with `{"checkoutId": "chk-INV-1001", "url": "https://pay.example.com/chk-INV-1001"}`.
  The checkout id is `chk-` plus your reference, so each reference gets its own checkout.
- `400 Bad Request` with `{"error": "..."}` when a field is missing.

The payment URL is for the customer. Nothing is served there; no real money moves.

## Payment report

When a customer pays, the provider sends a payment report to your payments service, to an
endpoint you design. In this exercise you send it yourself from Postman.
`sample-payment-report.json` holds an example body:

| Field | Type | Meaning |
|---|---|---|
| `eventId` | string | Unique id of this report |
| `checkoutId` | string | The checkout that was paid |
| `status` | string | Always `paid` here |
| `amountCents` | integer | Amount paid, in cents |

A real provider resends a report when it does not get a success answer, so the same report
can arrive more than once. Reports are JSON, with no authentication and no signature.
