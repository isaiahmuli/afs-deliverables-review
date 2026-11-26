import { http, HttpResponse } from 'msw'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../src/App'
import BookingList from '../src/screens/BookingList'
import { db } from '../src/mocks/db'
import { server } from '../src/mocks/server'
import { findRow, renderWithClient, row } from './utils'

const bookingRows = () =>
  within(screen.getByRole('list')).getAllByRole('listitem').map((li) => li.textContent)

test('booking updates availability for that date only and the booking list, without a reload', async () => {
  const user = userEvent.setup()
  renderWithClient(<App />)

  // Visit My bookings first so its list is already cached.
  await user.click(screen.getByRole('button', { name: 'My bookings' }))
  expect(await findRow('Nairobi Desk 2 on')).toBeInTheDocument()
  expect(screen.queryByText(/Nairobi Desk 1 on/)).not.toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Browse' }))
  await user.click(await screen.findByRole('button', { name: 'Book Nairobi Desk 1' }))
  await waitFor(() => expect(row('Nairobi Desk 1 —')).toHaveTextContent('Booked'))

  // Still available on another date.
  await user.selectOptions(screen.getByLabelText('Date'), '2030-03-05')
  await waitFor(() => expect(row('Nairobi Desk 1 —')).toHaveTextContent('Available'))

  await user.click(screen.getByRole('button', { name: 'My bookings' }))
  expect(await findRow('Nairobi Desk 1 on 2030-03-04')).toBeInTheDocument()
})

test('cancelling removes the booking at once and frees the desk', async () => {
  const user = userEvent.setup()
  let release!: () => void
  const gate = new Promise<void>((r) => (release = r))
  server.use(
    http.delete('/api/bookings/:id', async ({ params }) => {
      await gate
      db.bookings = db.bookings.filter((b) => b.id !== params.id)
      return new HttpResponse(null, { status: 204 })
    }),
  )
  renderWithClient(<App />)
  expect(await findRow('Nairobi Desk 2 —')).toHaveTextContent('Booked')

  await user.click(screen.getByRole('button', { name: 'My bookings' }))
  await user.click(await screen.findByRole('button', { name: 'Cancel Nairobi Desk 2' }))
  // Gone before the server has replied.
  expect(screen.queryByText(/Nairobi Desk 2 on/)).not.toBeInTheDocument()

  release()
  await user.click(screen.getByRole('button', { name: 'Browse' }))
  await waitFor(() => expect(row('Nairobi Desk 2 —')).toHaveTextContent('Available'))
})

test('a refused cancel puts the booking back exactly as it was', async () => {
  const user = userEvent.setup()
  db.bookings.push(
    { id: 'b-2', workspaceId: 'nai-3', workspaceName: 'Nairobi Desk 3', date: '2030-03-05' },
    { id: 'b-3', workspaceId: 'lis-1', workspaceName: 'Lisbon Desk 1', date: '2030-03-06' },
  )
  let release!: () => void
  const gate = new Promise<void>((r) => (release = r))
  server.use(
    http.delete('/api/bookings/:id', async () => {
      await gate
      return new HttpResponse(null, { status: 500 })
    }),
  )
  renderWithClient(<BookingList />)
  await findRow('Lisbon Desk 1 on')
  const before = bookingRows()
  expect(before).toEqual([
    'Nairobi Desk 2 on 2030-03-04',
    'Nairobi Desk 3 on 2030-03-05',
    'Lisbon Desk 1 on 2030-03-06',
  ].map((t) => expect.stringContaining(t)))

  await user.click(screen.getByRole('button', { name: 'Cancel Nairobi Desk 3' }))
  expect(screen.queryByText(/Nairobi Desk 3 on/)).not.toBeInTheDocument()

  // Hold the refetch that follows the failed cancel, so only the rollback can restore the row.
  server.use(http.get('/api/bookings', () => new Promise<never>(() => {})))
  release()
  expect(await screen.findByRole('alert')).toHaveTextContent('Could not cancel')
  expect(bookingRows()).toEqual(before)
})

test('a refetch already in flight cannot bring a cancelled booking back', async () => {
  const user = userEvent.setup()
  const { client } = renderWithClient(<BookingList />)
  await findRow('Nairobi Desk 2 on')

  // Start a slow refetch that will answer with the old list (still containing Desk 2).
  let releaseGet!: () => void
  const getGate = new Promise<void>((r) => (releaseGet = r))
  const staleList = structuredClone(db.bookings)
  server.use(
    http.get('/api/bookings', async () => {
      await getGate
      return HttpResponse.json(staleList)
    }),
    http.delete('/api/bookings/:id', () => new Promise<never>(() => {})),
  )
  void client.invalidateQueries({ queryKey: ['bookings'] })

  await user.click(screen.getByRole('button', { name: 'Cancel Nairobi Desk 2' }))
  releaseGet()
  await new Promise((r) => setTimeout(r, 50))
  expect(screen.queryByText(/Nairobi Desk 2 on/)).not.toBeInTheDocument()
})
