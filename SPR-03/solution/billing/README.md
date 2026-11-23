# Invoice and payment services: starter

Two Spring Boot projects, a PostgreSQL server, a payment provider stand-in, and Postman files.
Everything here builds and connects. The API behavior is yours to build.

## What you need

- Java 25. If it is missing, Gradle downloads it on the first build.
- Docker, running. It hosts the database and the provider stand-in.
- Postman.

## Start the infrastructure

From this folder:

    docker compose up -d

| Service | Address | What it is |
|---|---|---|
| PostgreSQL 18 | `localhost:5432` | One server with two databases: `invoices` and `payments` |
| Provider stand-in | `http://localhost:8089` | WireMock playing the payment provider; see `provider/PROVIDER-API.md` |

Each service has its own database login. Neither login can connect to the other database.

## Run the services

In two terminals:

    cd invoices && ./gradlew bootRun     # http://localhost:8081
    cd payments && ./gradlew bootRun     # http://localhost:8082

`./gradlew build` runs the tests. The supplied test starts the application, so the
database must be running first.

## What is supplied

| Where | What |
|---|---|
| `invoices/` | Project, dependencies, database settings, the `invoice` table (Flyway migration), `Invoice` entity, `InvoiceStatus`, `InvoiceRepository` |
| `payments/` | Project, dependencies, database settings, the `payment_link` table, `PaymentLink` entity, `PaymentLinkRepository` |
| `*/src/test/.../support/OpenApiResponseValidator.java` | Test helper: fetches the service's OpenAPI document and checks a response body against a named schema |
| `provider/` | The provider's API description, its WireMock mappings, and a sample payment report |
| `postman/` | An environment with the base URLs, and a collection with one worked request and the sample payment report |

Not supplied: controllers, request and response types, error handling, idempotency storage,
HTTP clients, timeouts, CORS, and tests. Those are the work.

Invoice ids start at 1001, so the first invoice you create is 1001.

Using the OpenAPI helper in a `MockMvc` test:

```java
String doc = OpenApiResponseValidator.fetchDocument(mvc);
OpenApiResponseValidator.assertMatches(doc, "YourSchemaName", responseBody);
```

The schema name is the one springdoc lists under `components.schemas` in `/v3/api-docs`.

## Postman

Import both files from `postman/` and select the **SPR-03 local** environment.
The worked request shows how a check is written. Set `paymentReportUrl` to your report
endpoint before sending the sample payment report.

To export results: open the collection, choose **Run**, run it, then use **Export Results**.

## Reset the database

    docker compose down -v
    docker compose up -d

`-v` removes the database's storage, so it starts empty and runs `db/init.sql` again.
