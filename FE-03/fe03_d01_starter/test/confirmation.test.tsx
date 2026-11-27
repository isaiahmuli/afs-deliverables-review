import { http, HttpResponse } from 'msw'
import { screen } from '@testing-library/react'
import { server } from '../src/mocks/server'
import { count, findRow, recordRequests, renderApp, row } from './utils'

test('the confirmation action cancels the booking and the screen shows it as cancelled', async () => {
  const seen = recordRequests()
  const { user } = renderApp('/bookings/b-1')
  expect(await screen.findByText('Status: confirmed')).toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Cancel booking' }))
  expect(await screen.findByText('Status: cancelled')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Cancel booking' })).not.toBeInTheDocument()
  expect(count(seen, 'POST /api/bookings/b-1/cancel')).toBe(1)

  // The invalidation reaches the other screens too.
  await user.click(screen.getByRole('link', { name: 'My bookings' }))
  expect(await screen.findByRole('heading', { name: 'My bookings' })).toBeInTheDocument()
  await screen.findByText((_, el) => el?.tagName === 'UL' && el.children.length === 0)
  await user.click(screen.getByRole('link', { name: 'Browse' }))
  expect(await findRow('Nairobi Desk 2 —')).toHaveTextContent('Available')
  expect(row('Nairobi Desk 2 —')).toBeInTheDocument()
})

test('a refused cancel from the confirmation shows an error and keeps it confirmed', async () => {
  server.use(http.post('/api/bookings/:id/cancel', () => new HttpResponse(null, { status: 500 })))
  const { user } = renderApp('/bookings/b-1')
  await user.click(await screen.findByRole('button', { name: 'Cancel booking' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('could not be cancelled')
  expect(screen.getByText('Status: confirmed')).toBeInTheDocument()
})

test('a booking that does not exist shows the 404 error screen', async () => {
  renderApp('/bookings/b-404')
  expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
})
