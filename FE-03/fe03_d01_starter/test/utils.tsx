import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { createRoutes } from '../src/routes'
import { server } from '../src/mocks/server'

// The whole app at an address: a fresh query client (retries off) and a memory router
// built from the same routes the browser router uses.
export function renderApp(path = '/') {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const router = createMemoryRouter(createRoutes(client), { initialEntries: [path] })
  const user = userEvent.setup()
  render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { client, router, user }
}

// Records requests that reach the mock API, as "METHOD /path".
export function recordRequests() {
  const seen: string[] = []
  server.events.on('request:start', ({ request }) => {
    seen.push(`${request.method} ${new URL(request.url).pathname}`)
  })
  return seen
}
export const count = (seen: string[], entry: string) => seen.filter((s) => s === entry).length

// A held response: the handler waits until release() is called.
export function gate() {
  let release!: () => void
  const promise = new Promise<void>((r) => (release = r))
  return { promise, release }
}

export const row = (start: string) =>
  screen.getByText((_, el) => el?.tagName === 'LI' && !!el.textContent?.startsWith(start))
export const findRow = (start: string) =>
  screen.findByText((_, el) => el?.tagName === 'LI' && !!el.textContent?.startsWith(start))
