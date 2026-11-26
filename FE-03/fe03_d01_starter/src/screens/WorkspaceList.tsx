import { useState } from 'react'
import { useCreateBooking, useWorkspaces } from '../api/queries'
import { CITIES, DATES } from '../mocks/db'

export default function WorkspaceList() {
  const [city, setCity] = useState<string>(CITIES[0])
  const [date, setDate] = useState<string>(DATES[0])
  const [page, setPage] = useState(1)

  const { data, isPending, isError, refetch } = useWorkspaces(city, date, page)
  const book = useCreateBooking()

  return (
    <section>
      <label>
        City
        <select value={city} onChange={(e) => { setCity(e.target.value); setPage(1) }}>
          {CITIES.map((c) => <option key={c}>{c}</option>)}
        </select>
      </label>
      <label>
        Date
        <select value={date} onChange={(e) => setDate(e.target.value)}>
          {DATES.map((d) => <option key={d}>{d}</option>)}
        </select>
      </label>
      {isPending && <p>Loading workspaces…</p>}
      {isError && (
        <div role="alert">
          Could not load workspaces. <button onClick={() => refetch()}>Try again</button>
        </div>
      )}
      {book.isError && <p role="alert">Booking failed. Please try again.</p>}
      <ul>
        {data?.items.map((w) => (
          <li key={w.id}>
            {w.name} — {w.available ? 'Available' : 'Booked'}
            {w.available && (
              <button
                disabled={book.isPending}
                onClick={() => book.mutate({ workspaceId: w.id, date })}
              >
                Book {w.name}
              </button>
            )}
          </li>
        ))}
      </ul>
      <button disabled={page === 1} onClick={() => setPage(page - 1)}>Previous</button>
      <button disabled={!data || page >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
    </section>
  )
}
