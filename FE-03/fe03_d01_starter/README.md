# Workspace booking

A website where people find a workspace by city, book a day, and manage their bookings.
The mock API runs inside the browser in development (MSW) and inside the tests.

## Run

    pnpm install
    pnpm dev
    pnpm test

## What it does today

Browse: pick Nairobi and 2030-03-04. Click Book Nairobi Desk 1. It still shows as Available.
Open My bookings, return to Browse, book a desk, then open My bookings again. The new booking is missing until you reload the page.

Click Next, then Previous. The list for page 2 matches page 1. Change the date. The
availability does not change.

Open History, go to Browse and back. Every return sends the same request again.
History loads every past booking in one request.

Cancel a booking. It stays on screen until you reload.

If the mock API fails, the list stays blank with no message.
