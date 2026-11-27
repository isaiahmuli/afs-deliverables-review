# Submission: FE-04 bike reservation

The spec I wrote before coding is in [`SPEC.md`](SPEC.md).

## What changed

- **Accessibility (`src/screens/Reserve.tsx`, `Confirmation.tsx`, `index.css`):**
  - The station picker is a `<button>` disclosure with `aria-expanded` and `aria-controls`. Its
    name is "Station + value". The stations in its list are buttons.
  - Reserve is a native button with visible text, and it stays disabled until a station is chosen.
  - The name error is tied to its field (`aria-describedby`, `aria-invalid`).
  - There's a 3px `:focus-visible` outline instead of `outline: none`.
  - The confirmation heading takes focus.
- **Translation (`src/i18n/`):**
  - i18next and react-i18next, with `en-US.json` and `de-DE.json` taken from `GLOSSARY.md`.
  - Bike counts use plural keys (`bikesAvailable_one` / `_other`).
  - Sentences take values through interpolation.
  - `format.ts` formats the fee, date and time with `Intl` for the active locale, in UTC.
  - A Language select switches without a reload and keeps `<html lang>` in step.
- **Browser tests:** `playwright.config.ts` has `retries: 1`, `trace: 'on-first-retry'` and
  `serviceWorkers: 'block'`. `e2e/fixtures.ts` serves the shared handlers through
  `@msw/playwright` (0.7.0, added). `src/main.tsx` renders even when the worker can't start.
- **Other:** `tsconfig.json` has `resolveJsonModule`. I deleted the `tsconfig.tsbuildinfo` that
  came in the zip. The mock API is unchanged.

## Exit criteria

| ID | Where | Tests |
|---|---|---|
| D1-1 | `e2e/reservation.spec.ts`, `playwright.config.ts` | `D1-1: a rider reserves a bike…`, plus `traces/failed-run-trace.zip` |
| D1-2 | `expectVisibleFocus` (focused, outline ≥ 2px) | `D1-2: the reservation works with the keyboard alone…` (Language → name → picker → stations → Reserve → heading) |
| D1-3 | picker button with `aria-expanded` and `aria-labelledby` | `D1-3: the station picker exposes its role, name and open state`, Vitest `the picker is a button…` |
| D1-4 | axe `wcag2a` + `wcag2aa` | `D1-4: …` covers the page, the picker open, the name error, and the confirmation |
| D1-5 | `src/i18n/locales/*.json`, `t()` everywhere | `switching language changes the text without a reload`, `in German, no English text is left on either page`, `test/translations.test.ts` |
| D1-6 | `src/i18n/format.ts`, plural keys | `…bike counts use the right plural for 1 and for 2 or more` and `…fee, date and time use the locale format…`, each for en-US and de-DE |
| D1-7 | `userEvent.setup()`, queries by role and label only | all of `test/*.test.tsx` |

| | en-US | de-DE |
|---|---|---|
| 1 bike / 2 bikes | 1 bike available / 2 bikes available | 1 Fahrrad verfügbar / 2 Fahrräder verfügbar |
| Fee | Reservation fee: €3.50 | Reservierungsgebühr: 3,50 € |
| Sentence | Thank you, Ada. Your bike at Central Station is held until March 4, 2030 at 9:30 AM. | Danke, Ada. Ihr Fahrrad bei Central Station ist bis 4. März 2030 um 09:30 Uhr reserviert. |

## The missing-translation check

`test/translations.test.ts` has three tests:

1. en-US and de-DE must have the same keys.
2. Every key literal in `src` must exist in both. A plural key counts if its `_one` and `_other`
   forms exist.
3. The checker must report a removed key.

This is its output after I removed `reserve.failed` from `de-DE.json`. I restored the key
afterwards.

```
 FAIL  test/translations.test.ts > English and German have exactly the same keys
AssertionError: keys missing from de-DE: expected [ 'reserve.failed' ] to deeply equal []
 FAIL  test/translations.test.ts > every key used in src exists in both languages
AssertionError: keys used in src but missing from de-DE: expected [ 'reserve.failed' ] to deeply equal []
 Test Files  1 failed | 3 passed (4)
```

Without the check, nothing would show the gap, because `fallbackLng` would quietly show the
English text on the German page.

## The deliberately failed run

I removed the station from the confirmation sentence and ran `pnpm test:e2e -g "D1-1"`. The test
failed with `expect(locator).toBeVisible() failed` and retried once, and Playwright kept the
retry's trace. I copied it to `traces/failed-run-trace.zip` (97 KB), because `.gitignore`
excludes `test-results/`, and restored the code. Open it with:

    pnpm exec playwright show-trace traces/failed-run-trace.zip

## Checking the tests catch the bugs

In a scratch copy I broke the code on purpose, one change at a time. Each of these made at least
one test fail:

- a hard-coded "Reserve" label or loading text
- no plural (always "bikes")
- the fee built with `toFixed`
- no `timeZone: 'UTC'` (also with `TZ=UTC`)
- the date always in en-US
- the name error not tied to its field
- no trim on the name
- `outline: none`
- no `aria-expanded`
- no focus on the confirmation

The tests set `TZ=Pacific/Auckland`, so a missing `timeZone: 'UTC'` fails on every machine.

## `pnpm test`

```
 Test Files  4 passed (4)
      Tests  17 passed (17)
```

## `pnpm test:e2e`

```
  ✓  2 [chromium] › e2e/smoke.spec.ts:4:1 › the reservation page loads
  ✓  1 [chromium] › e2e/reservation.spec.ts:6:1 › D1-1: a rider reserves a bike from choosing a station to the confirmation
  ✓  3 [chromium] › e2e/reservation.spec.ts:20:1 › D1-2: the reservation works with the keyboard alone, in order, with visible focus
  ✓  4 [chromium] › e2e/reservation.spec.ts:48:1 › D1-3: the station picker exposes its role, name and open state
  ✓  5 [chromium] › e2e/reservation.spec.ts:70:1 › D1-4: every page of the flow, including the open picker, has no WCAG 2 A/AA violations
  ✓  6 [chromium] › e2e/reservation.spec.ts:90:1 › a refused reservation tells the rider and stays on the page
  6 passed
```

`pnpm build` passes too. I didn't test with a real screen reader. Role, name and state were
checked through Testing Library and Playwright's accessibility queries, and through axe.
