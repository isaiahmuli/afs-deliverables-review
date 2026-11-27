# Submission: FE-03 workspace booking

The spec I wrote before coding is in [`SPEC.md`](SPEC.md).

## What changed

- `src/routes.tsx`: nested routes under one `Layout`, with dynamic segments `:workspaceId` and
  `:bookingId`, loaders that fill the query cache, the confirmation's cancel action, and the root
  `errorElement`. `src/main.tsx` builds a browser router from it, and the tests build a memory
  router from the same routes.
- `src/Layout.tsx` and `src/ErrorPage.tsx`: the shared header and nav, and an error screen that
  keeps the header.
- `src/api/queryKeys.ts` and `src/api/queries.ts`: the key factory, and shared query options with
  a `staleTime` each, used by both loaders and screens. Also the booking mutation, and the
  optimistic cancel from My bookings.
- `src/api/client.ts`: `getHistory(cursor)` fetches one page instead of `limit=1000`.
- `src/screens/*`: the screens use the router (`Link`, `useParams`, `Form`) and the shared queries.
  The booking form uses React Hook Form with a Zod schema through `zodResolver`.
- `src/App.tsx` is removed, because the router replaces its view state.
- The mock API (`src/mocks/*`) is unchanged.

## Exit criteria

| ID | Where | Tests |
|---|---|---|
| D1-1 | `queryKeys.ts`, list loading and error states | `the list shows a loading state…`, `…an error state…`, `page, date and city each show their own results…` |
| D1-2 | `WORKSPACES_STALE_TIME` = 30 s | `coming back within staleTime sends no request; coming back later refetches` |
| D1-3 | `useCreateBooking` invalidates `['workspaces']` and `['bookings']` | `booking updates availability for that date only and My bookings…` |
| D1-4 | `useCancelBooking`: `cancelQueries`, `setQueryData`, rollback | `a refused cancel puts the booking back exactly as it was`, `a refetch already in flight…` |
| D1-5 | `historyQuery` with `getNextPageParam` | `history shows the first page and Load more appends…` |
| D1-6 | `routes.tsx`: layout, `:workspaceId`, `:bookingId`, `errorElement` | `each screen has its own address… Back/Forward…`, four `…works when opened directly`, `an address that matches no route…` |
| D1-7 | workspace and booking loaders (`ensureQueryData`), confirmation `action` | `the loader has the workspace ready… does not fetch again`, `a workspace that does not exist…`, `the loader turns a server 404…`, `the confirmation action cancels the booking…`, `a refused cancel from the confirmation…` |
| D1-8 | `bookingSchema` + `zodResolver`, `aria-describedby` per field | four `…is refused beside its field and nothing is sent`, `a valid booking is sent exactly once…`, `a taken workspace and date…` |

## Checking the tests catch the bugs

I broke the code on purpose in a scratch copy, one change at a time. Each of these made at least
one test fail:

- page dropped from the key
- `staleTime: 0` on the list
- no invalidation after booking
- no rollback, or no `cancelQueries`
- the action not invalidating
- no loader on the workspace route
- no `errorElement`
- a looser email rule, or no trim on the name
- the Book button not disabled while sending
- the email message not tied to its field
- history loading everything
- `placeholderData` keeping the previous page

One break needed a closer look. `staleTime: 0` on the workspace query passed, because
`useSuspenseQuery` raises `staleTime` to at least 1 s. With plain `useQuery` and `staleTime: 0`,
the "does not fetch again" test fails: the screen sends a second request after the loader's.

## `pnpm test`

```
 Test Files  7 passed (7)
      Tests  28 passed (28)
   Start at  20:21:15
   Duration  2.02s
```

`pnpm build` (`tsc -b && vite build`) passes too. I ran pnpm as `npx pnpm@12.23.0`, because pnpm
isn't installed here. I didn't click through the app in a browser. Everything above was checked
through the tests against MSW, using a memory router.
