import { http, HttpResponse } from 'msw'
import { screen } from '@testing-library/react'
import { server } from '../src/mocks/server'
import { count, gate, recordRequests, renderApp } from './utils'

async function openForm() {
  const seen = recordRequests()
  const app = renderApp('/workspaces/nai-1/book')
  await screen.findByRole('heading', { name: 'Book Nairobi Desk 1' })
  return { ...app, seen, name: screen.getByLabelText('Name'), email: screen.getByLabelText('Email') }
}

test.each([
  ['an empty name', '', 'sam@example.com', 'Name', 'Enter your name'],
  ['a name of only spaces', '   ', 'sam@example.com', 'Name', 'Enter your name'],
  ['an empty email', 'Sam', '', 'Email', 'Enter a valid email address'],
  ['a malformed email', 'Sam', 'not-an-email', 'Email', 'Enter a valid email address'],
])('%s is refused beside its field and nothing is sent', async (_, nameValue, emailValue, field, message) => {
  const { user, seen, name, email } = await openForm()
  if (nameValue) await user.type(name, nameValue)
  if (emailValue) await user.type(email, emailValue)
  await user.click(screen.getByRole('button', { name: 'Book' }))

  const input = screen.getByLabelText(field)
  expect(await screen.findByText(message)).toBeInTheDocument()
  expect(input).toHaveAccessibleDescription(message)
  expect(input).toHaveAttribute('aria-invalid', 'true')
  const other = field === 'Name' ? email : name
  expect(other).toHaveAttribute('aria-invalid', 'false')
  expect(count(seen, 'POST /api/bookings')).toBe(0)
})

test('a valid booking is sent exactly once and confirms the chosen workspace and date', async () => {
  const held = gate()
  server.use(
    http.post('/api/bookings', async () => {
      await held.promise
    }),
  )
  const { user, seen, name, email, router } = await openForm()
  await user.type(name, 'Sam Otieno')
  await user.type(email, 'sam@example.com')
  await user.selectOptions(screen.getByLabelText('Date'), '2030-03-06')

  const book = screen.getByRole('button', { name: 'Book' })
  await user.dblClick(book)
  await user.click(book)
  held.release()

  expect(await screen.findByText('Nairobi Desk 1 on 2030-03-06 for Sam Otieno')).toBeInTheDocument()
  expect(screen.getByText('Status: confirmed')).toBeInTheDocument()
  expect(router.state.location.pathname).toBe('/bookings/b-2')
  expect(count(seen, 'POST /api/bookings')).toBe(1)
})

test('a taken workspace and date is refused with a message', async () => {
  server.use(http.post('/api/bookings', () => new HttpResponse(null, { status: 409 })))
  const { user, name, email } = await openForm()
  await user.type(name, 'Sam')
  await user.type(email, 'sam@example.com')
  await user.click(screen.getByRole('button', { name: 'Book' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('already booked on that date')
})
