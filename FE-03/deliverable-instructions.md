---
module: FE-03
code: FE-03
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Make the booking site stay in sync, shareable, and safe to submit
subtitle: Fix a booking site whose screens drift from the server, give each screen its own address, and check the booking form before it is sent.
time_budget_min: 120
status: draft
---

## Topic: Deliverable

### Exercise: Fix and extend the booking site

A workspace-booking website lets people find a workspace by city, book a day, and manage their bookings. Its screens drift from the server. A booked workspace still shows as available until a reload. Paging returns the same results, and a failed request leaves a blank list. History downloads every booking at once.

The site also never changes its address, so people cannot share a workspace link or use Back. The booking form sends blank names and invalid emails, and a removed workspace or mistyped address leaves a blank page.

Keep availability and bookings consistent without reloads. Let people open and share a workspace directly, book from its page with clear field errors, and cancel from the confirmation. Restore a booking if its cancellation is refused, and load older bookings on request.

What the project does today:

- Book Nairobi Desk 1 for 2030-03-04 and return to Browse: it still reads Available.
- Next then Previous shows page 1 again; changing the date changes nothing.
- Returning to Browse repeats the request; a new booking is missing from My bookings until reload.
- Cancelling from My bookings or the confirmation changes nothing on screen.
- History loads all 12 past bookings in one request.
- Opening a workspace keeps the address at `/`; reload returns to the list, and Back leaves the site.
- A workspace that does not exist is blank. An empty form, or `not-an-email`, is sent.

:::download ./assets/fe03_d01_starter.zip Booking site starter
A Vite + React + TypeScript project with every screen, TanStack Query, and a mock API with MSW test setup. README.md lists the commands.
:::

:::note
**Time limit:** 120 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

- Switching city, date, or page shows matching workspaces and availability for the selected date, never results from another selection
- The workspace list shows a loading state while it loads and a clear error state when the API fails
- Leaving the workspace list and coming back within the chosen freshness window makes no new request; a later return updates older results from the server
- Booking a workspace updates its availability for the booked date and the booking list without a browser reload
- Cancelling a booking from the booking list removes it from the screen immediately, and if the server refuses, it reappears exactly as it was
- The booking history shows its first page, a Load more action adds the next page below it, and the action goes away when no pages remain
- The workspace list, a single workspace, its booking form, and a booking's confirmation each have their own address that works when opened directly, and the form is reached from the workspace's page
- Every screen shows the same header and navigation, and the Back and Forward buttons move between screens inside the app
- An address that matches no route, and a workspace that does not exist, each show an error screen rather than a blank one
- A workspace's details are ready when its screen first appears, with no empty frame while it loads
- An empty name or a malformed email is refused with a message beside its own field, and nothing is sent
- A valid booking is sent exactly once and shows a confirmation for the selected workspace and date
- The confirmation has a cancel form; submitting it cancels the booking and the screen shows it as cancelled
- Tests exercise all of the above against a mock API

A booking on 2030-03-04 does not affect 2030-03-05. The booking form has no date rule.

### Theory: Rules

- React 19, TypeScript 6, TanStack Query 5, React Router 7, React Hook Form 7, Zod 4, @hookform/resolvers 5, MSW 3, Vitest 5
- Tests reach the API only through MSW handlers; no test uses the real network
- The mock API's endpoints and response shapes are fixed; changing them is out of scope
- Availability is specific to the selected booking date; a booking does not make the workspace unavailable on other dates
- Before coding, write a short spec of each screen's data, how fresh it must be, and what each write changes
- The spec also covers the routes, the booking rules, and each failure or error screen; then build against it
- The submission is the edited project and its tests
- Out of scope: sign-in, styling, server code

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Read server state with queries keyed by every request variable | Query keys come from one shared factory and include every request dependency, such as city, selected date, and page where applicable, no server data is copied into component state or a store, and tests with a fresh query client per test, retries off, and a mock API cover a loading state and an error state |
| D1-2 | Choose how long saved data stays fresh | Queries set a staleTime, and a test shows a remount within it sends no new request |
| D1-3 | Keep other screens in step after a write | A mutation invalidates the affected query keys on success, and a test shows a screen that displays that data changes without a reload |
| D1-4 | Undo an optimistic update when the server refuses it | An optimistic update cancels in-flight queries for the affected data, changes the cache before the reply, and a test shows the original data restored when the mock API returns an error |
| D1-5 | Page through a long list with an infinite query | An infinite query with getNextPageParam loads and renders the next page on request, and a test shows page 2 appended below page 1 |
| D1-6 | Map addresses to nested routes with a dynamic segment and an error route | A browser router defines nested routes under one shared layout, dynamic segments for a single workspace and a single booking, and an error element that shows for an unmatched address; tests built on a memory router cover navigation between screens and an unknown address |
| D1-7 | Load a route's data before it renders and handle a form with a route action | At least one route has a loader that fills the query cache before it renders, the screen reads the same query without fetching again, and the loader throws a 404 response for a workspace that does not exist; the confirmation route has an action that receives the cancel form, sends the cancellation, and invalidates the affected queries so the rerun loader shows the booking as cancelled; memory-router tests cover a missing workspace, the loader, and the action |
| D1-8 | Validate a form with a schema and show errors by field | A Zod schema wired through zodResolver blocks an invalid submission and shows each message beside the field that caused it; form tests submit an invalid value for each schema rule and assert the message shows and nothing is sent, and a valid submission is sent exactly once |

### Exercise: What to submit

- The edited project source, without `node_modules`
- The tests, and the output of `pnpm test` showing them pass
- Your short spec, as a Markdown file in the project
