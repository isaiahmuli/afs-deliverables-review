import { useSuspenseQuery } from '@tanstack/react-query'
import { Form, useActionData, useNavigation, useParams } from 'react-router'
import { bookingQuery } from '../api/queries'

export default function Confirmation() {
  const { bookingId } = useParams() as { bookingId: string }
  const { data } = useSuspenseQuery(bookingQuery(bookingId))
  const result = useActionData() as { error: string } | null | undefined
  const cancelling = useNavigation().state === 'submitting'

  return (
    <section>
      <h2>Booking confirmation</h2>
      <p>{data.workspaceName} on {data.date} for {data.name}</p>
      <p>Status: {data.status}</p>
      {result?.error && <p role="alert">{result.error}</p>}
      {data.status === 'confirmed' && (
        <Form method="post">
          <button type="submit" disabled={cancelling}>Cancel booking</button>
        </Form>
      )}
    </section>
  )
}
