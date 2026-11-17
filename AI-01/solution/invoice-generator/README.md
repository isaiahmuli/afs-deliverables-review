# Invoice Generator

A REST API that turns an invoice request (who is billed, when, where, and for what) into a PDF.

**Status:** specification and agent context only (milestone M0). No application code yet; the
first build is milestone M1.

| Start here | |
|---|---|
| [`docs/SPEC.md`](docs/SPEC.md) | What to build: features, API contract, architecture, requirements with acceptance checks, scope, open questions, milestones |
| [`CLAUDE.md`](CLAUDE.md) / [`AGENTS.md`](AGENTS.md) | Instructions for Claude Code / OpenAI Codex |
| [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md) | Code, test, and Git rules |
| [`docs/MCP.md`](docs/MCP.md) | Context7 MCP setup and when agents should use it |
| [`.claude/skills/implement-spec-task/SKILL.md`](.claude/skills/implement-spec-task/SKILL.md) | Reusable skill for building one spec task |

## Requirements

Java 25 and Git. Gradle comes with the wrapper (`./gradlew`).

```
./gradlew build      # compile and run tests
./gradlew bootRun    # start on http://localhost:8080
```

Project created with [Spring Initializr](https://start.spring.io/) (Gradle Kotlin, Java 25,
Spring Boot 4.1.2, dependencies: Spring Web, Validation).
