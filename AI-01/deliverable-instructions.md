---
module: AI-01
code: AI-01
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Specify an invoice generator for a builder agent
subtitle: Turn an invoice API idea into a bounded specification and reusable agent context.
time_budget_min: 90
status: draft
---

## Topic: Deliverable

### Exercise: Prepare the invoice generator for an agent to build

A team wants an invoice generator API. A caller sends who is billed, when, where, and for what. The API returns a PDF invoice.

The idea leaves features, requirements, and design decisions unresolved. Without a clear brief, a builder agent will fill those gaps with guesses.

Write the specification and agent context that make the work ready to delegate. Give the agent clear design steps and milestones it can collaborate on. Running application code is not the goal.

:::note
**Time limit:** 90 minutes. No starter files are supplied.
:::

## Topic: Outcomes

### Theory: The finished work

- A fresh Spring Boot project holds your specification and agent files. It does not need to run.
- One Markdown specification describes the proposed invoice generator's features, requirements, architecture, and technologies. It explains each part and the user journey from request to PDF.
- The specification separates the first build from deferred or excluded work. Open questions tell the builder agent what to ask instead of guessing.
- Clear design steps and milestones divide the proposed work into bounded tasks an agent can collaborate on.
- `CLAUDE.md` and `AGENTS.md` provide aligned entry points for the two agents. They cover project context, operation, commands, boundaries, verification, and review rules.
- Those entry points link to the specification, conventions, and any other project docs the agent needs.
- One skill packages a repeatable task for future invoice work. A skill is a reusable set of instructions for an agent.
- One public MCP source is connected. MCP gives agents external tools or information. A note explains when to consult it instead of memory.
- Every requirement states how the agent will prove it is done. Use a command, an example request and response, or an expected output.

Acceptance checks describe evidence for the future build. They do not require you to implement or run that build for this deliverable.

### Theory: Rules

- Use Java 25, Spring Boot 4 from Spring Initializr, Git, Claude Code, and OpenAI Codex.
- Use the current supported releases of both coding agents, current stable Git, and MCP support available in your chosen client.
- Keep the specification in one Markdown file in the repository.
- Choose the proposed architecture and technologies yourself, and name their versions. The Spring Boot project only anchors the files.
- The assessed work is your specification and agent context.
- Application implementation, PDF styling, and comparisons of the two agents' output quality are out of scope.

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Describe a concept or prototype in a clear specification document | The specification covers features, requirements, architecture, how each part works, and the user journey |
| D1-2 | Write durable project instructions for coding agents | CLAUDE.md and AGENTS.md agree on project context, operation, and boundaries, and point to the project docs |
| D1-3 | Package reusable agent behavior as a skill | A skill states when to use it, what it needs, what it may do, how it is verified, and when to stop |
| D1-4 | Connect a public MCP source for agent use | The MCP configuration is shown with a note on when an agent should consult it |
| D1-5 | Define done as evidence an agent must show | Each requirement in the specification ends with an observable acceptance check: a command, an example request and response, or an expected output |
| D1-6 | Scope a first build narrowly and defer the rest | The specification separates the first milestone from deferred or excluded work |

### Exercise: What to submit

Submit the Spring Boot project containing:

- The Markdown specification, including milestones, open questions, and acceptance checks.
- `CLAUDE.md`, `AGENTS.md`, and the project docs they reference.
- The reusable skill's source file.
- The public MCP connection configuration and its consultation note. Keep credentials out of the submitted configuration.
