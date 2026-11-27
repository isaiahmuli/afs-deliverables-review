import type { Booking, HistoryPage, NewBooking, Workspace, WorkspacePage } from './types'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init)
  if (!res.ok) throw new Response(null, { status: res.status, statusText: res.statusText })
  return (await res.json()) as T
}

export const getWorkspaces = (city: string, date: string, page: number) =>
  request<WorkspacePage>(`/api/workspaces?city=${city}&date=${date}&page=${page}`)

export const getWorkspace = (id: string) => request<Workspace>(`/api/workspaces/${id}`)

// Confirmed bookings only.
export const getBookings = () => request<Booking[]>('/api/bookings')

export const getBooking = (id: string) => request<Booking>(`/api/bookings/${id}`)

export const createBooking = (booking: NewBooking) =>
  request<Booking>('/api/bookings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(booking),
  })

export const cancelBooking = (id: string) =>
  request<Booking>(`/api/bookings/${id}/cancel`, { method: 'POST' })

// One page of past bookings; pass the previous page's nextCursor for the next one.
export const getHistory = (cursor: string | null) =>
  request<HistoryPage>(`/api/bookings/history${cursor ? `?cursor=${cursor}` : ''}`)
