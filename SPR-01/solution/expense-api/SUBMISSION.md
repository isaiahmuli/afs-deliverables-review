# Expense submission API: submission notes

Generated from start.spring.io (Spring Boot 4.1.2, Java 25, Gradle wrapper 9.9.1; dependencies:
Spring Web MVC, Validation, Actuator). Resolved versions: Hibernate Validator 9.1.5.Final,
JUnit 6.0.3, Jackson 3.1.7.

- **Spec (written first):** `docs/SPEC.md`: contract, data model, validation rules, acceptance checks A1–A11.

## Run it

```bash
./gradlew clean build                                   # 36 tests
./gradlew bootRun                                       # base limit: $500 per claim
./gradlew bootRun --args='--spring.profiles.active=team-b'   # team B: $100 per claim
evidence/requests.sh http://localhost:8080 valid list get1 unknown empty over limit150 health metrics hidden
```

## Evidence

| What | File |
|---|---|
| Clean build from a fresh copy (no `build/` or `.gradle/`), every test listed as PASSED | `evidence/01_clean_build_tests.txt` |
| Every outcome over real HTTP, base profile: 201, 200, 404, 400 (all fields), 400 (custom constraint), 400 (bad type), $150 accepted, health, metrics, hidden endpoints | `evidence/02_requests_base_profile.txt` |
| Same build with `team-b`: the same $150 claim is refused with 422 and not saved | `evidence/03_requests_team_b_profile.txt` |
| The script that produced 02 and 03 | `evidence/requests.sh` |

The profile switch, in one comparison:

```text
base (limit 500)   POST $150 of $150  -> 201 Created, Location: /api/expenses/2
team-b (limit 100) POST $150 of $150  -> 422 {"title":"Claim exceeds team limit",
                    "detail":"The claim of 150.00 USD exceeds this team's limit of 100.00 USD per claim."}
                   GET /api/expenses  -> []
```

## Packages

```text
web           ExpenseController, ExpenseRequest/Response, ApiExceptionHandler   -> service
web.validation @ClaimWithinReceipt (custom constraint) and its validator
service       ExpenseService (team limit), ExpenseNotFoundException, ClaimLimitExceededException -> repository, config
repository    ExpenseRepository, InMemoryExpenseRepository
config        ExpenseLimitsProperties (@ConfigurationProperties "expenses")
domain        Expense, NewExpense
```

## How each exit criterion is met

| ID | Where |
|---|---|
| D1-1 Layers and DI | Constructor injection only. `ExpenseController` maps HTTP to `ExpenseService`; the service holds the limit rule and uses `ExpenseRepository`; it imports nothing from `org.springframework.http` or `web`. |
| D1-2 Configuration | `ExpenseLimitsProperties` binds `expenses.max-claim-amount` (validated, > 0): `500.00` in `application.properties`, `100.00` in `application-team-b.properties`. `TeamBProfileIntegrationTest` and evidence 02/03 show the same request accepted then refused. |
| D1-3 Validation | Built-in: `@NotBlank`, `@Size`, `@NotNull`, `@PastOrPresent`, `@DecimalMin`, `@Digits(fraction = 2)`. Custom: `@ClaimWithinReceipt` (claim ≤ receipt total), reported on `claimAmount`. A `400` lists every failing field (`{}` → 5 fields). |
| D1-4 Actuator | `management.endpoints.web.exposure.include=health,metrics`. Health is `UP`; `http.server.requests` has COUNT and TOTAL_TIME for `/api/expenses`; `env`, `configprops`, `beans` and others return 404. |
| D1-5 Web layer | `ExpenseControllerTest` (`@WebMvcTest`, MockMvc, `@MockitoBean ExpenseService`): status and JSON body for 201, 400 (all fields, custom constraint, bad amounts, bad types), 422, 200, 404; `verify(service, never())` on every refusal. |
| D1-6 Full app | `ExpenseApiIntegrationTest` (`@SpringBootTest`, random port, `RestTestClient` over real HTTP): submit then retrieve by id and in the list; refused submissions not saved; health, metrics, hidden endpoints. `./gradlew clean build` passes from a fresh copy. |

## Decisions

- Errors are RFC 9457 problem details. Field failures: `400` with `errors: [{field, message}]`,
  sorted by field. Over the team limit: `422` with the claim and the limit.
- A value of the wrong type (`"fifty"` for an amount, `2026-13-40` for a date) stops JSON parsing
  before validation, so that response names only that field.
- `ApiExceptionHandler` extends `ResponseEntityExceptionHandler`. With
  `spring.mvc.problemdetails.enabled=true`, a plain `@RestControllerAdvice` is skipped for
  validation errors and Spring Boot's default handler answers `"Invalid request content."` with
  no field names.
