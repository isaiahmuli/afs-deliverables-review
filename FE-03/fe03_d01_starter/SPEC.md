# Data spec: workspace booking

Written before coding. All server data lives in the TanStack Query cache; components only keep
UI selections (city, date, page, tab) in state. Every query key comes from `src/api/queryKeys.ts`.

## Screens

### Browse (WorkspaceList)

- **Data:** `GET /api/workspaces?city&date&page`, one page of workspaces with `available` for that date.
- **Key:** `['workspaces', { city, date, page }]`. City, date and page all change the response, so all
  three are in the key. No `placeholderData`: a new selection shows a loading state, never the
  previous selection's results.
- **Freshness:** `staleTime` 30 s. Leaving Browse and coming back within 30 s makes no request; later
  than that the cached list shows and is refetched in the background.
- **States:** "Loading workspaces…" until the first response; on failure an alert
  "Could not load workspaces." with a Try again button.
- **Writes:** Book (`POST /api/bookings`). On success, invalidate every `['workspaces']` key (the
  server recomputes availability per date, so only the booked date changes) and `['bookings']`.
  On failure (409 already booked, 404) show "Booking failed" and leave the cache alone.

### My bookings (BookingList)

- **Data:** `GET /api/bookings`. **Key:** `['bookings']`. **Freshness:** `staleTime` 30 s; writes keep it
  correct through invalidation, so it does not need `Infinity`.
- **States:** loading text; error alert.
- **Writes:** Cancel (`DELETE /api/bookings/:id`), optimistic:
  1. cancel in-flight `['bookings']` queries so a late reply cannot overwrite the change,
  2. save the current list, remove the booking from the cache,
  3. on error put the saved list back (same items, same order) and show "Could not cancel",
  4. on settle invalidate `['bookings']` and `['workspaces']` (the desk is free again on that date).

### History

- **Data:** `GET /api/bookings/history?cursor`, pages of 5 with `nextCursor` (null on the last page).
- **Key:** `['history']`. Infinite query: first page param `null`, next param = `nextCursor`.
- **Freshness:** past bookings do not change; `staleTime` 5 min.
- **States:** loading text; error alert. "Load more" appends the next page below; it disappears when
  `nextCursor` is null and reads "Loading…" while a page is in flight.
- **Writes:** none.

## Tests (MSW only, fresh QueryClient per test, retries off)

Selection changes, loading and error, staleTime remount and later refetch, booking updates
Browse and My bookings, optimistic cancel and rollback on a 500, history Load more.
