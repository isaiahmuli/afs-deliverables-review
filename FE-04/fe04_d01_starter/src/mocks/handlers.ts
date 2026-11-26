import { http, HttpResponse } from 'msw'

// The mock API is fixed: do not change its endpoints or response shapes.
const stations = [
  { id: 'st-1', name: 'Central Station', bikesAvailable: 3 },
  { id: 'st-2', name: 'Harbour Gate', bikesAvailable: 2 },
  { id: 'st-3', name: 'Park Square', bikesAvailable: 1 },
]

const bikes: Record<string, { id: string; label: string }[]> = {
  'st-1': [{ id: 'bk-11', label: 'Bike 11' }, { id: 'bk-12', label: 'Bike 12' }, { id: 'bk-13', label: 'Bike 13' }],
  'st-2': [{ id: 'bk-21', label: 'Bike 21' }, { id: 'bk-22', label: 'Bike 22' }],
  'st-3': [{ id: 'bk-31', label: 'Bike 31' }],
}

export const handlers = [
  http.get('/api/stations', () => HttpResponse.json(stations)),

  http.get('/api/stations/:id/bikes', ({ params }) => {
    const list = bikes[params.id as string]
    return list ? HttpResponse.json(list) : new HttpResponse(null, { status: 404 })
  }),

  http.post('/api/reservations', async ({ request }) => {
    const { stationId, bikeId } = (await request.json()) as { stationId: string; bikeId: string }
    const station = stations.find((s) => s.id === stationId)
    const bike = bikes[stationId]?.find((b) => b.id === bikeId)
    if (!station || !bike) return new HttpResponse(null, { status: 404 })
    return HttpResponse.json(
      { id: 'rv-1', stationName: station.name, bikeLabel: bike.label },
      { status: 201 },
    )
  }),
]
