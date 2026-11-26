import { useEffect, useRef, useState } from 'react'
import { getBikes, getStations, reserve } from '../api/client'
import type { Bike, Reservation, Station } from '../api/types'

export default function Reserve({ onDone }: { onDone: (r: Reservation) => void }) {
  const [stations, setStations] = useState<Station[]>([])
  const [station, setStation] = useState<Station | null>(null)
  const [open, setOpen] = useState(false)
  const [bikes, setBikes] = useState<Bike[]>([])
  const [bikeId, setBikeId] = useState('')
  const [error, setError] = useState('')
  const triggerRef = useRef<HTMLButtonElement>(null)
  const listRef = useRef<HTMLUListElement>(null)

  useEffect(() => {
    getStations().then(setStations, () => setError('Could not load stations.'))
  }, [])

  // When the list opens, move focus to the first station.
  useEffect(() => {
    if (open) listRef.current?.querySelector('button')?.focus()
  }, [open, stations])

  function close() {
    setOpen(false)
    triggerRef.current?.focus()
  }

  async function pick(s: Station) {
    setStation(s)
    close()
    setBikes([])
    setBikeId('')
    try {
      const list = await getBikes(s.id)
      setBikes(list)
      setBikeId(list[0]?.id ?? '')
    } catch {
      setError('Could not load bikes for this station.')
    }
  }

  async function submit() {
    if (!station || !bikeId) return
    try {
      onDone(await reserve(station.id, bikeId))
    } catch {
      setError('The reservation failed. Please try again.')
    }
  }

  return (
    <section>
      <h1>Reserve a bike</h1>

      <div onKeyDown={(e) => { if (e.key === 'Escape' && open) close() }}>
        <span id="station-label">Station</span>
        <div>
          <button
            ref={triggerRef}
            type="button"
            className="trigger"
            id="station-trigger"
            aria-labelledby="station-label station-trigger"
            aria-expanded={open}
            aria-controls="station-list"
            onClick={() => setOpen(!open)}
          >
            {station ? station.name : 'Choose a station'}
          </button>
        </div>
        {open && (
          <ul className="picker-list" id="station-list" ref={listRef} aria-labelledby="station-label">
            {stations.map((s) => (
              <li key={s.id}>
                <button type="button" onClick={() => pick(s)}>
                  {s.name} ({s.bikesAvailable} available)
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      {station && (
        <div>
          <label htmlFor="bike">Bike</label>{' '}
          <select id="bike" value={bikeId} onChange={(e) => setBikeId(e.target.value)}>
            {bikes.map((b) => (
              <option key={b.id} value={b.id}>{b.label}</option>
            ))}
          </select>
        </div>
      )}

      {error && <p role="alert">{error}</p>}

      <button type="button" className="reserve" disabled={!station || !bikeId} onClick={submit}>
        <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
          <path d="M2 8l4 4 8-8" fill="none" stroke="currentColor" strokeWidth="2" />
        </svg>
        Reserve
      </button>
    </section>
  )
}
