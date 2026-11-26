import { http, HttpResponse } from 'msw'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../src/App'
import WorkspaceList from '../src/screens/WorkspaceList'
import { WORKSPACES_STALE_TIME } from '../src/api/queries'
import { db } from '../src/mocks/db'
import { server } from '../src/mocks/server'
import { countRequests, findRow, renderWithClient, row } from './utils'

afterEach(() => {
  server.events.removeAllListeners()
  vi.useRealTimers()
})

test('shows a loading state, then the workspaces', async () => {
  renderWithClient(<WorkspaceList />)
  expect(screen.getByText('Loading workspaces…')).toBeInTheDocument()
  expect(await findRow('Nairobi Desk 1 —')).toHaveTextContent('Available')
  expect(screen.queryByText('Loading workspaces…')).not.toBeInTheDocument()
})

test('shows an error state when the workspaces request fails', async () => {
  server.use(http.get('/api/workspaces', () => new HttpResponse(null, { status: 500 })))
  renderWithClient(<WorkspaceList />)
  expect(await screen.findByRole('alert')).toHaveTextContent('Could not load workspaces.')
  expect(screen.queryByText('Loading workspaces…')).not.toBeInTheDocument()
})

test('page, date and city each show their own results', async () => {
  const user = userEvent.setup()
  renderWithClient(<WorkspaceList />)

  // Nairobi Desk 2 is booked on 2030-03-04 only.
  expect(await findRow('Nairobi Desk 2 —')).toHaveTextContent('Booked')
  await user.selectOptions(screen.getByLabelText('Date'), '2030-03-05')
  await waitFor(() => expect(row('Nairobi Desk 2 —')).toHaveTextContent('Available'))

  // Hold page 2 to check what shows while it loads: a loading state, not page 1.
  let release!: () => void
  const gate = new Promise<void>((r) => (release = r))
  server.use(
    http.get('/api/workspaces', async ({ request }) => {
      if (new URL(request.url).searchParams.get('page') === '2') await gate
      // Returning nothing falls through to the normal handler.
    }),
  )
  await user.click(screen.getByRole('button', { name: 'Next' }))
  expect(screen.getByText('Loading workspaces…')).toBeInTheDocument()
  expect(screen.queryByText(/Nairobi Desk 1 —/)).not.toBeInTheDocument()
  release()
  expect(await findRow('Nairobi Desk 5 —')).toBeInTheDocument()
  expect(screen.queryByText(/Nairobi Desk 1 —/)).not.toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Previous' }))
  expect(await findRow('Nairobi Desk 1 —')).toBeInTheDocument()
  expect(screen.queryByText(/Nairobi Desk 5 —/)).not.toBeInTheDocument()

  await user.selectOptions(screen.getByLabelText('City'), 'Lisbon')
  expect(await findRow('Lisbon Desk 1 —')).toBeInTheDocument()
  expect(screen.queryByText(/Nairobi Desk/)).not.toBeInTheDocument()
})

test('coming back within staleTime sends no request; coming back later refetches', async () => {
  const user = userEvent.setup()
  const requests = countRequests('/api/workspaces')
  renderWithClient(<App />)
  expect(await findRow('Nairobi Desk 1 —')).toHaveTextContent('Available')
  expect(requests).toHaveLength(1)

  // Within the freshness window: the cached list shows and nothing is sent.
  await user.click(screen.getByRole('button', { name: 'My bookings' }))
  await user.click(screen.getByRole('button', { name: 'Browse' }))
  expect(row('Nairobi Desk 1 —')).toHaveTextContent('Available')
  expect(requests).toHaveLength(1)

  // The server changes behind our back, and the window passes.
  db.bookings.push({ id: 'b-x', workspaceId: 'nai-1', workspaceName: 'Nairobi Desk 1', date: '2030-03-04' })
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(Date.now() + WORKSPACES_STALE_TIME + 1_000)

  await user.click(screen.getByRole('button', { name: 'My bookings' }))
  await user.click(screen.getByRole('button', { name: 'Browse' }))
  await waitFor(() => expect(row('Nairobi Desk 1 —')).toHaveTextContent('Booked'))
  expect(requests).toHaveLength(2)
})
