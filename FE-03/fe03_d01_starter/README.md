# Workspace booking

A website where people find a workspace by city, book a day, and manage their bookings.
The mock API runs inside the browser in development (MSW) and inside the tests.
React Router, React Hook Form and Zod are installed but not set up.

## Run

    pnpm install
    pnpm dev
    pnpm test

## What it does today

Browse: pick Nairobi and 2030-03-04. Open Nairobi Desk 1, book it for 2030-03-04, then return to
Browse. It still shows as Available. Open My bookings, book a desk, and open My bookings again.
The new booking is missing until you reload the page.

Click Next, then Previous. The list for page 2 matches page 1. Change the date. The
availability does not change. Leave Browse and come back. The same request is sent again.

Open History. Every past booking loads in one request.

Cancel a booking from My bookings. It stays on screen until you reload.

If the mock API fails, the list stays blank with no message.

Open a workspace. The address stays `/`. Reload the page and you are back on the list. The
browser Back button leaves the site. While a workspace loads, its screen is empty. A workspace
that does not exist leaves it blank.

Open the booking form and click Book with nothing filled in. A booking with a blank name and
email is sent to the server. So is an email such as `not-an-email`.

After a booking, the confirmation appears. Click Cancel booking. The status stays confirmed
until you reload.
