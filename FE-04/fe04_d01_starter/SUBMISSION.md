# Submission: FE-04 bike reservation

The spec I wrote before coding is in [`SPEC.md`](SPEC.md).

## What changed

- `src/screens/Reserve.tsx`: the station picker is now a real `<button>` disclosure. It has
  `aria-expanded`, `aria-controls` and the name "Station + value". The station list items are
  buttons. Opening the list focuses the first station, and Escape or choosing a station returns
  focus to the picker. Reserve is a native button named "Reserve" (the icon is `aria-hidden`), and
  it stays disabled until a station and bike are chosen. Errors show in a `role="alert"`.
- `src/screens/Confirmation.tsx`: the heading takes focus on arrival.
- `src/index.css`: removed `button { outline: none }` and added a 3px `:focus-visible` outline.
- `src/main.tsx`: the app renders even if the MSW worker can't start (see below).
- `playwright.config.ts`: `retries: 1`, `trace: 'on-first-retry'`, `serviceWorkers: 'block'`.
- `e2e/fixtures.ts`: the shared handlers through `@msw/playwright` (0.7.0, added), plus the focus
  and axe helpers. `e2e/reservation.spec.ts` holds the tests.
- The mock API (`src/mocks/*`) is unchanged.

### Why service workers are blocked

The rules say tests get the mock API through `@msw/playwright` or `page.route`. But while the
app's own MSW service worker runs, it answers first, and `page.route` never sees a request. A
probe route counted 0 hits and the page showed the worker's data. So Playwright blocks service
workers. Blocking makes `worker.start()` reject, and the starter's `main.tsx` waited on it before
rendering. Without the try/catch, all 6 tests fail with "element(s) not found". `pnpm dev` still
uses the worker as before.

## Exit criteria

| ID | Where | Test |
|---|---|---|
| D1-1 | `e2e/reservation.spec.ts` | `D1-1: a rider reserves a bike…` (Harbour Gate, Bike 22) |
| D1-2 | `playwright.config.ts` | `traces/failed-run-trace.zip`, see below |
| D1-3 | `expectVisibleFocus` checks focus and a ≥2px outline at each step | `D1-3: the reservation works with the keyboard alone…` |
| D1-4 | picker `button` + `aria-expanded` + `aria-labelledby` | `D1-4: the station picker exposes its role, name and open state` |
| D1-5 | `expectNoAxeViolations` with `wcag2a` and `wcag2aa` | `D1-5: …including the open picker…`: page closed, picker open, station chosen, confirmation |

There is also a test where the API refuses the reservation (500 via `network.use`). It checks that
the rider sees an alert and stays on the page.

## The deliberately failed run (D1-2)

I broke the confirmation step on purpose. It said "Bike 22 is held for you." and dropped the
station. Then I ran `pnpm test:e2e -g "D1-1"`. The test failed, retried once, failed again, and
Playwright kept the retry's trace. I copied it to `traces/failed-run-trace.zip` (66 KB), because
the starter's `.gitignore` excludes `test-results/`, and restored the code. Open it with:

    pnpm exec playwright show-trace traces/failed-run-trace.zip

The trace's error is `expect(locator).toBeVisible() failed … getByText('Bike 22 is held for you at Harbour Gate.')`.

## Checking the tests catch the bugs

In a scratch copy I put back each starter defect separately. Every one made at least one test
fail:

| Defect | Failing tests |
|---|---|
| `outline: none` on buttons | D1-3 |
| No `aria-expanded` | D1-4, D1-5 |
| No focus on the confirmation | D1-3 |
| Unnamed Reserve | 5 tests |

## `pnpm test:e2e`

```
Running 6 tests using 2 workers
  ✓  1 [chromium] › e2e/smoke.spec.ts:4:1 › the reservation page loads (333ms)
  ✓  2 [chromium] › e2e/reservation.spec.ts:4:1 › D1-1: a rider reserves a bike from choosing a station to the confirmation (497ms)
  ✓  3 [chromium] › e2e/reservation.spec.ts:15:1 › D1-3: the reservation works with the keyboard alone, in order, with visible focus (352ms)
  ✓  4 [chromium] › e2e/reservation.spec.ts:48:1 › D1-4: the station picker exposes its role, name and open state (344ms)
  ✓  5 [chromium] › e2e/reservation.spec.ts:70:1 › D1-5: every page of the flow, including the open picker, has no WCAG 2 A/AA violations (1.4s)
  ✓  6 [chromium] › e2e/reservation.spec.ts:88:1 › a failed reservation tells the rider and stays on the page (453ms)

  6 passed (4.3s)
```

`pnpm build` passes too. I ran pnpm as `npx pnpm@12.23.0`, because pnpm isn't installed here.

I didn't test with a real screen reader. Role, name and state were checked through Playwright's
accessibility queries and axe.
