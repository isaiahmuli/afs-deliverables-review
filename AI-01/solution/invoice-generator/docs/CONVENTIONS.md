# Conventions

Rules for anyone, human or agent, changing this repository. `docs/SPEC.md` says **what** to
build. This file says **how**.

## Code

- **Java 25, Spring Boot 4.1.2.** Use what the Boot BOM manages; add a version only for libraries
  Boot does not manage (OpenPDF, PDFBox), and name that version in `docs/SPEC.md` section 4 first.
- **Packages:** `com.acme.invoice.api`, `.domain`, `.pdf` (see SPEC section 6). `domain` must not
  import Spring, Jackson, or OpenPDF.
- **Records** for request DTOs and domain values. No Lombok.
- **Constructor injection** only. No field `@Autowired`.
- **Money:** `BigDecimal` only, never `double` or `float`. Round with `RoundingMode.HALF_UP` to
  the currency's default fraction digits, in `InvoiceCalculator` and nowhere else.
- **Time:** `OffsetDateTime` as received. Never call `LocalDateTime.now()` or convert to the server zone.
- **Errors:** return `ProblemDetail` from `ApiExceptionHandler`. Never put stack traces or request
  data in a response.
- **Logging:** never log names, addresses, or line items. Log status, item count, and duration only.
- **Comments:** explain *why*, not *what*. Do not leave commented-out code or TODOs without an
  open-question ID (`// OQ-3: tax deferred`).

## Tests

- Every requirement R*n* in SPEC section 7 has the test class named in its acceptance check. Do
  not rename those classes; reviewers search for them.
- Test behaviour through public APIs (HTTP via MockMvc, the calculator's public method, the
  renderer's output text). Do not assert private methods or mock the class under test.
- Cover success, boundary, and failure paths for each rule (e.g. quantity 1, 10,000, 0, 10,001).
- No test may need the network, a database, or Docker.

## Build and dependencies

- Always use `./gradlew`. The local `gradle` may be the wrong version.
- Adding a dependency needs: a reason in the task plan, the version from Context7 or Maven Central
  (not memory), and an update to SPEC section 4.
- Never add persistence, security, or messaging starters (excluded in SPEC section 8.3).

## Git

- One task (T1–T6) = one branch = one focused diff. Branch name: `m1/t<n>-<short-name>`.
- Commit messages: imperative and specific, referencing the task and requirements:
  `T3: add InvoiceCalculator (R5)`.
- **Agents do not commit, push, or open PRs** unless a human asks in that session.
- Never commit `build/`, `.gradle/`, IDE folders, `.env` files, keys, or tokens. `.gitignore`
  already covers build output; check `git status` before handing back.

## Review note (end of every task)

Each task ends with a short note in the agent's final message:

```
Task: T<n> (<requirements>)
Files changed: <list>
Checks run: <command> → <result, pasted>
MCP lookups: <library id + question, or "none">
Spec deviations or open questions: <none, or OQ-n raised>
```
