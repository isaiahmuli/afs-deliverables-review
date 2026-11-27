import {
  infiniteQueryOptions,
  queryOptions,
  useMutation,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query'
import {
  cancelBooking,
  createBooking,
  getBooking,
  getBookings,
  getHistory,
  getWorkspace,
  getWorkspaces,
} from './client'
import { queryKeys } from './queryKeys'
import type { Booking, NewBooking } from './types'

export const WORKSPACES_STALE_TIME = 30_000

// Query options are shared by route loaders and components, so both read the same cache entry.
export const workspacesQuery = (city: string, date: string, page: number) =>
  queryOptions({
    queryKey: queryKeys.workspaces.list(city, date, page),
    queryFn: () => getWorkspaces(city, date, page),
    staleTime: WORKSPACES_STALE_TIME,
  })

// staleTime must be above 0: otherwise the screen refetches what its loader has just fetched.
export const workspaceQuery = (id: string) =>
  queryOptions({
    queryKey: queryKeys.workspaces.detail(id),
    queryFn: () => getWorkspace(id),
    staleTime: 5 * 60_000,
  })

export const bookingsQuery = () =>
  queryOptions({ queryKey: queryKeys.bookings.list(), queryFn: getBookings, staleTime: 30_000 })

export const bookingQuery = (id: string) =>
  queryOptions({
    queryKey: queryKeys.bookings.detail(id),
    queryFn: () => getBooking(id),
    staleTime: 30_000,
  })

export const historyQuery = () =>
  infiniteQueryOptions({
    queryKey: queryKeys.history.all,
    queryFn: ({ pageParam }) => getHistory(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) => lastPage.nextCursor,
    staleTime: 5 * 60_000,
  })

// After any booking change, availability and booking lists may be out of date.
export const invalidateAfterBookingChange = (queryClient: QueryClient) =>
  Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.all }),
    queryClient.invalidateQueries({ queryKey: queryKeys.bookings.all }),
  ])

export function useCreateBooking() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (booking: NewBooking) => createBooking(booking),
    onSuccess: () => invalidateAfterBookingChange(queryClient),
  })
}

export function useCancelBooking() {
  const queryClient = useQueryClient()
  const listKey = queryKeys.bookings.list()
  return useMutation({
    mutationFn: (id: string) => cancelBooking(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: listKey })
      const previous = queryClient.getQueryData<Booking[]>(listKey)
      queryClient.setQueryData<Booking[]>(listKey, (old) => old?.filter((b) => b.id !== id))
      return { previous }
    },
    onError: (_error, _id, context) => {
      if (context?.previous) queryClient.setQueryData(listKey, context.previous)
    },
    // Not awaited: the list is already right, so success or error should not wait for a refetch.
    onSettled: () => {
      void invalidateAfterBookingChange(queryClient)
    },
  })
}
