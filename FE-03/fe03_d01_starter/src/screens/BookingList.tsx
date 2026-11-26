import { useBookings, useCancelBooking } from '../api/queries'

export default function BookingList() {
  const { data, isPending, isError } = useBookings()
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
            {b.workspaceName} on {b.date}
            <button onClick={() => cancel.mutate(b.id)}>Cancel {b.workspaceName}</button>
          </li>
        ))}
      </ul>
    </section>
  )
}
