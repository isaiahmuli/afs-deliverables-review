import { useEffect, useRef } from 'react'
import type { Reservation } from '../api/types'

export default function Confirmation({ reservation }: { reservation: Reservation }) {
  const headingRef = useRef<HTMLHeadingElement>(null)

  // Move focus to the result so keyboard and screen-reader users land on it.
  useEffect(() => headingRef.current?.focus(), [])

  return (
    <section>
      <h1 ref={headingRef} tabIndex={-1}>Reservation confirmed</h1>
      <p>
        {reservation.bikeLabel} is held for you at {reservation.stationName}.
      </p>
    </section>
  )
}
