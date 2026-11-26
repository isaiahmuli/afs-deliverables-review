import type { ReactElement } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { server } from '../src/mocks/server'

// A fresh client per test, retries off, so tests never share cache and failures show at once.
export function renderWithClient(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return { client, ...render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>) }
}

// Counts requests that reach the mock API whose path matches.
export function countRequests(path: string) {
  const urls: string[] = []
  server.events.on('request:start', ({ request }) => {
    if (new URL(request.url).pathname === path) urls.push(request.url)
  })
  return urls
}

// The <li> for a workspace or booking, found by the text it starts with.
export const row = (start: string) =>
  screen.getByText((_, el) => el?.tagName === 'LI' && !!el.textContent?.startsWith(start))

export const findRow = (start: string) =>
  screen.findByText((_, el) => el?.tagName === 'LI' && !!el.textContent?.startsWith(start))
