import { http, HttpResponse } from 'msw'
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

  http.get('/api/bookings', () => HttpResponse.json(db.bookings)),

  http.post('/api/bookings', async ({ request }) => {
    const { workspaceId, date } = (await request.json()) as { workspaceId: string; date: string }
    const workspace = db.workspaces.find((w) => w.id === workspaceId)
    if (!workspace) return new HttpResponse(null, { status: 404 })
    if (!isAvailable(workspaceId, date)) return new HttpResponse(null, { status: 409 })
    const booking = { id: `b-${db.nextId++}`, workspaceId, workspaceName: workspace.name, date }
    db.bookings.push(booking)
    return HttpResponse.json(booking, { status: 201 })
  }),

  http.delete('/api/bookings/:id', ({ params }) => {
    const before = db.bookings.length
    db.bookings = db.bookings.filter((b) => b.id !== params.id)
    return new HttpResponse(null, { status: db.bookings.length < before ? 204 : 404 })
  }),

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
]
