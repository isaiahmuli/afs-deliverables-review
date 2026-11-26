import type { Booking, HistoryPage, WorkspacePage } from './types'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T)
}

export const getWorkspaces = (city: string, date: string, page: number) =>
  request<WorkspacePage>(`/api/workspaces?city=${city}&date=${date}&page=${page}`)

export const getBookings = () => request<Booking[]>('/api/bookings')

export const createBooking = (workspaceId: string, date: string) =>
  request<Booking>('/api/bookings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ workspaceId, date }),
  })

export const cancelBooking = (id: string) =>
  request<void>(`/api/bookings/${id}`, { method: 'DELETE' })

// One page of past bookings; pass the previous page's nextCursor for the next one.
export const getHistory = (cursor: string | null) =>
  request<HistoryPage>(`/api/bookings/history${cursor ? `?cursor=${cursor}` : ''}`)
