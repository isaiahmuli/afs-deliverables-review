// One place for every query key, so reads and invalidations always agree.
export const queryKeys = {
  workspaces: {
    all: ['workspaces'] as const,
    list: (city: string, date: string, page: number) =>
      [...queryKeys.workspaces.all, { city, date, page }] as const,
  },
  bookings: {
    all: ['bookings'] as const,
  },
  history: {
    all: ['history'] as const,
  },
}
