import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { bookingsQuery, useCancelBooking } from '../api/queries'

export default function BookingList() {
  const { data, isPending, isError } = useQuery(bookingsQuery())
  const cancel = useCancelBooking()

  return (
    <section>
      <h2>My bookings</h2>
      {isPending && <p>Loading bookings…</p>}
      {isError && <p role="alert">Could not load bookings.</p>}
      {cancel.isError && <p role="alert">Could not cancel the booking. It has been restored.</p>}
      <ul>
        {data?.map((b) => (
          <li key={b.id}>
            <Link to={`/bookings/${b.id}`}>{b.workspaceName} on {b.date}</Link>{' '}
            <button onClick={() => cancel.mutate(b.id)}>Cancel {b.workspaceName}</button>
          </li>
        ))}
      </ul>
    </section>
  )
}
