# Spec: workspace booking

Written before coding. Server data lives only in the TanStack Query cache. Components keep only UI
selections (city, date, page) and form input. Every query key comes from `src/api/queryKeys.ts`.

## Routes

All screens are children of one layout route (`Layout`: header, nav, `<Outlet />`). The root
route's `errorElement` (`ErrorPage`) renders the same header, so error screens keep it too.

| Address | Screen | Loader | Action |
|---|---|---|---|
| `/` | WorkspaceList | none (needs city, date, page from UI state) | none |
| `/workspaces/:workspaceId` | WorkspaceDetail | `ensureQueryData(workspace)`; 404 → error screen | none |
| `/workspaces/:workspaceId/book` | BookingForm | same workspace loader | none (form uses a mutation) |
| `/bookings` | BookingList | none | none |
| `/bookings/:bookingId` | Confirmation | `ensureQueryData(booking)`; 404 → error screen | `cancel`: POST cancel, then invalidate |
| `/history` | History | none | none |
| anything else | ErrorPage "Page not found" (no route matches) | | |

- The form is reached by the "Book this workspace" link on the workspace page.
- Back and Forward work because every screen has an address (browser router in the app, memory
  router in tests).
- The client throws a `Response` on an HTTP error. A loader lets it propagate, so a missing
  workspace shows the error screen with status 404.

## Screen data and freshness

| Query | Key | staleTime | Why |
|---|---|---|---|
| Workspace list | `['workspaces', 'list', { city, date, page }]` | 30 s | availability changes, but not every second |
| Workspace | `['workspaces', 'detail', id]` | 5 min | names change rarely. Must be > 0, or the screen refetches what the loader just fetched |
| My bookings | `['bookings', 'list']` | 30 s | writes invalidate it |
| Booking | `['bookings', 'detail', id]` | 30 s | writes invalidate it |
| History | `['history']` (infinite, cursor) | 5 min | past bookings don't change |

- The list shows "Loading workspaces…" while it loads, and an alert with Try again if it fails. A
  new city, date or page shows a loading state, never the previous selection's results.
- Workspace and Confirmation read with `useSuspenseQuery`. The loader filled the cache, so there's
  no empty frame and no second request.

## Writes

| Write | Where | Effect |
|---|---|---|
| Book (`POST /api/bookings`) | form, mutation | on success invalidate `['workspaces']` and `['bookings']`, then go to `/bookings/:id` |
| Cancel from My bookings (`POST /api/bookings/:id/cancel`) | mutation, optimistic | cancel `['bookings']` queries, remove it from the list cache, restore the saved list on error, invalidate `['bookings']` and `['workspaces']` when settled |
| Cancel from Confirmation | route action (`<Form method="post">`) | POST cancel, await invalidation of `['bookings']` and `['workspaces']`. The page re-renders with status "cancelled" |

## Booking rules (Zod, through `zodResolver`)

- `name`: trimmed, at least 1 character → "Enter your name".
- `email`: a valid email → "Enter a valid email address".
- `date`: one of the offered dates (a select, so there's no rule to violate; the brief says no date
  rule).
- Each message shows beside its own field (`aria-describedby`, `aria-invalid`). When invalid,
  nothing is sent.
- A valid booking is sent once. The button is disabled while it's being sent.

## Failures

- Workspace list fails → alert + Try again.
- Unknown address → "Page not found". Missing workspace or booking → "Not found" with the 404.
- Booking refused (409, already booked) → a form-level alert "This workspace is already booked on
  that date". Other errors → "Booking failed".
- Cancel from My bookings refused → the row comes back where it was, plus an alert.
- Cancel from Confirmation refused → the action returns an error message, shown as an alert.

## Tests (Vitest, MSW only, fresh QueryClient per test, retries off, memory router)

Query criteria as before (keys, loading and error, staleTime, invalidation, optimistic cancel,
infinite history), plus routing (navigation, Back, unknown address), loaders (missing workspace,
no second fetch), the confirmation action, and the form (each rule blocks the send, a valid
booking is sent once).
