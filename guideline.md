# Guidelines

These apply to every deliverable, on every branch and every change.

## report.md (required for every deliverable)

Every deliverable folder gets a `report.md` (`<CODE>/report.md`). It is the reviewer's private
file: it is gitignored and must never be staged, committed, or pushed.

### What to note while doing the deliverable

- Read the instructions only as a trainee would. Note every point where you hesitate, guess, or
  have to make an assumption. These notes are the raw material for the report.
- Map the solution back to every exit criterion before writing the report. Any criterion that is
  hard to evidence or to grade becomes a row in the report.
- If the instructions change mid-review, redo the affected work from the new version and
  rewrite `report.md`. Do not keep findings about text that no longer exists.

### Format

Keep it **simple and short**, focused mostly on ambiguities and challenges, from the trainee's
point of view in plain language. Use exactly this structure (see `AI-01/report.md`):

```markdown
# <CODE> Deliverable Review – SWE1 Trainee Perspective

**Deliverable:** <CODE> / <n> – <Title>
**Reviewed:** <D Month YYYY>

## 1. Overall Assessment
One or two sentences: achievable from the instructions? Within the time limit? (realistic SWE1 time)

## 2. What Worked Well
- A few short bullets.

## 3. Challenges and Ambiguities
| Area | Observation | Suggested Improvement |
| --- | --- | --- |
| Short label | What a trainee hits or can't decide, in one or two sentences | One concrete fix |
```

- Do not include a walkthrough, an exit-criteria table, an evidence dump, or a prerequisites
  section.
- Put the most important rows first: the time limit, setup blockers, and anything that makes an
  exit criterion ungradeable.
- Each observation should say what a trainee would actually do or get wrong.
- Detailed evidence belongs in the solution (for example its README or findings note), not in
  the report.
