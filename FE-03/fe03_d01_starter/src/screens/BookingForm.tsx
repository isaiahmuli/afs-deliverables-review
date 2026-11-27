import { useSuspenseQuery } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate, useParams } from 'react-router'
import { useCreateBooking, workspaceQuery } from '../api/queries'
import { DATES } from '../mocks/db'

export const bookingSchema = z.object({
  name: z.string().trim().min(1, 'Enter your name'),
  email: z.email('Enter a valid email address'),
  date: z.string(),
})
type BookingInput = z.infer<typeof bookingSchema>

export default function BookingForm() {
  const { workspaceId } = useParams() as { workspaceId: string }
  const { data: workspace } = useSuspenseQuery(workspaceQuery(workspaceId))
  const navigate = useNavigate()
  const book = useCreateBooking()
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<BookingInput>({
    resolver: zodResolver(bookingSchema),
    defaultValues: { name: '', email: '', date: DATES[0] },
  })

  const onSubmit = async (values: BookingInput) => {
    try {
      const booking = await book.mutateAsync({ workspaceId, ...values })
      navigate(`/bookings/${booking.id}`)
    } catch {
      // The mutation's error is shown below.
    }
  }

  const refused = book.error instanceof Response && book.error.status === 409

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate>
      <h2>Book {workspace.name}</h2>
      <div>
        <label htmlFor="name">Name</label>
        <input id="name" aria-invalid={!!errors.name} aria-describedby="name-error" {...register('name')} />
        {errors.name && <span id="name-error">{errors.name.message}</span>}
      </div>
      <div>
        <label htmlFor="email">Email</label>
        <input id="email" type="email" aria-invalid={!!errors.email} aria-describedby="email-error" {...register('email')} />
        {errors.email && <span id="email-error">{errors.email.message}</span>}
      </div>
      <div>
        <label htmlFor="date">Date</label>
        <select id="date" {...register('date')}>
          {DATES.map((d) => <option key={d}>{d}</option>)}
        </select>
      </div>
      {book.isError && (
        <p role="alert">
          {refused ? 'This workspace is already booked on that date.' : 'Booking failed. Please try again.'}
        </p>
      )}
      <button type="submit" disabled={isSubmitting}>Book</button>
    </form>
  )
}
