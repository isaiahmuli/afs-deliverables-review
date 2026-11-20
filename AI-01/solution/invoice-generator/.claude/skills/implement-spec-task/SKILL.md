---
name: implement-spec-task
description: Implement exactly one bounded task (T1–T6 or a later milestone task) from docs/SPEC.md for the invoice generator, with a plan gate, acceptance-check evidence, and a review note. Use when asked to build, continue, or fix a spec task or requirement (R1, R2, ...). Not for changing the spec itself.
---

# Implement one spec task

Repeatable workflow for building one task from `docs/SPEC.md` section 10.2 so that every task
is planned, bounded, proven with the spec's acceptance checks, and handed back for human review.
Works the same in Claude Code and Codex.

## When to use it

Use when a human asks you to:
- build a named task ("do T3", "start the calculator task"), or
- implement or fix a named requirement ("make R7 pass"), or
- continue an unfinished task.

Do **not** use it to:
- change requirements, scope, or open-question answers in `docs/SPEC.md` (humans own the spec);
- work on anything listed under SPEC section 8.2 (deferred) or 8.3 (excluded);
- do several tasks in one go. One invocation = one task.

## What it needs

Before writing any code, confirm you have all of these. If one is missing, ask; do not assume.

| Input | Where it comes from |
|---|---|
| Task ID (e.g. `T3`) or requirement IDs (e.g. `R5`) | The human's request |
| The task row and its requirements, quoted word for word | `docs/SPEC.md` sections 7 and 10.2 |
| Dependencies are done (the tasks listed under "Depends on") | Ask the human, or check that those tasks' acceptance checks pass |
| Coding, test, and Git rules | `docs/CONVENTIONS.md` |
| Current library API details | Context7 MCP, per `docs/MCP.md` |

## What it may do

**Step 1: Specify.** Quote the task row and every requirement it covers, including each
acceptance check. List any open questions (OQ-*n*) that touch this task and the "until
answered" rule you will follow.

**Step 2: Plan, then stop for approval.** Post a plan with:
- the files to create or change (paths), each with a one-line reason;
- the tests to add, named as in the acceptance checks;
- any dependency to add, with its version checked through Context7 or Maven Central;
- the commands you will run to verify.

**Wait for the human to approve the plan.** If they change it, follow the new plan.

**Step 3: Build.** Change only the files in the approved plan. Follow `docs/CONVENTIONS.md`.
Consult Context7 before using OpenPDF, PDFBox, or Spring Boot 4 APIs you are not certain of.

**Step 4: Verify.** Run every acceptance check for the task's requirements exactly as written in
the spec, then `./gradlew build`. Paste the real output. If a check fails, fix the code and
re-run. Never edit a check or test to make it pass.

**Step 5: Review your own diff.** Run `git status` and `git diff --stat`, then check:
- only planned files changed; no `build/`, `.gradle/`, IDE files, secrets, or stray files;
- no new logging of request content; no `double` for money; no Spring imports in `domain`;
- tests cover success, boundary, and failure paths for each rule in the task.

**Step 6: Hand back.** End with the review note from `docs/CONVENTIONS.md`.

**Allowed:** read any file; create and edit files in the approved plan; run `./gradlew`
tasks, `curl` against `localhost`, and `git status` / `git diff`; query Context7.

**Not allowed:** `git commit`, `push`, `merge`, `rebase`, or opening PRs (unless the human asks
in this session); editing `docs/SPEC.md` requirement text; adding persistence, security, or
messaging dependencies; touching files outside the plan; running anything against a
non-local host other than Context7.

## How it is verified

The task is done only when **all** of these are shown in the hand-back:

1. Each acceptance check for the task's requirements was run, and its pasted output matches
   the spec's "Expected".
2. `./gradlew build` ends in `BUILD SUCCESSFUL` with 0 test failures.
3. `git status` shows only the planned files changed.
4. The review note lists files, checks with results, MCP lookups, and any open questions raised.

"Should work", "tests look fine", or a summary without output is not verification.

## When to stop

Stop and hand back to the human, without guessing, when any of these happen:

- **Plan written.** Always stop after step 2 until it is approved.
- **Missing input.** A required input from "What it needs" is unavailable.
- **Open question.** The task needs behaviour covered by an unanswered OQ beyond its
  "until answered" rule, or you find a new ambiguity. Propose a new OQ; do not decide it.
- **Spec conflict.** The spec contradicts itself, the conventions, or a library's real
  behaviour (e.g. an acceptance check cannot pass as written).
- **Scope pressure.** The fix needs files outside the plan, a deferred or excluded feature, or
  a new dependency that was not approved.
- **Stuck.** The same check still fails after 3 fix attempts. Report what you tried and the
  latest output.
- **Done.** All verification items pass. Hand back; do not start the next task.
