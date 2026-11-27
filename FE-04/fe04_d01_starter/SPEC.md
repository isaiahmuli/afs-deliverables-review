# Spec: accessible, bilingual bike reservation

Written before coding.

## Flow

1. The Reserve page loads the stations (`GET /api/stations`), showing "Loading stations…" until
   they arrive.
2. The rider types their name, opens the station picker and chooses a station. The list closes,
   focus returns to the picker, and the reservation fee appears.
3. The rider presses Reserve. An empty name shows "Please enter your name" beside the field, and
   nothing is sent. Otherwise `POST /api/reservations` runs. If it fails, "Could not reserve. Try
   again." shows.
4. The Confirmation page shows. Focus moves to its heading. It shows the thank-you sentence with
   the pickup date and time, and the fee.

A Language select at the top (English, Deutsch) switches the language on both pages without a
reload. The `<html lang>` follows it (`en-US` or `de-DE`).

## Controls

| Control | Role | Accessible name | State |
|---|---|---|---|
| Language | native `select` with a `<label>` | "Language" / "Sprache" | native |
| Your name | `textbox` with a `<label>` | "Your name" / "Ihr Name" | `aria-invalid` and `aria-describedby` → error when empty |
| Station picker | `button` (disclosure) | "Station" + value, via `aria-labelledby` | `aria-expanded` true/false, `aria-controls` → list |
| Station list | `list` of `button`s | "Central Station (1 bike available)" | rendered only while open |
| Reserve | native `button` | "Reserve" / "Reservieren" (icon `aria-hidden`) | `disabled` until a station is chosen |
| Confirmation heading | `h1`, `tabIndex=-1` | "Reservation confirmed" | focused on arrival |

## Keyboard and focus

Tab order: Language → Your name → Station picker → (stations, while open) → Reserve. Enter or
Space opens the picker and focuses the first station. Escape closes it and returns focus to the
picker. Choosing a station returns focus to the picker. Every focused control shows a 3px
`:focus-visible` outline. The starter's `button { outline: none }` goes.

## Text by locale

Locales: `en-US` and `de-DE`. Resources are in `src/i18n/locales/*.json`, and every string comes
from `GLOSSARY.md`. Components never contain text, only `t('key')`.

| Thing | How | en-US | de-DE |
|---|---|---|---|
| Bike count | plural keys `bikesAvailable_one` / `_other` with `count` | 1 bike available / 2 bikes available | 1 Fahrrad verfügbar / 2 Fahrräder verfügbar |
| Station option | `{{station}} ({{availability}})`; availability is the plural text | Central Station (1 bike available) | Central Station (1 Fahrrad verfügbar) |
| Fee | `Intl.NumberFormat(locale, { style: 'currency', currency: 'EUR' })` | €3.50 | 3,50 € |
| Date | `Intl.DateTimeFormat(locale, { dateStyle: 'long', timeZone: 'UTC' })` | March 4, 2030 | 4. März 2030 |
| Time | `Intl.DateTimeFormat(locale, { timeStyle: 'short', timeZone: 'UTC' })` | 9:30 AM | 09:30 (sentence adds "Uhr") |
| Sentence | one key with `{{name}} {{station}} {{date}} {{time}}`, no string joining | Thank you, Ada. Your bike at Central Station is held until March 4, 2030 at 9:30 AM. | Danke, Ada. Ihr Fahrrad bei Central Station ist bis 4. März 2030 um 09:30 Uhr reserviert. |

The fee stays in EUR in both locales. Station names come from the API and aren't translated.

## Mock API in tests

- Vitest: the shared handlers through `msw/node` (already set up).
- Playwright: the shared handlers through `@msw/playwright` (it uses `page.route`). The app's own
  service worker would answer first and hide requests from `page.route`. So Playwright blocks
  service workers, and `main.tsx` renders even when the worker can't start.

## Checks

| Check | Where |
|---|---|
| Full reservation in a browser, with retry and trace on retry, and a saved failing trace | Playwright |
| Keyboard-only reservation: focus order and visible outline at each step | Playwright |
| Picker role, name, `aria-expanded` open and closed | Playwright |
| axe `wcag2a` + `wcag2aa`: page, picker open, station chosen, confirmation → 0 violations | Playwright |
| Switching language changes the text without a reload | Vitest |
| Plurals (1 and 2) in both locales, fee, date and time in both locales | Vitest |
| German render contains none of the English strings (nothing hard-coded) | Vitest |
| Key check: en and de have the same keys, and every key used in `src` exists in both. Shown failing on a removed key | Vitest |
| Name error beside its field with nothing sent, and failed reservation | Vitest |

Component tests use `userEvent.setup()` and find elements by role or label.
