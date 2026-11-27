// Run in a zone far from UTC, so a date formatted without timeZone: 'UTC' shows up as wrong
// on every machine, not only on machines that are not already on UTC.
process.env.TZ = 'Pacific/Auckland'

import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest'
import { cleanup } from '@testing-library/react'
import { server } from '../src/mocks/server'
import i18n from '../src/i18n'

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
beforeEach(async () => {
  await i18n.changeLanguage('en-US')
})
afterEach(() => {
  cleanup()
  server.resetHandlers()
  server.events.removeAllListeners()
})
afterAll(() => server.close())
