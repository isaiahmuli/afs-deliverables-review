// One place for every query key, so reads and invalidations always agree.
export const queryKeys = {
  workspaces: {
    all: ['workspaces'] as const,
    list: (city: string, date: string, page: number) =>
      [...queryKeys.workspaces.all, 'list', { city, date, page }] as const,
    detail: (id: string) => [...queryKeys.workspaces.all, 'detail', id] as const,
  },
  bookings: {
    all: ['bookings'] as const,
    list: () => [...queryKeys.bookings.all, 'list'] as const,
    detail: (id: string) => [...queryKeys.bookings.all, 'detail', id] as const,
  },
  history: {
    all: ['history'] as const,
  },
}
