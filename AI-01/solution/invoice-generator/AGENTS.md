# AGENTS.md: Invoice Generator

Project brief for **OpenAI Codex** (and other AGENTS.md-aware agents). [`CLAUDE.md`](CLAUDE.md) is
the same brief for Claude Code. Keep the shared sections of both files identical; only
"Codex specifics" differs.

## Project context

A stateless REST API for invoices: a caller POSTs who is billed, when, where, and for what, and gets
back a PDF invoice. There is no database, authentication, or deployment.

| Doc | Read it when |
|---|---|
| [`docs/SPEC.md`](docs/SPEC.md) | **Always first.** Features, API contract, architecture, requirements R1–R12 with acceptance checks, scope, open questions, milestones and tasks |
| [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md) | Before writing code, tests, or commits; ends with the review-note template |
| [`docs/MCP.md`](docs/MCP.md) | Before using any library API (OpenPDF, PDFBox, Spring Boot 4) |
| [`.claude/skills/implement-spec-task/SKILL.md`](.claude/skills/implement-spec-task/SKILL.md) | Whenever you build or fix a spec task (T1–T6) or requirement |

`docs/SPEC.md` wins over everything else, including this file. Current milestone: **M1, tasks T1–T6**.

## Technology

Java 25 · Spring Boot 4.1.2 (Spring MVC, Bean Validation, Jackson 3) · OpenPDF 3.0.5 ·
PDFBox 3.0.8 (tests only) · Gradle 9.9.1 wrapper (Kotlin DSL) · Git.
Exact versions and reasons: SPEC section 4.

## Operation

| Purpose | Command |
|---|---|
| Build and run all tests | `./gradlew build` |
| Run one requirement's tests | `./gradlew test --tests '*<TestClass>'` (class names are in SPEC section 7) |
| Run the app on `localhost:8080` | `./gradlew bootRun` |
| Try the API | `curl -s -o /tmp/invoice.pdf -H "Content-Type: application/json" --data @docs/examples/invoice-request.json http://localhost:8080/api/v1/invoices` |
| Check for persistence deps (R10) | `./gradlew dependencies --configuration runtimeClasspath \| grep -iE "jdbc\|jpa\|r2dbc\|mongo\|redis" \|\| echo none` |

Always use `./gradlew`, never a system `gradle`.

## How we work

Follow **Specify → Plan → Build → Verify → Review → Hand back** for every task, using the
`implement-spec-task` skill:

1. Work on **one task at a time**, in the order of SPEC section 10.2.
2. **Post a plan and wait for approval** before editing files.
3. Change only the files in the approved plan.
4. **Prove it:** run the task's acceptance checks exactly as written and paste the real output.
5. End with the review note from `docs/CONVENTIONS.md`.

## Boundaries

**Always**
- Ask when something is unclear or an open question (SPEC section 9) applies. Never guess requirements.
- Consult Context7 (MCP) for library APIs and versions instead of relying on memory.
- Keep request data (names, addresses) out of logs, errors, and test output.

**Ask first**
- Adding or upgrading a dependency.
- Touching files outside the approved plan.
- Anything that would change the API contract in SPEC section 5.

**Never**
- Edit requirement text in `docs/SPEC.md`, or build deferred/excluded work (SPEC sections 8.2, 8.3).
- Add a database, persistence, authentication, messaging, or deployment setup.
- `git commit`, `push`, `merge`, or open PRs unless a human asks in this session.
- Commit or create secrets, `.env` files, `build/`, or `.gradle/` content.
- Weaken, skip, or delete a test or acceptance check to make it pass.

## Verification and review rules

- **Done = evidence.** A requirement is done only when its SPEC acceptance check has been run and
  its output matches "Expected". Confidence is not evidence.
- `./gradlew build` must end with `BUILD SUCCESSFUL` and 0 failures before hand-back.
- Self-review the diff (`git status`, `git diff --stat`) for unplanned files, money as `double`,
  Spring imports in `domain`, logged request data, and missing failure-path tests.
- A human reviews and commits every task. Stop after one task.

## Codex specifics

- **Skill:** the task workflow lives in
  [`.claude/skills/implement-spec-task/SKILL.md`](.claude/skills/implement-spec-task/SKILL.md),
  the single source shared with Claude Code. **Read that file and follow it** for any T*n* or R*n* work.
- **MCP:** Context7 is configured in [`.codex/config.toml`](.codex/config.toml). Codex only loads
  project config after you **trust this repository**. Otherwise add the same block to
  `~/.codex/config.toml`. Check with `codex mcp list`.
- **Plan gate:** for step 2 of the skill, post the plan and end your turn. Do not edit files in the same turn.
- **Sandbox:** `./gradlew` needs network access the first time, to download Gradle and dependencies.
  Request it rather than working around a failed download.
- Keep this file and `CLAUDE.md` in sync. If you change a shared section here, make the same change there.
