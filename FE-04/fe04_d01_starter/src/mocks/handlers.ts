import { http, HttpResponse } from 'msw'

// The mock API is fixed: do not change its endpoints or response shapes.
// Prices are numbers in EUR. Times are ISO 8601 strings in UTC.
export const stations = [
  { id: 'st-1', name: 'Central Station', bikesAvailable: 1, feeEur: 3.5 },
  { id: 'st-2', name: 'Harbour Gate', bikesAvailable: 2, feeEur: 3.5 },
  { id: 'st-3', name: 'Park Square', bikesAvailable: 5, feeEur: 3.5 },
]

export const handlers = [
  http.get('/api/stations', () => HttpResponse.json(stations)),

  http.post('/api/reservations', async ({ request }) => {
    const { stationId, riderName } = (await request.json()) as { stationId: string; riderName: string }
    const station = stations.find((s) => s.id === stationId)
    if (!station) return new HttpResponse(null, { status: 404 })
    return HttpResponse.json(
      {
        id: 'rv-1',
        stationName: station.name,
        riderName,
        feeEur: station.feeEur,
        pickupBy: '2030-03-04T09:30:00Z',
      },
      { status: 201 },
    )
  }),
]
