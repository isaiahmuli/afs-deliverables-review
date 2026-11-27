---
module: FE-04
code: FE-04
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Make the bike reservation page work without a mouse and launch it in German
subtitle: Fix keyboard and screen-reader access to a reservation flow, translate it, and guard it with tests.
time_budget_min: 120
status: draft
---

## Topic: Deliverable

### Exercise: Fix, translate, and test the reservation page

A bike-share app lets riders enter a name, pick a station, and reserve a bike. A rider who cannot use a mouse never gets past the station picker, and a screen reader reads the Reserve control as just "clickable". The city has flagged the page in an accessibility review. No test reserves a bike the way a rider does, so when a change broke the confirmation step last month, nobody noticed until riders wrote in.

The app also launches in Berlin next month, and every word on the page is written into its components. There is no way to switch to German, and nothing would catch a translation that goes missing.

Make the reservation usable by keyboard and screen reader through to confirmation. Show the reservation and confirmation pages in English or German, with text, dates, prices, and counts correct in each. Add tests that run like a rider and checks that catch broken reservations, returning accessibility defects, and missing translations.

What the project does today:

- Tab reaches the name field, then skips the station picker and the Reserve control. No button shows keyboard focus.
- The Reserve control has no accessible name. The picker does not say whether its list is open.
- Central Station reads `1 bikes available`, the fee reads `EUR 3.50`, and the confirmation date reads `03/04/2030 at 9:30 AM`.
- No test reserves a bike, and Playwright records no trace.

The station picker is a hand-built control: a clickable element that opens a list. `GLOSSARY.md` pairs every English string with its German text.

:::download ./assets/fe04_d01_starter.zip Reservation page starter
A Vite + React + TypeScript project with the reservation and confirmation pages in English, a German glossary, a mock API, Playwright, and Vitest with Testing Library. README.md lists the commands.
:::

:::note
**Time limit:** 120 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

- A rider can reserve a bike from choosing a station to the confirmation using only the keyboard, with visible focus in a sensible order
- The station picker and any other hand-built control expose a role, a name, and a state, such as open or closed
- The reservation and confirmation pages have no WCAG 2 A or AA violations the automated scan can find
- A browser test completes a full reservation against a mock API and keeps a trace when it fails and is retried
- Every piece of text on both pages appears in English and German, and switching language changes it without a reload
- Sentences that carry a value read naturally in both languages, and a station's bike count reads correctly in each, such as 1 bike available and 2 bikes available
- Dates, times, and prices use the selected locale's format; the EUR 3.50 fee uses a decimal point in English and a comma in German
- A check fails when text exists in one language and not the other
- Component tests work through the page the way a rider does

### Theory: Rules

- React 19, Vitest 5, React Testing Library 16, user-event 14, jsdom 30, jest-dom 7, i18next 26, react-i18next 17, Playwright 1, axe-core 4 through @axe-core/playwright, MSW 3
- Two locales: en-US for English and de-DE for German; all fees remain in EUR when the locale changes
- Browser tests run in English; German is checked by the component tests
- Tests reach the API only through the shared MSW handlers (@msw/playwright or page.route in the browser); no test uses the real network
- The mock API's endpoints and response shapes are fixed; prices are numbers and times are ISO strings
- The station picker may stay a hand-built disclosure, a button that opens a list, or become a native select; a full combobox pattern is out of scope
- The submission is the edited project and its tests
- Before coding, write a short spec of the flow, each control's role, name, and state, and the focus order
- The spec also covers the text, dates, prices, and counts by locale and the checks that guard them; then build against it
- Out of scope: right-to-left languages, currency conversion, a designed language picker, visual-regression tests, styling beyond what keyboard focus needs, server code

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Test a critical user flow end to end in a real browser | A Playwright test completes the reservation from choosing a station to the confirmation, against a mock API; the configuration allows at least one retry and records a trace on retry, and the submission includes a trace from a deliberately failed run |
| D1-2 | Support keyboard-only use | A Playwright test completes the reservation with the keyboard alone, asserting the focus order and that the focused control is visible; this test may also satisfy D1-1 |
| D1-3 | Give hand-built controls the right role, name, and state | The station picker exposes a role, an accessible name, and, while it stays hand-built, its open or closed state, and a test checks each of them |
| D1-4 | Pass automated WCAG 2 AA checks | An axe scan through @axe-core/playwright with the wcag2a and wcag2aa tags covers every page of the flow, including the station picker while it is open, and reports zero violations |
| D1-5 | Externalize UI text for translation | No user-facing text is written straight into a component, every piece of text renders through a translation key with English and German resources, values enter sentences through interpolation and never by joining strings, a test switches the language and asserts the displayed text changes, and a check fails when a key is missing from either language, shown failing on a key removed from one of them |
| D1-6 | Localize dates, numbers, and plurals | Dates, times, and prices format through Intl or i18next formatting, and station availability counts use plural keys, with tests asserting different output in each locale and the singular and plural output in both |
| D1-7 | Test components the way a user uses them | Component tests use userEvent.setup() and find elements by role or label |

### Exercise: What to submit

- The edited project source, without `node_modules`
- The Playwright and Vitest tests, and the output of `pnpm test:e2e` and `pnpm test` showing them pass
- The trace file from a deliberately failed run
- The check that fails on a missing translation, and its output on a key removed from one language
- Your short spec, as a Markdown file in the project
