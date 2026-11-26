---
module: FE-04
code: FE-04
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Make the bike reservation page work without a mouse
subtitle: Fix keyboard and screen-reader access to a reservation flow, then guard it with browser tests.
time_budget_min: 90
status: draft
---

## Topic: Deliverable

### Exercise: Fix and test the reservation page

A bike-share app lets riders pick a station, choose a bike, and reserve it. A rider who cannot use a mouse says they never get past the station picker: Tab skips over it, and pressing Enter does nothing. A screen reader reads the Reserve control out as just "clickable", with no name, and says nothing about whether the station list is open.

The city has flagged the page in an accessibility review. The team also has no test that reserves a bike the way a rider does, so when a change last month broke the confirmation step, nobody noticed until riders wrote in.

Make the whole reservation usable by keyboard and screen reader, through to confirmation. Protect the flow with checks that catch broken reservations and returning accessibility defects.

What the project does today:

- Press Tab from the top of the page. Focus never reaches the station picker or the Reserve control.
- Click the picker and choose a station. No button shows where keyboard focus is.
- The Reserve control has no accessible name. The picker does not say whether its list is open.
- No test reserves a bike. Playwright records no trace.

The station picker is a hand-built control: a clickable element that opens a list of stations. The mock API runs in the browser through MSW, so the dev server and the tests share the same handlers.

:::download ./assets/fe04_d01_starter.zip Reservation page starter
A Vite + React + TypeScript project with the reservation and confirmation pages, a mock API, and Playwright installed with one smoke test. README.md lists the commands.
:::

:::note
**Time limit:** 90 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

- A rider can go from choosing a station to seeing the confirmation using only the keyboard
- Whichever control has focus is visible, and focus moves in a sensible order from the first control to the confirmation
- The station picker, and any other hand-built control, tells a screen reader its role, its name, and its state, such as open or closed
- The reservation page and the confirmation page have no WCAG 2 A or AA violations that the automated scan can find
- A browser test completes a full reservation against a mock API, and it keeps a trace of the run when it fails and is retried

### Theory: Rules

- React 19, Playwright 1, axe-core 4 through @axe-core/playwright, MSW 3
- Tests reach the API only through the mock API, served to the browser by the shared MSW handlers through @msw/playwright or by page.route; no test uses the real network
- The mock API's endpoints and response shapes are fixed; changing them is out of scope
- The submission is the edited project and its tests
- The station picker may stay a hand-built disclosure, a button that opens a list, or become a native select; a full combobox pattern is out of scope
- Before coding, write a short spec of the reservation flow, each control's role, name, and state, the focus order, and the checks that guard them, then build against it
- Out of scope: translation, visual-regression tests, styling beyond what keyboard focus needs, server code

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Test a critical user flow end to end in a real browser | A Playwright test completes the reservation from choosing a station to the confirmation, against a mock API |
| D1-2 | Keep a trace of a failing end-to-end run | The Playwright configuration allows at least one retry and records a trace on retry, and the submission includes a trace from a deliberately failed run |
| D1-3 | Support keyboard-only use | A Playwright test completes the reservation with the keyboard alone, asserting the focus order and that the focused control is visible; this test may also satisfy D1-1 |
| D1-4 | Give hand-built controls the right role, name, and state | The station picker exposes a role, an accessible name, and, while it stays hand-built, its open or closed state, and a test checks each of them |
| D1-5 | Pass automated WCAG 2 AA checks | An axe scan through @axe-core/playwright with the wcag2a and wcag2aa tags covers every page of the flow, including the station picker while it is open, and reports zero violations |

### Exercise: What to submit

- The edited project source, without `node_modules`
- The Playwright tests, and the output of `pnpm test:e2e` showing them pass
- The trace file from a deliberately failed run
- Your short spec, as a Markdown file in the project
