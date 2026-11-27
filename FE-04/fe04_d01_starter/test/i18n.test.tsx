import { screen, within } from '@testing-library/react'
import en from '../src/i18n/locales/en-US.json'
import de from '../src/i18n/locales/de-DE.json'
import i18n from '../src/i18n'
import { renderApp, reserveAs } from './utils'

const locales = {
  'en-US': {
    nameLabel: 'Your name',
    reserveLabel: 'Reserve',
    options: ['Central Station (1 bike available)', 'Harbour Gate (2 bikes available)', 'Park Square (5 bikes available)'],
    fee: 'Reservation fee: €3.50',
    sentence: 'Thank you, Ada. Your bike at Central Station is held until March 4, 2030 at 9:30 AM.',
  },
  'de-DE': {
    nameLabel: 'Ihr Name',
    reserveLabel: 'Reservieren',
    options: ['Central Station (1 Fahrrad verfügbar)', 'Harbour Gate (2 Fahrräder verfügbar)', 'Park Square (5 Fahrräder verfügbar)'],
    fee: 'Reservierungsgebühr: 3,50 €',
    sentence: 'Danke, Ada. Ihr Fahrrad bei Central Station ist bis 4. März 2030 um 09:30 Uhr reserviert.',
  },
} as const

test('switching language changes the text without a reload', async () => {
  const { user } = renderApp()
  await screen.findByRole('button', { name: /^Station/ })
  await user.type(screen.getByLabelText('Your name'), 'Ada')

  await user.selectOptions(screen.getByLabelText('Language'), 'de-DE')
  expect(screen.getByRole('heading', { name: 'Fahrrad reservieren' })).toBeInTheDocument()
  expect(screen.getByLabelText('Ihr Name')).toHaveValue('Ada') // same page, nothing reloaded
  expect(screen.getByRole('button', { name: 'Station Station wählen' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Reservieren' })).toBeInTheDocument()
  expect(document.documentElement.lang).toBe('de-DE')

  await user.selectOptions(screen.getByLabelText('Sprache'), 'en-US')
  expect(screen.getByRole('heading', { name: 'Reserve a bike' })).toBeInTheDocument()
  expect(document.documentElement.lang).toBe('en-US')
})

test.each(Object.entries(locales))('%s: bike counts use the right plural for 1 and for 2 or more', async (locale, l) => {
  const { user } = renderApp()
  await user.selectOptions(screen.getByLabelText(/Language|Sprache/), locale)
  await user.click(await screen.findByRole('button', { name: /^Station/ }))
  const options = within(screen.getByRole('list')).getAllByRole('button').map((b) => b.textContent)
  expect(options).toEqual(l.options)
})

test.each(Object.entries(locales))('%s: fee, date and time use the locale format in the sentences', async (locale, l) => {
  const { user } = renderApp()
  await user.selectOptions(screen.getByLabelText(/Language|Sprache/), locale)
  await user.type(screen.getByLabelText(l.nameLabel), 'Ada')
  await user.click(await screen.findByRole('button', { name: /^Station/ }))
  await user.click(screen.getByRole('button', { name: new RegExp(`^${l.options[0].replace(/[()]/g, '\\$&')}`) }))
  expect(screen.getByText(l.fee)).toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: l.reserveLabel }))
  expect(await screen.findByText(l.sentence)).toBeInTheDocument()
  expect(screen.getByText(l.fee)).toBeInTheDocument()
})

// Every English string whose German differs must be gone from the German page,
// including accessible names. Anything left over was written into a component.
test('in German, no English text is left on either page', async () => {
  const leaves = (o: object, p = ''): [string, string][] =>
    Object.entries(o).flatMap(([k, v]) => (typeof v === 'string' ? [[p + k, v]] : leaves(v, `${p}${k}.`)))
  const deByKey = new Map(leaves(de))
  const english = leaves(en)
    .filter(([k, v]) => deByKey.get(k) !== v)
    .map(([, v]) => v.split(/\{\{[^}]+\}\}/)) // the literal parts around values
    .flat()
    .map((s) => s.trim())
    .filter((s) => s.length > 3)

  const pageText = () =>
    [document.body.textContent ?? '', ...[...document.querySelectorAll('[aria-label]')].map((e) => e.getAttribute('aria-label'))].join('\n')
  const leftovers = (text: string) => english.filter((s) => text.includes(s))

  // Start in German, so the loading state is checked too.
  await i18n.changeLanguage('de-DE')
  const { user } = renderApp()
  expect(screen.getByText('Stationen werden geladen…')).toBeInTheDocument()
  expect(leftovers(pageText())).toEqual([])
  await user.click(await screen.findByRole('button', { name: /^Station/ }))
  expect(leftovers(pageText())).toEqual([])

  await user.click(screen.getByRole('button', { name: /^Central Station/ }))
  await user.click(screen.getByRole('button', { name: 'Reservieren' })) // empty name: error shows
  expect(screen.getByRole('alert')).toHaveTextContent('Bitte geben Sie Ihren Namen ein')
  expect(leftovers(pageText())).toEqual([])

  await user.type(screen.getByLabelText('Ihr Name'), 'Ada')
  await user.click(screen.getByRole('button', { name: 'Reservieren' }))
  await screen.findByRole('heading', { name: 'Reservierung bestätigt' })
  expect(leftovers(pageText())).toEqual([])
})

test('the reservation flow works in German end to end', async () => {
  const { user } = renderApp()
  await user.selectOptions(screen.getByLabelText('Language'), 'de-DE')
  await reserveAs(user, { nameLabel: 'Ihr Name', station: /^Harbour Gate/, reserveLabel: 'Reservieren', name: 'Jonas' })
  expect(await screen.findByRole('heading', { name: 'Reservierung bestätigt' })).toHaveFocus()
  expect(screen.getByText(/Danke, Jonas\. Ihr Fahrrad bei Harbour Gate/)).toBeInTheDocument()
})
