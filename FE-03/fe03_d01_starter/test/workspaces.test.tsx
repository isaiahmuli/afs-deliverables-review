import { http, HttpResponse } from 'msw'
import { screen, waitFor } from '@testing-library/react'
import { WORKSPACES_STALE_TIME } from '../src/api/queries'
import { db } from '../src/mocks/db'
import { server } from '../src/mocks/server'
import { count, findRow, gate, recordRequests, renderApp, row } from './utils'

test('the list shows a loading state, then the workspaces', async () => {
  renderApp('/')
  expect(await screen.findByText('Loading workspaces…')).toBeInTheDocument()
  expect(await findRow('Nairobi Desk 1 —')).toHaveTextContent('Available')
  expect(screen.queryByText('Loading workspaces…')).not.toBeInTheDocument()
})

test('the list shows an error state when the API fails', async () => {
  server.use(http.get('/api/workspaces', () => new HttpResponse(null, { status: 500 })))
  renderApp('/')
  expect(await screen.findByRole('alert')).toHaveTextContent('Could not load workspaces.')
})

test('page, date and city each show their own results, never another selection', async () => {
  const { user } = renderApp('/')
  expect(await findRow('Nairobi Desk 2 —')).toHaveTextContent('Booked')
  await user.selectOptions(screen.getByLabelText('Date'), '2030-03-05')
  await waitFor(() => expect(row('Nairobi Desk 2 —')).toHaveTextContent('Available'))

  // Hold page 2 to check what shows while it loads: a loading state, not page 1.
  const page2 = gate()
  server.use(
    http.get('/api/workspaces', async ({ request }) => {
      if (new URL(request.url).searchParams.get('page') === '2') await page2.promise
    }),
  )
  await user.click(screen.getByRole('button', { name: 'Next' }))
  expect(screen.getByText('Loading workspaces…')).toBeInTheDocument()
  expect(screen.queryByText(/Nairobi Desk 1 —/)).not.toBeInTheDocument()
  page2.release()
  expect(await findRow('Nairobi Desk 5 —')).toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Previous' }))
  expect(await findRow('Nairobi Desk 1 —')).toBeInTheDocument()

  await user.selectOptions(screen.getByLabelText('City'), 'Lisbon')
  expect(await findRow('Lisbon Desk 1 —')).toBeInTheDocument()
  expect(screen.queryByText(/Nairobi Desk/)).not.toBeInTheDocument()
})

test('coming back within staleTime sends no request; coming back later refetches', async () => {
  const seen = recordRequests()
  const { user } = renderApp('/')
  expect(await findRow('Nairobi Desk 1 —')).toHaveTextContent('Available')
  expect(count(seen, 'GET /api/workspaces')).toBe(1)

  await user.click(screen.getByRole('link', { name: 'My bookings' }))
  await user.click(screen.getByRole('link', { name: 'Browse' }))
  expect(row('Nairobi Desk 1 —')).toHaveTextContent('Available')
  expect(count(seen, 'GET /api/workspaces')).toBe(1)

  // The server changes, and the freshness window passes.
  db.bookings.push({ ...db.bookings[0], id: 'b-x', workspaceId: 'nai-1', workspaceName: 'Nairobi Desk 1' })
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(Date.now() + WORKSPACES_STALE_TIME + 1_000)

  await user.click(screen.getByRole('link', { name: 'My bookings' }))
  await user.click(screen.getByRole('link', { name: 'Browse' }))
  await waitFor(() => expect(row('Nairobi Desk 1 —')).toHaveTextContent('Booked'))
  expect(count(seen, 'GET /api/workspaces')).toBe(2)
})
