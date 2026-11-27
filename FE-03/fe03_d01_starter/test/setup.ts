import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll, beforeEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'
import { resetDb } from '../src/mocks/db'
import { server } from '../src/mocks/server'

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
beforeEach(() => resetDb())
afterEach(() => {
  cleanup()
  server.resetHandlers()
  server.events.removeAllListeners()
  vi.useRealTimers()
})
afterAll(() => server.close())
