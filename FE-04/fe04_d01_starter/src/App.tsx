import { useState } from 'react'
import type { Reservation } from './api/types'
import LanguageSelect from './LanguageSelect'
import Confirmation from './screens/Confirmation'
import Reserve from './screens/Reserve'
import './i18n'

export default function App() {
  const [reservation, setReservation] = useState<Reservation | null>(null)
  return (
    <main>
      <LanguageSelect />
      {reservation ? <Confirmation reservation={reservation} /> : <Reserve onDone={setReservation} />}
    </main>
  )
}
