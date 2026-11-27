import { http, HttpResponse } from 'msw'
import type { NewBooking } from '../api/types'
import { HISTORY_PAGE_SIZE, PAGE_SIZE, db, isAvailable } from './db'

// The mock API is fixed: do not change its endpoints or response shapes.
export const handlers = [
  http.get('/api/workspaces', ({ request }) => {
    const url = new URL(request.url)
    const city = url.searchParams.get('city')
    const date = url.searchParams.get('date') ?? ''
    const page = Number(url.searchParams.get('page') ?? 1)
    const all = db.workspaces.filter((w) => w.city === city)
    const items = all
      .slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
      .map((w) => ({ ...w, available: isAvailable(w.id, date) }))
    return HttpResponse.json({ items, page, totalPages: Math.ceil(all.length / PAGE_SIZE) })
  }),

  http.get('/api/workspaces/:id', ({ params }) => {
    const workspace = db.workspaces.find((w) => w.id === params.id)
    return workspace ? HttpResponse.json(workspace) : new HttpResponse(null, { status: 404 })
  }),

  // Confirmed bookings only.
  http.get('/api/bookings', () =>
    HttpResponse.json(db.bookings.filter((b) => b.status === 'confirmed')),
  ),

  // Past bookings, newest cursor first. `limit` defaults to 5.
  // Each page is { items, nextCursor }; nextCursor is null on the last page.
  http.get('/api/bookings/history', ({ request }) => {
    const url = new URL(request.url)
    const start = Number(url.searchParams.get('cursor') ?? 0)
    const limit = Number(url.searchParams.get('limit') ?? HISTORY_PAGE_SIZE)
    const items = db.history.slice(start, start + limit)
    const next = start + limit
    return HttpResponse.json({ items, nextCursor: next < db.history.length ? String(next) : null })
  }),

  http.get('/api/bookings/:id', ({ params }) => {
    const booking = db.bookings.find((b) => b.id === params.id)
    return booking ? HttpResponse.json(booking) : new HttpResponse(null, { status: 404 })
  }),

  // Accepts whatever it is sent. It refuses a taken workspace and date with 409.
  http.post('/api/bookings', async ({ request }) => {
    const body = (await request.json()) as NewBooking
    const workspace = db.workspaces.find((w) => w.id === body.workspaceId)
    if (!workspace) return new HttpResponse(null, { status: 404 })
    if (!isAvailable(body.workspaceId, body.date)) return new HttpResponse(null, { status: 409 })
    const booking = {
      ...body,
      id: `b-${db.nextId++}`,
      workspaceName: workspace.name,
      status: 'confirmed' as const,
    }
    db.bookings.push(booking)
    return HttpResponse.json(booking, { status: 201 })
  }),

  http.post('/api/bookings/:id/cancel', ({ params }) => {
    const booking = db.bookings.find((b) => b.id === params.id)
    if (!booking) return new HttpResponse(null, { status: 404 })
    booking.status = 'cancelled'
    return HttpResponse.json(booking)
  }),
]
