---
module: FE-03
code: FE-03
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Make workspace availability and bookings stay in sync
subtitle: Fix a booking site whose screens drift from the server, and add paging to booking history.
time_budget_min: 90
status: draft
---

## Topic: Deliverable

### Exercise: Fix and extend the booking site

A workspace-booking website lets people find a workspace by city, book a day, and manage their bookings. Its screens do not stay in step with the server. After someone books a workspace, it still shows as available for that date. The new booking appears only when they reload the page.

The same gap shows up wherever people browse. Moving to the next page of a city's workspaces brings back the same results. Returning to a recently viewed list sends the same request again. When the server is down, the list stays blank with no message. Cancelling a booking gives no immediate feedback, and opening booking history downloads every past booking at once.

Keep availability and booking details consistent after booking changes, without asking people to reload the page. Show changes promptly, restore a booking if cancellation fails, and let people load older bookings when they want them.

What the project does today:

- Browse Nairobi on 2030-03-04 and book Nairobi Desk 1. It still reads Available.
- Click Next, then Previous. Page 2 shows the same workspaces as page 1. Changing the date does not change availability.
- Leave Browse and come back. The same request is sent again.
- Open My bookings once, book a desk, and open My bookings again. The new booking is missing until you reload.
- Cancel a booking. It stays on screen.
- Open History. All 12 past bookings load in one request.
- Make the workspaces request fail. The list is blank, with no message.

:::download ./assets/fe03_d01_starter.zip Booking site starter
A Vite + React + TypeScript project with browse, booking and history screens. TanStack Query and a mock API with MSW test setup are wired. README.md lists the commands.
:::

:::note
**Time limit:** 90 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

- Switching city, date, or page shows matching workspaces and availability for the selected date, never results from another selection
- The workspace list shows a loading state while it loads and a clear error state when the API fails
- Leaving the workspace list and coming back within the chosen freshness window makes no new request; a later return updates older results from the server
- Booking a workspace updates its availability for the booked date and the booking list without a browser reload
- Cancelling a booking removes it from the screen immediately, and if the server refuses, it reappears exactly as it was
- The booking history shows its first page, a Load more action adds the next page below it, and the action goes away when no pages remain
- Tests exercise all of the above against a mock API

A booking on 2030-03-04 makes that workspace unavailable on 2030-03-04 only. The same workspace stays available on 2030-03-05.

### Theory: Rules

- React 19, TypeScript 6, TanStack Query 5, MSW 3, Vitest 5
- Tests reach the API only through MSW handlers; no test uses the real network
- The mock API's endpoints and response shapes are fixed; changing them is out of scope
- Availability is specific to the selected booking date; a booking does not make the workspace unavailable on other dates
- Before coding, write a short spec of each screen's data, how fresh it must be, what each write changes, and each failure, then build against it
- The submission is the edited project and its tests
- Out of scope: routing, forms, sign-in, styling, server code

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Read server state with queries keyed by every request variable | Query keys come from one shared factory and include every request dependency, such as city, selected date, and page where applicable, and no server data is copied into component state or a store |
| D1-2 | Test loading and error states against a mock API | Tests use a fresh query client per test with retries off and a mock API, and cover a loading state and an error state |
| D1-3 | Choose how long saved data stays fresh | Queries set a staleTime, and a test shows a remount within it sends no new request |
| D1-4 | Keep other screens in step after a write | A mutation invalidates the affected query keys on success, and a test shows a screen that displays that data changes without a reload |
| D1-5 | Undo an optimistic update when the server refuses it | An optimistic update cancels in-flight queries for the affected data, changes the cache before the reply, and a test shows the original data restored when the mock API returns an error |
| D1-6 | Page through a long list with an infinite query | An infinite query with getNextPageParam loads and renders the next page on request, and a test shows page 2 appended below page 1 |

### Exercise: What to submit

- The edited project source, without `node_modules`
- The tests, and the output of `pnpm test` showing them pass
- Your short spec, as a Markdown file in the project
