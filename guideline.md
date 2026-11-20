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
# <CODE>

**Date:** <D Mon YYYY>\
**Deliverable:** <CODE>: <Title>

I worked through this deliverable as a SWE1 trainee would, following the instructions as written,
and kept notes wherever I hesitated or had to guess.

## Overview

One short paragraph: what the trainee builds or fixes, the starter (or that there is none),
whether a SWE1 can finish it from the instructions alone, and the realistic time against the
budget. End with "What works well:" and a few bullets: how setup went and the moments that teach
well. Quote what you actually saw (test counts, output lines, error messages).

## Concerns

1. **Short label (high impact, if it is).** What a trainee would do or get wrong, with the
   numbers or output that show it, and the one sentence that would prevent it.
2. ...

## Summary

- **Changes needed:** Yes. <one-line list of the fixes above> / No.
- **Can be done in 2 hrs?** Yes / Borderline, <a–b h> / No, <a–b h>.
- **Verdict:** One or two short sentences.
```

- Keep the header minimal: the title is just the code, then only the Date and Deliverable lines
  (`<CODE>: <Title>`). No From, Branch, PR number or `status:`, and no starter details in the
  intro sentence (the starter belongs in the Overview paragraph).
- Use exactly these sections, in this order: Overview, Concerns, Summary. No "At a glance"
  table and no separate "Suggested changes" line (that list is the "Changes needed" bullet).
- Do not include a walkthrough, an exit-criteria table, an evidence dump, or a prerequisites
  section. Detailed evidence belongs in the solution (for example its `SUBMISSION.md`).
- If the realistic time is over the budget, that is the first concern ("Time (high impact)").
- Order the points by impact: the time limit, setup blockers, and anything that makes an exit
  criterion ungradeable or makes a trainee who solves the stated problem still fail a criterion.
- Each point should say what a trainee would actually do or get wrong, and how you know (what
  happened when you did it).
- Say what is good too. "Overview" is where strong starters and well-designed moments are
  credited.
- The summary is a short list, not a table: each file covers one deliverable.
- Mention things that affect more than one deliverable (for example version drift between
  starters) where you notice them.
