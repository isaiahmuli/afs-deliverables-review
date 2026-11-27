import { http, HttpResponse } from 'msw'
import { screen, waitFor, within } from '@testing-library/react'
import { db } from '../src/mocks/db'
import { server } from '../src/mocks/server'
import { findRow, gate, renderApp, row } from './utils'

const bookingRows = () =>
  within(screen.getByRole('list')).getAllByRole('listitem').map((li) => li.textContent)

test('booking updates availability for that date only and My bookings, without a reload', async () => {
  const { user } = renderApp('/bookings')
  // My bookings is visited first, so its list is already cached.
  expect(await findRow('Nairobi Desk 2 on')).toBeInTheDocument()

  await user.click(screen.getByRole('link', { name: 'Browse' }))
  await user.click(await screen.findByRole('link', { name: 'Open Nairobi Desk 1' }))
  await user.click(await screen.findByRole('link', { name: 'Book this workspace' }))
  await user.type(await screen.findByLabelText('Name'), 'Sam Otieno')
  await user.type(screen.getByLabelText('Email'), 'sam@example.com')
  await user.click(screen.getByRole('button', { name: 'Book' }))
  expect(await screen.findByText('Nairobi Desk 1 on 2030-03-04 for Sam Otieno')).toBeInTheDocument()

  await user.click(screen.getByRole('link', { name: 'Browse' }))
  await waitFor(() => expect(row('Nairobi Desk 1 —')).toHaveTextContent('Booked'))
  await user.selectOptions(screen.getByLabelText('Date'), '2030-03-05')
  await waitFor(() => expect(row('Nairobi Desk 1 —')).toHaveTextContent('Available'))

  await user.click(screen.getByRole('link', { name: 'My bookings' }))
  expect(await findRow('Nairobi Desk 1 on 2030-03-04')).toBeInTheDocument()
})

test('cancelling from My bookings removes it at once and frees the desk', async () => {
  const held = gate()
  server.use(
    http.post('/api/bookings/:id/cancel', async () => {
      await held.promise
    }), // falls through to the real handler once released
  )
  const { user } = renderApp('/bookings')
  await user.click(await screen.findByRole('button', { name: 'Cancel Nairobi Desk 2' }))
  expect(screen.queryByText(/Nairobi Desk 2 on/)).not.toBeInTheDocument()

  held.release()
  await user.click(screen.getByRole('link', { name: 'Browse' }))
  await waitFor(() => expect(row('Nairobi Desk 2 —')).toHaveTextContent('Available'))
})

test('a refused cancel puts the booking back exactly as it was', async () => {
  db.bookings.push(
    { ...db.bookings[0], id: 'b-2', workspaceId: 'nai-3', workspaceName: 'Nairobi Desk 3', date: '2030-03-05' },
    { ...db.bookings[0], id: 'b-3', workspaceId: 'lis-1', workspaceName: 'Lisbon Desk 1', date: '2030-03-06' },
  )
  const held = gate()
  server.use(
    http.post('/api/bookings/:id/cancel', async () => {
      await held.promise
      return new HttpResponse(null, { status: 500 })
    }),
  )
  const { user } = renderApp('/bookings')
  await findRow('Lisbon Desk 1 on')
  const before = bookingRows()
  expect(before).toHaveLength(3)

  await user.click(screen.getByRole('button', { name: 'Cancel Nairobi Desk 3' }))
  expect(screen.queryByText(/Nairobi Desk 3 on/)).not.toBeInTheDocument()

  // Hold the refetch that follows, so only the rollback can bring the row back.
  server.use(http.get('/api/bookings', () => new Promise<never>(() => {})))
  held.release()
  expect(await screen.findByRole('alert')).toHaveTextContent('Could not cancel')
  expect(bookingRows()).toEqual(before)
})

test('a refetch already in flight cannot bring a cancelled booking back', async () => {
  const { client, user } = renderApp('/bookings')
  await findRow('Nairobi Desk 2 on')

  const slowGet = gate()
  const staleList = structuredClone(db.bookings)
  server.use(
    http.get('/api/bookings', async () => {
      await slowGet.promise
      return HttpResponse.json(staleList)
    }),
    http.post('/api/bookings/:id/cancel', () => new Promise<never>(() => {})),
  )
  void client.invalidateQueries({ queryKey: ['bookings'] })
  await user.click(screen.getByRole('button', { name: 'Cancel Nairobi Desk 2' }))
  slowGet.release()
  await new Promise((r) => setTimeout(r, 50))
  expect(screen.queryByText(/Nairobi Desk 2 on/)).not.toBeInTheDocument()
})
