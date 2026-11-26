import type { Booking, Workspace } from '../api/types'

export const CITIES = ['Nairobi', 'Lisbon', 'Austin'] as const
export const DATES = ['2030-03-04', '2030-03-05', '2030-03-06'] as const
export const PAGE_SIZE = 4
export const HISTORY_PAGE_SIZE = 5

type Row = Omit<Workspace, 'available'>

const workspaces = (): Row[] =>
  CITIES.flatMap((city) =>
    // Nairobi has 6 workspaces (2 pages), the other cities have 4.
    Array.from({ length: city === 'Nairobi' ? 6 : 4 }, (_, i) => ({
      id: `${city.slice(0, 3).toLowerCase()}-${i + 1}`,
      name: `${city} Desk ${i + 1}`,
      city,
    })),
  )

const history = (): Booking[] =>
  Array.from({ length: 12 }, (_, i) => ({
    id: `past-${i + 1}`,
    workspaceId: `nai-${(i % 6) + 1}`,
    workspaceName: `Nairobi Desk ${(i % 6) + 1}`,
    date: `2029-0${(i % 9) + 1}-10`,
  }))

export const db = {
  workspaces: workspaces(),
  bookings: [] as Booking[],
  history: history(),
  nextId: 1,
}

export function resetDb() {
  db.workspaces = workspaces()
  db.history = history()
  db.nextId = 2
  db.bookings = [
    { id: 'b-1', workspaceId: 'nai-2', workspaceName: 'Nairobi Desk 2', date: DATES[0] },
  ]
}
resetDb()

export const isAvailable = (workspaceId: string, date: string) =>
  !db.bookings.some((b) => b.workspaceId === workspaceId && b.date === date)
