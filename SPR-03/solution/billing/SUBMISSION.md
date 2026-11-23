# Invoice and payment services: submission notes

Built on the supplied starter. Versions as resolved: Spring Boot 4.1.2 (Spring Framework 7.0.10),
Java 25, Gradle 9.9.1, springdoc-openapi 3.2.0 (OpenAPI 3.1.0 documents), Hibernate 7.4.13,
Flyway 12.4.0, JUnit 6.0.3, WireMock 3.13.2, PostgreSQL 18.7 (`postgres:18` in `compose.yaml`).
Postman checks were run with Newman 6.2.3, Postman's command-line Collection Runner.

- **Spec (written with AI, before coding):** `docs/SPEC.md`: operations, data each service owns,
  business rules, calls between services, failure behavior, CORS, acceptance checks A1–A14.
- **Added to the starter:** `spring-boot-starter-restclient` in `payments/build.gradle` (Boot 4
  needs it for `@ImportHttpServices` and the `spring.http.serviceclient.*` settings).

## Run it

Docker must be running. From this folder:

```bash
docker compose up -d                          # PostgreSQL 18 + the provider stand-in
cd invoices && ./gradlew bootRun              # http://localhost:8081
cd payments && ./gradlew bootRun              # http://localhost:8082
cd invoices && ./gradlew build                # 26 JUnit tests
cd payments && ./gradlew build                # 19 JUnit tests (WireMock plays invoices and the provider)
```

Acceptance checks: import `postman/spr03-acceptance.postman_collection.json` and
`postman/spr03.postman_environment.json`, select **SPR-03 local**, and run the collection.
Start from an empty database (`docker compose down -v && docker compose up -d`), because the
JUnit tests write to the same databases.

(My machine already has PostgreSQL on 5432, so I ran Compose with the database on 5433 and
started the services with `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/<db>`.)

## Evidence

| File | What |
|---|---|
| `evidence/01_postman_collection_run.txt` / `.json` | Collection Runner results (Newman CLI and JSON export): 45 requests, 114 assertions, 0 failed |
| `evidence/02_invoices_clean_build_tests.txt` | `./gradlew clean build` from a fresh copy: 26 tests passed |
| `evidence/03_payments_clean_build_tests.txt` | `./gradlew clean build` from a fresh copy: 19 tests passed |

Printed lines from the tests:

```text
[idempotency] replay with key d85d9508 returned the original invoice INV-1032; invoices before 27, after 27
[timeout] invoices delayed 6 s: payments answered 504 after 3.01 s
[report replay] evt-2e641a8c sent 3 times: applied once, 0 calls to invoices for the 2 resends
```

## How each exit criterion is met

| ID | Where |
|---|---|
| D1-1 Versioned resources | Paths are `/api/v1/...`. `InvoiceApiTest`: `201` + `Location` on create (and the Location is fetched), `204` on delete, `404` for unknown invoices, `409` for forbidden changes (cancel paid, draft → paid, delete sent or cancelled). Payment links: `201` + `Location`, then `200`. |
| D1-2 problem+json | One `ApiExceptionHandler` per service (extends `ResponseEntityExceptionHandler`, so Spring's own 404/405/415 get the same shape). Every refusal has `type`, `title`, `status`, `detail`; tests assert the body for each failure mode, and the Postman collection checks every 4xx/5xx in both services. CORS rejections are problem+json too. |
| D1-3 Idempotent writes | Status set twice → `200`, unchanged; delete twice → `204` then `404`, nothing else changes. Create with `Idempotency-Key`: replay returns the stored `201` response (same body and Location, `Idempotent-Replayed: true`), same key + other content → `422`, no new invoice; 4 simultaneous requests with one key → 1 invoice. Payment reports are stored by `eventId` the same way. `IdempotencyTest`, `PaymentReportTest`, Postman folders 2, 4, 6, 7. |
| D1-4 OpenAPI 3.1 | springdoc serves `openapi: 3.1.0` for both services. `InvoiceApiTest`/`IdempotencyTest` validate real invoice and problem responses against `Invoice` and `Problem` with the supplied `OpenApiResponseValidator`; `OpenApiDocumentTest` shows the validator rejects a wrong body. Payments: `PaymentLink` and `Problem` validated too (optional). |
| D1-5 Data ownership and declarative calls | Each service has its own database and login; payments never reads invoice rows. `InvoicesClient` and `ProviderClient` are `@HttpExchange` interfaces registered with `@ImportHttpServices` on `RestClient`; base URL, connect timeout (1 s) and read timeout (3 s) per group in `application.yaml`, with `application-staging.yaml` as a second environment. |
| D1-6 WireMock | `PaymentLinkTest` and `PaymentReportTest`: invoices answers (link created, report applied), returns `500`/`503` (payments answers `502`, no internals leaked), is 6 s slow (payments answers `504` after about 3 s); provider `503` and slow provider too. |
| D1-7 CORS | `CorsTest` (invoices) and `CorsAndOpenApiTest` (payments): a preflight from `https://billing.example.com` gets the methods and headers it needs; another origin gets `403`; Postman folder 8 checks both services. Allowed origins are configuration (`billing.cors.allowed-origins`). |

## Decisions

- **Status changes:** `PUT /api/v1/invoices/{id}/status` with `{status}`; one endpoint for send,
  cancel and paid. The payments service sets `PAID` through it.
- **Repeated delete:** the second `DELETE` is `404`. Nothing changes, which is what "no further
  effect" asks; D1-1 also wants `404` for a missing resource.
- **Idempotency storage:** `idempotency_record` table in the invoices database (Flyway V2), written
  in the same transaction as the invoice, keyed by `Idempotency-Key`, with a SHA-256 of the
  content. Same content means same customer, description and amount (`1200` = `1200.00`).
  The key is optional: requests without one are always new.
- **Payment link:** `POST /api/v1/payment-links` with `{invoiceId}`. The invoice's status and total
  come from the invoices API; the provider is called only when there is no link yet.
- **Reports payments can't apply:** `422` (unknown checkout, status other than `paid`, wrong
  amount, invoice gone) and nothing is stored; `409` for a cancelled invoice. Reports refused
  because invoices failed or was slow are not stored either, so the provider's resend is applied.
- **Amount check:** yes. Only a full payment marks the invoice paid (partial payments are out of
  scope).
- **HTTP/1.1 for outgoing calls:** Java's `HttpClient` (Boot's default client here) offers
  plain-HTTP servers an upgrade to HTTP/2, and Spring sends JSON bodies in chunks. WireMock 3.13.2
  doesn't handle that combination, so every checkout failed (`400` "required" or a reset stream);
  a fixed-length body or HTTP/1.1 works. `HttpClientsConfig` pins the client to HTTP/1.1.
  (Starting WireMock with `--disable-http2-plain`, or `http2PlainDisabled(true)` in tests, also
  fixes it.)
- **Timeouts:** read timeout 3 s → `504` (`downstream-timeout`); an error status or no connection
  → `502` (`downstream-failed`). Downstream bodies are logged, never returned.
- **CORS** is a `CorsFilter` so a rejection can carry the same problem+json body.
