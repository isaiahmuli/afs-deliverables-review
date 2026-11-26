import { useHistory } from '../api/queries'

export default function History() {
  const { data, isPending, isError, hasNextPage, fetchNextPage, isFetchingNextPage } = useHistory()

  return (
    <section>
      <h2>Booking history</h2>
      {isPending && <p>Loading history…</p>}
      {isError && <p role="alert">Could not load history.</p>}
      <ul>
        {data?.pages.flatMap((page) => page.items).map((b) => (
          <li key={b.id}>{b.workspaceName} on {b.date}</li>
        ))}
      </ul>
      {hasNextPage && (
        <button disabled={isFetchingNextPage} onClick={() => fetchNextPage()}>
          {isFetchingNextPage ? 'Loading…' : 'Load more'}
        </button>
      )}
    </section>
  )
}
