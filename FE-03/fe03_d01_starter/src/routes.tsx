import type { QueryClient } from '@tanstack/react-query'
import type { RouteObject } from 'react-router'
import { cancelBooking } from './api/client'
import { bookingQuery, invalidateAfterBookingChange, workspaceQuery } from './api/queries'
import ErrorPage from './ErrorPage'
import Layout from './Layout'
import BookingForm from './screens/BookingForm'
import BookingList from './screens/BookingList'
import Confirmation from './screens/Confirmation'
import History from './screens/History'
import WorkspaceDetail from './screens/WorkspaceDetail'
import WorkspaceList from './screens/WorkspaceList'

// Loaders fill the query cache before a screen renders. A missing record makes the client
// throw a 404 Response, which the error element shows.
export function createRoutes(queryClient: QueryClient): RouteObject[] {
  const workspaceLoader = ({ params }: { params: { workspaceId?: string } }) =>
    queryClient.ensureQueryData(workspaceQuery(params.workspaceId!))

  return [
    {
      path: '/',
      element: <Layout />,
      errorElement: <ErrorPage />,
      children: [
        { index: true, element: <WorkspaceList /> },
        { path: 'workspaces/:workspaceId', loader: workspaceLoader, element: <WorkspaceDetail /> },
        { path: 'workspaces/:workspaceId/book', loader: workspaceLoader, element: <BookingForm /> },
        { path: 'bookings', element: <BookingList /> },
        {
          path: 'bookings/:bookingId',
          loader: ({ params }) => queryClient.ensureQueryData(bookingQuery(params.bookingId!)),
          action: async ({ params }) => {
            try {
              await cancelBooking(params.bookingId!)
            } catch {
              return { error: 'The booking could not be cancelled. Please try again.' }
            }
            // Awaited, so the booking query has refetched before the screen re-renders.
            await invalidateAfterBookingChange(queryClient)
            return null
          },
          element: <Confirmation />,
        },
        { path: 'history', element: <History /> },
      ],
    },
  ]
}
