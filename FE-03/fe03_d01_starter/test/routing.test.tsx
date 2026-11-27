import { http, HttpResponse } from 'msw'
import { screen, waitFor } from '@testing-library/react'
import { server } from '../src/mocks/server'
import { count, findRow, gate, recordRequests, renderApp } from './utils'

const header = () => screen.getByRole('heading', { level: 1, name: 'Workspace booking' })

test('each screen has its own address, with the same header, and Back/Forward move between them', async () => {
  const { router, user } = renderApp('/')
  await findRow('Nairobi Desk 1 —')
  expect(header()).toBeInTheDocument()

  await user.click(screen.getByRole('link', { name: 'Open Nairobi Desk 1' }))
  expect(await screen.findByRole('heading', { name: 'Nairobi Desk 1' })).toBeInTheDocument()
  expect(router.state.location.pathname).toBe('/workspaces/nai-1')
  expect(header()).toBeInTheDocument()

  // The form is reached from the workspace's page.
  await user.click(screen.getByRole('link', { name: 'Book this workspace' }))
  expect(await screen.findByRole('heading', { name: 'Book Nairobi Desk 1' })).toBeInTheDocument()
  expect(router.state.location.pathname).toBe('/workspaces/nai-1/book')
  expect(header()).toBeInTheDocument()

  await router.navigate(-1)
  expect(await screen.findByRole('heading', { name: 'Nairobi Desk 1' })).toBeInTheDocument()
  await router.navigate(-1)
  expect(await findRow('Nairobi Desk 1 —')).toBeInTheDocument()
  await router.navigate(1)
  expect(await screen.findByRole('heading', { name: 'Nairobi Desk 1' })).toBeInTheDocument()
})

test.each([
  ['/workspaces/nai-3', 'Nairobi Desk 3'],
  ['/workspaces/nai-3/book', 'Book Nairobi Desk 3'],
  ['/bookings/b-1', 'Booking confirmation'],
  ['/history', 'Booking history'],
])('%s works when opened directly', async (path, heading) => {
  renderApp(path)
  expect(await screen.findByRole('heading', { name: heading })).toBeInTheDocument()
  expect(header()).toBeInTheDocument()
})

test('an address that matches no route shows an error screen with the header', async () => {
  renderApp('/no/such/page')
  expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  expect(header()).toBeInTheDocument()
})

test('a workspace that does not exist shows the 404 error screen, not a blank one', async () => {
  renderApp('/workspaces/nope-9')
  expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  expect(header()).toBeInTheDocument()
})

test('the loader has the workspace ready when its screen first appears, and the screen does not fetch again', async () => {
  const seen = recordRequests()
  const held = gate()
  server.use(
    http.get('/api/workspaces/:id', async () => {
      await held.promise
    }),
  )
  const { router, user } = renderApp('/')
  await findRow('Nairobi Desk 1 —')
  await user.click(screen.getByRole('link', { name: 'Open Nairobi Desk 1' }))

  // While the loader waits, the list stays on screen: no empty workspace frame.
  await waitFor(() => expect(router.state.navigation.state).toBe('loading'))
  expect(screen.getByRole('heading', { name: 'Workspaces' })).toBeInTheDocument()

  held.release()
  expect(await screen.findByRole('heading', { name: 'Nairobi Desk 1' })).toBeInTheDocument()
  expect(screen.getByText(/fast internet and a quiet room/)).toBeInTheDocument()
  expect(count(seen, 'GET /api/workspaces/nai-1')).toBe(1)
})

test('the loader turns a server 404 into an error response', async () => {
  server.use(http.get('/api/workspaces/:id', () => new HttpResponse(null, { status: 404 })))
  const { router } = renderApp('/workspaces/nai-1')
  expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  expect(router.state.errors).toMatchObject({ '0': { status: 404 } })
})
