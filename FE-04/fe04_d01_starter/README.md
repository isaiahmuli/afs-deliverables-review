# Bike reservation

A page where a rider picks a station, chooses a bike, and reserves it.
The mock API runs in the browser through MSW, so `pnpm dev` and the Playwright tests use the same handlers.

## Run

    pnpm install
    pnpm exec playwright install chromium
    pnpm dev
    pnpm test:e2e

## What it does today

Press Tab from the top of the page. Focus never reaches the station picker or the Reserve control.
Click the picker with a mouse and choose a station. Keyboard focus is not visible on any button.

With a screen reader, the Reserve control is announced with no name. The station picker does not
say whether its list is open.

No test reserves a bike. Playwright has no trace setting.
