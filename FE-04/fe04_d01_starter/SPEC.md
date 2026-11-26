# Spec: accessible bike reservation

Written before coding.

## Flow

1. The Reserve page loads the stations (`GET /api/stations`).
2. The rider opens the station picker and chooses a station. The list closes, and focus goes back
   to the picker, which now shows the station.
3. The bikes for that station load (`GET /api/stations/:id/bikes`). The Bike select appears with
   the first bike chosen.
4. The rider presses Reserve (`POST /api/reservations`).
5. The Confirmation page shows. Focus moves to its heading, so keyboard and screen-reader users
   land on the result.

## Controls

| Control | Role | Accessible name | State |
|---|---|---|---|
| Station picker | `button` (disclosure) | "Station" + current value, via `aria-labelledby` (e.g. "Station Choose a station") | `aria-expanded` true/false; `aria-controls` points at the list |
| Station list | `list` of `button`s, one per station | "Central Station, 3 bikes available" (the visible text) | only rendered while open |
| Bike | native `select` with a `<label>` | "Bike" | native |
| Reserve | native `button` | "Reserve" (visible text; the tick icon is `aria-hidden`) | `disabled` until a station and bike are chosen |
| Confirmation heading | `h1`, `tabIndex=-1` | "Reservation confirmed" | gets focus on arrival |

I'm keeping the picker as a hand-built disclosure, because a full combobox is out of scope.

## Keyboard and focus

- Tab order: station picker → (station buttons, while open) → Bike → Reserve.
- Enter or Space on the picker toggles the list. When it opens, focus moves to the first station.
  Escape closes it and returns focus to the picker.
- Choosing a station closes the list and returns focus to the picker.
- Every focused control shows a visible outline (`:focus-visible`, 3px solid, contrasting). The
  starter's `button { outline: none }` goes.

## Mock API in tests

- Tests use `@msw/playwright` with the shared handlers from `src/mocks/handlers.ts` (it works
  through `page.route`).
- The app's own service worker would answer first and hide every request from `page.route`. So
  Playwright blocks service workers (`serviceWorkers: 'block'`), and `main.tsx` renders even when
  the worker can't start.

## Checks

- **D1-1:** mouse/role test reserves Bike 21 at Harbour Gate and sees the confirmation.
- **D1-3:** keyboard-only test using Tab, Enter, arrow keys and Space. It asserts each focused
  control in order, and checks that each has a visible outline.
- **D1-4:** picker test checks the role `button`, the name, and `aria-expanded` false → true →
  false (Escape).
- **D1-5:** axe with `wcag2a` and `wcag2aa` on the Reserve page closed, with the picker open,
  after choosing a station, and on Confirmation. Each must report 0 violations.
- **D1-2:** `retries: 1` and `trace: 'on-first-retry'`. A trace from a deliberately failed run is
  saved in `traces/`.
