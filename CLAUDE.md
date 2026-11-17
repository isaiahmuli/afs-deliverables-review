# Deliverables Review: AFS 30-Nov cohort

This workspace exists to **review trainee deliverables by doing them**. For each deliverable, we
complete it as a SWE1 (junior engineer) trainee would, then report how the deliverable itself
holds up: clarity, gaps, difficulty, and time. The goal is to improve the deliverable before
trainees get it. It is not to produce a polished product.

## Layout

One folder per deliverable, named by its curriculum code (for example `AI-01/`, `EDA-01/`).

```
<CODE>/
  deliverable-instructions.md   # what the trainee sees (source of truth; may be .yml in older ones)
  starter/                      # only if the deliverable says starter files are supplied
  solution/                     # our attempt, done as a trainee would
  report.md                     # SWE1-perspective report on the deliverable (required)
```

Folders may be reorganised later when deliverables turn out to build on each other. Ask before
moving or deleting anything a previous session produced.

## Source of truth

- `deliverable-instructions.md` (the trainee-facing file) is the **only** spec to work from.
- Any `blueprint.yml` is just the curriculum page for that day. Do not compare its deliverable
  section against the instructions. Use it only to check what the day's lessons teach, for
  example whether a prerequisite was covered.
- If the instructions change mid-review, redo the affected work from the new version and
  rewrite `report.md`. Do not keep findings about text that no longer exists.

## How to do a deliverable

1. Read the instructions only as a trainee would. Note every point where you hesitate, guess, or
   have to make an assumption. These notes are the raw material for the report.
2. Respect the deliverable's rules exactly: required versions, tools, scope, what is in and out,
   and the time budget. Do not add work outside scope to make the solution look better.
3. Use current, real versions. Check Maven Central, PyPI, Docker Hub, or start.spring.io rather
   than relying on memory, and name exact versions.
4. Verify what can be verified: run builds, tests, or connection checks, and keep the output as
   evidence. If something could not be verified, say so plainly.
5. Map the solution back to every exit criterion before writing the report.
6. Do not commit or push. A trainee repo inside `solution/` may be `git init`-ed but is left
   uncommitted unless the user asks.

## report.md (required for every deliverable)

Write it from the trainee's point of view, in plain language. Include:

- **Summary:** could a SWE1 complete it from the instructions alone, difficulty, and realistic
  SWE1 time versus `time_budget_min`.
- **Walkthrough:** step by step, where the trainee would get stuck and why.
- **Ambiguities:** a table with columns *instruction text → trainee's question → impact
  (High/Medium/Low) → suggested wording fix*.
- **Exit criteria check:** how the solution meets each criterion, and any criterion that is hard
  to evidence or to grade.
- **Prerequisites:** what the trainee must already know, and whether the day's material covers it.
- **What worked well**, and **recommendations** in priority order.

Explain findings so the trainer can decide: say what happens to a trainee who follows the text
literally ("a trainee who does X will fail D1-3 because..."), not reviewer shorthand.

## Environment notes

- JDK 25, Python 3.14, uv, and Docker are installed. The local `gradle` is 8.8 and cannot load
  Spring Boot 4 plugins, so use the project's Gradle wrapper (9.x).
- Use the scratchpad for throwaway files, not deliverable folders.
