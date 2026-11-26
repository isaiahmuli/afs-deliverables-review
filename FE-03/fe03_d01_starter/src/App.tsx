import { useState } from 'react'
import BookingList from './screens/BookingList'
import History from './screens/History'
import WorkspaceList from './screens/WorkspaceList'

export default function App() {
  const [tab, setTab] = useState<'browse' | 'bookings' | 'history'>('browse')
  return (
    <main>
      <h1>Workspace booking</h1>
      <nav>
        <button onClick={() => setTab('browse')}>Browse</button>
        <button onClick={() => setTab('bookings')}>My bookings</button>
        <button onClick={() => setTab('history')}>History</button>
      </nav>
      {tab === 'browse' && <WorkspaceList />}
      {tab === 'bookings' && <BookingList />}
      {tab === 'history' && <History />}
    </main>
  )
}
