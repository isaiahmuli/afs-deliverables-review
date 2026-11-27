import { http, HttpResponse } from 'msw'
import { screen } from '@testing-library/react'
import { server } from '../src/mocks/server'
import { picker, recordRequests, renderApp, reserveAs } from './utils'

test('the stations show a loading state first', async () => {
  renderApp()
  expect(screen.getByText('Loading stations…')).toBeInTheDocument()
  expect(await screen.findByRole('button', { name: 'Station Choose a station' })).toBeInTheDocument()
})

test('the picker is a button that reports whether its list is open', async () => {
  const { user } = renderApp()
  await screen.findByRole('button', { name: /^Station/ })
  expect(picker()).toHaveAttribute('aria-expanded', 'false')
  await user.click(picker())
  expect(picker()).toHaveAttribute('aria-expanded', 'true')
  expect(screen.getByRole('list', { name: 'Station' })).toBeInTheDocument()
  await user.keyboard('{Escape}')
  expect(picker()).toHaveAttribute('aria-expanded', 'false')
  expect(picker()).toHaveFocus()
})

test.each([['an empty name', ''], ['a name of only spaces', '   ']])(
  '%s is refused beside the field and nothing is sent',
  async (_, name) => {
    const seen = recordRequests()
    const { user } = renderApp()
    if (name) await user.type(screen.getByLabelText('Your name'), name)
    await user.click(await screen.findByRole('button', { name: /^Station/ }))
    await user.click(screen.getByRole('button', { name: /^Park Square/ }))
    await user.click(screen.getByRole('button', { name: 'Reserve' }))

    const field = screen.getByLabelText('Your name')
    expect(field).toHaveAccessibleDescription('Please enter your name')
    expect(field).toHaveAttribute('aria-invalid', 'true')
    expect(seen.filter((s) => s === 'POST /api/reservations')).toHaveLength(0)
  },
)

test('a refused reservation shows a message and stays on the page', async () => {
  server.use(http.post('/api/reservations', () => new HttpResponse(null, { status: 500 })))
  const { user } = renderApp()
  await reserveAs(user, { nameLabel: 'Your name', station: /^Central Station/, reserveLabel: 'Reserve' })
  expect(await screen.findByRole('alert')).toHaveTextContent('Could not reserve. Try again.')
  expect(screen.getByRole('heading', { name: 'Reserve a bike' })).toBeInTheDocument()
})

test('a reservation is sent once and the confirmation takes focus', async () => {
  const seen = recordRequests()
  const { user } = renderApp()
  await reserveAs(user, { nameLabel: 'Your name', station: /^Central Station/, reserveLabel: 'Reserve' })
  expect(await screen.findByRole('heading', { name: 'Reservation confirmed' })).toHaveFocus()
  expect(seen.filter((s) => s === 'POST /api/reservations')).toHaveLength(1)
})
