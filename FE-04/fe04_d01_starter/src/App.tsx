import { useState } from 'react'
import type { Reservation } from './api/types'
import Confirmation from './screens/Confirmation'
import Reserve from './screens/Reserve'

export default function App() {
  const [reservation, setReservation] = useState<Reservation | null>(null)
  return (
    <main>
      {reservation ? <Confirmation reservation={reservation} /> : <Reserve onDone={setReservation} />}
    </main>
  )
}
