# Deliverables Review: AFS 30-Nov cohort

This workspace exists to **review trainee deliverables by doing them**. For each deliverable, we
complete it as a SWE1 (junior engineer) trainee would, then report how the deliverable itself
holds up: clarity, gaps, difficulty, and time. The goal is to improve the deliverable before
trainees get it. It is not to produce a polished product.

## Guidelines (always apply)

Before starting work on any branch, and before making any change, read
[`guideline.md`](guideline.md) and apply it. It holds the rules for each deliverable's
private feedback file. When it and this file disagree, follow `guideline.md`.

## Layout

One folder per deliverable, named by its curriculum code (for example `AI-01/`, `EDA-01/`).

```
<CODE>/
  deliverable-instructions.md   # what the trainee sees (source of truth; may be .yml in older ones)
  starter/                      # only if the deliverable says starter files are supplied
  solution/                     # our attempt, done as a trainee would
  <CODE>-deliverable-feedback.md  # private feedback, see guideline.md (older folders: report.md)
```

Folders may be reorganised later when deliverables turn out to build on each other. Ask before
moving or deleting anything a previous session produced.

## Source of truth

- `deliverable-instructions.md` (the trainee-facing file) is the **only** spec to work from.
- Any `blueprint.yml` is just the curriculum page for that day. Do not compare its deliverable
  section against the instructions. Use it only to check what the day's lessons teach, for
  example whether a prerequisite was covered.
- If the instructions change mid-review, redo the affected work from the new version.

## How to do a deliverable

1. If the instructions reference starter files and they are not in the folder, stop and ask for
   them. Never write a stand-in starter.
2. Respect the deliverable's rules exactly: required versions, tools, scope, what is in and out,
   and the time budget. Do not add work outside scope to make the solution look better.
3. Use current, real versions. Check Maven Central, PyPI, Docker Hub, or start.spring.io rather
   than relying on memory, and name exact versions.
4. Verify what can be verified: run builds, tests, or connection checks, and keep the output as
   evidence. If something could not be verified, say so plainly.
5. Do not commit or push unless the user asks. A trainee repo inside `solution/` may be
   `git init`-ed but is left uncommitted unless the user asks.

## Environment notes

- JDK 25, Python 3.15, uv, and Docker are installed. A local PostgreSQL already listens on port 5432, so map Docker databases to another port. The local `gradle` is 8.8 and cannot load
  Spring Boot 4 plugins, so use the project's Gradle wrapper (9.x).
- Use the scratchpad for throwaway files, not deliverable folders.
