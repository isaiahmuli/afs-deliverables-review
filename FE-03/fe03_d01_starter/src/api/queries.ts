import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cancelBooking, createBooking, getBookings, getHistory, getWorkspaces } from './client'
import { queryKeys } from './queryKeys'
import type { Booking } from './types'

export const WORKSPACES_STALE_TIME = 30_000
const BOOKINGS_STALE_TIME = 30_000
const HISTORY_STALE_TIME = 5 * 60_000

export function useWorkspaces(city: string, date: string, page: number) {
  return useQuery({
    queryKey: queryKeys.workspaces.list(city, date, page),
    queryFn: () => getWorkspaces(city, date, page),
    staleTime: WORKSPACES_STALE_TIME,
  })
}

export function useBookings() {
  return useQuery({
    queryKey: queryKeys.bookings.all,
    queryFn: getBookings,
    staleTime: BOOKINGS_STALE_TIME,
  })
}

export function useHistory() {
  return useInfiniteQuery({
    queryKey: queryKeys.history.all,
    queryFn: ({ pageParam }) => getHistory(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) => lastPage.nextCursor,
    staleTime: HISTORY_STALE_TIME,
  })
}

export function useCreateBooking() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ workspaceId, date }: { workspaceId: string; date: string }) =>
      createBooking(workspaceId, date),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.bookings.all }),
      ]),
  })
}

export function useCancelBooking() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => cancelBooking(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.bookings.all })
      const previous = queryClient.getQueryData<Booking[]>(queryKeys.bookings.all)
      queryClient.setQueryData<Booking[]>(queryKeys.bookings.all, (old) =>
        old?.filter((b) => b.id !== id),
      )
      return { previous }
    },
    onError: (_error, _id, context) => {
      if (context?.previous) queryClient.setQueryData(queryKeys.bookings.all, context.previous)
    },
    // Not awaited: the screen already shows the right list, so the error (or success)
    // should not wait for the refetch to finish.
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.bookings.all })
      void queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.all })
    },
  })
}
