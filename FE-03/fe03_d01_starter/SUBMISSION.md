# Submission: FE-03 workspace booking

The spec I wrote before coding is in [`SPEC.md`](SPEC.md).

## What changed

- `src/api/queryKeys.ts`: one key factory. Workspace keys hold city, date and page.
- `src/api/queries.ts`: every query and mutation hook, each with its `staleTime`.
- `src/api/client.ts`: `getHistory(cursor)` fetches one page instead of `limit=1000`.
- `src/screens/*`: the screens use the hooks. Browse shows loading and error states, booking goes
  through a mutation, cancel is optimistic, and History has Load more.
- Tests: `test/utils.tsx`, `test/workspaces.test.tsx`, `test/bookings.test.tsx`, `test/history.test.tsx`.
- The mock API (`src/mocks/*`) is unchanged.

## Exit criteria

| ID | Where | Test |
|---|---|---|
| D1-1 | `queryKeys.ts`; no `useState` holds server data | `page, date and city each show their own results` |
| D1-2 | `test/utils.tsx` makes a fresh client per test with `retry: false` | `shows a loading state…`, `shows an error state…` |
| D1-3 | `WORKSPACES_STALE_TIME` = 30 s | `coming back within staleTime sends no request; coming back later refetches` |
| D1-4 | `useCreateBooking` invalidates `['workspaces']` and `['bookings']` | `booking updates availability for that date only and the booking list…` |
| D1-5 | `useCancelBooking`: `cancelQueries`, `setQueryData`, rollback in `onError` | `a refused cancel puts the booking back exactly as it was`, `a refetch already in flight cannot bring a cancelled booking back` |
| D1-6 | `useHistory` with `getNextPageParam: last => last.nextCursor` | `history shows the first page and Load more appends…` |

## Checking the tests catch the bugs

I broke the code on purpose in a scratch copy and reran the suite. Each break made at least one
test fail: page or date missing from the key, no invalidation after booking or cancel, no
rollback, no `cancelQueries`, no optimistic removal, `staleTime: 0`, and `placeholderData` keeping
the previous page on screen.

## `pnpm test`

```
$ vitest run
 Test Files  4 passed (4)
      Tests  10 passed (10)
   Start at  17:20:48
   Duration  1.27s
```

`pnpm build` (`tsc -b && vite build`) also passes. I used pnpm 12.23.0 through `npx pnpm@12.23.0`,
because pnpm is not installed by default and Node 25 no longer ships corepack.

I didn't click through the app in a browser. All of the behaviour above was checked through the tests against MSW.
