# Bike reservation

A page where a rider enters a name, picks a station, and reserves a bike, then sees a confirmation.
The mock API runs in the browser through MSW, so `pnpm dev` and the Playwright tests share the same
handlers. In component tests the same handlers run in Node.
i18next and react-i18next are installed but not set up. `GLOSSARY.md` holds the German text.

## Run

    pnpm install
    pnpm exec playwright install chromium
    pnpm dev
    pnpm test
    pnpm test:e2e

## What it does today

Press Tab from the top of the page. Focus reaches the name field, then never reaches the station picker
or the Reserve control. Click the picker with a mouse and choose a station. Keyboard focus is not visible
on any button.

With a screen reader, the Reserve control is announced with no name. The station picker does not say
whether its list is open.

All text is English and written inside the components. There is no way to switch language.
Central Station shows `1 bikes available`. The fee reads `EUR 3.50`. The confirmation date reads
`03/04/2030 at 9:30 AM`.

No test reserves a bike. Playwright has no trace setting.

Dates and times are in UTC. Use `timeZone: 'UTC'` when you format them, so tests give the same output on
every machine.
