import type { Bike, Reservation, Station } from './types'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return (await res.json()) as T
}

export const getStations = () => request<Station[]>('/api/stations')

export const getBikes = (stationId: string) => request<Bike[]>(`/api/stations/${stationId}/bikes`)

export const reserve = (stationId: string, bikeId: string) =>
  request<Reservation>('/api/reservations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ stationId, bikeId }),
  })
