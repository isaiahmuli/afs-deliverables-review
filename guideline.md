# Guidelines

These apply to every deliverable, on every branch and every change.

## Deliverable feedback (required for every deliverable)

Every deliverable folder gets a feedback file, `<CODE>/<CODE>-deliverable-feedback.md` (for
example `SPR-01/SPR-01-deliverable-feedback.md`). Older folders still use `report.md`. It is the
reviewer's private file: it is gitignored and must never be staged, committed, or pushed.

### What to note while doing the deliverable

- Read the instructions only as a trainee would. Note every point where you hesitate, guess, or
  have to make an assumption. These notes are the raw material for the report.
- Map the solution back to every exit criterion before writing the report. Any criterion that is
  hard to evidence or to grade becomes a point in the report.
- If the instructions change mid-review, redo the affected work from the new version and
  rewrite the feedback file. Do not keep findings about text that no longer exists.

### Format

Write it as **first-person feedback from someone who did the deliverable as a SWE1 trainee**:
what it was like, where you lost time or had to guess, and what you would change. Plain
language, short paragraphs and numbered points, with **real numbers from your run** (counts,
timings, error text) rather than general claims. This applies from JAVA-01 onwards; AI-01, SQL-01
and SQL-02 keep the older table format.

Use this structure:

```markdown
# <CODE> feedback: <Title>

**From:** <reviewer> · **Date:** <D Mon YYYY>
**Deliverable:** <CODE> / <n> – <Title> (`status: <status>` in the instructions)
**Branch:** `deliverable/<code>` (PR #<n>)

I worked through this deliverable as a SWE1 trainee would: <starter only / no starter>, following
the instructions as written, and kept notes wherever I hesitated or had to guess.

## At a glance

| | <CODE> |
|---|---|
| Can a SWE1 finish it from the instructions alone? | Yes / Yes, with effort / ... |
| Difficulty | Low / Medium / Medium-high / High |
| Realistic SWE1 time (budget is <n> min) | <a–b h> |
| Starter quality | Excellent / Good / ... / No starter supplied |

## Experience

A short paragraph or a few bullets: how setup went, what worked well, and the moments that teach
well. Quote what you actually saw (test counts, output lines, error messages).

## Where I hesitated / where a trainee will get stuck

1. **Short label (high impact, if it is).** What a trainee would do or get wrong, with the
   numbers or output that show it, and the one sentence that would prevent it.
2. ...

**Suggested changes:** a one-line list of the fixes above.

## Summary for the tracker

| Code | Any changes needed? | Can be done in 2 hrs? | Verdict |
|---|---|---|---|
| <CODE> | Yes: <short list> / No | Yes / Borderline, <a–b h> / No, <a–b h> | One or two short sentences. |
```

- Do not include a walkthrough, an exit-criteria table, an evidence dump, or a prerequisites
  section. Detailed evidence belongs in the solution (for example its `SUBMISSION.md`).
- Order the points by impact: the time limit, setup blockers, and anything that makes an exit
  criterion ungradeable or makes a trainee who solves the stated problem still fail a criterion.
- Each point should say what a trainee would actually do or get wrong, and how you know (what
  happened when you did it).
- Say what is good too. "Experience" is where strong starters and well-designed moments are
  credited.
- Mention things that affect more than one deliverable (for example version drift between
  starters) where you notice them.
