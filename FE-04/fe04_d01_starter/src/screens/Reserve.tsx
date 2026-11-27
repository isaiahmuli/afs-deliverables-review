import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getStations, reserve } from '../api/client'
import type { Reservation, Station } from '../api/types'
import { formatFee } from '../i18n/format'

type ErrorKey = 'reserve.nameRequired' | 'reserve.failed'

export default function Reserve({ onDone }: { onDone: (r: Reservation) => void }) {
  const { t, i18n } = useTranslation()
  const [stations, setStations] = useState<Station[] | null>(null)
  const [station, setStation] = useState<Station | null>(null)
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [error, setError] = useState<ErrorKey | null>(null)
  const [sending, setSending] = useState(false)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const listRef = useRef<HTMLUListElement>(null)

  useEffect(() => {
    getStations().then(setStations, () => setStations([]))
  }, [])

  // When the list opens, move focus to the first station.
  useEffect(() => {
    if (open) listRef.current?.querySelector('button')?.focus()
  }, [open])

  function close() {
    setOpen(false)
    triggerRef.current?.focus()
  }

  async function submit() {
    if (!name.trim()) {
      setError('reserve.nameRequired')
      return
    }
    if (!station || sending) return
    setError(null)
    setSending(true)
    try {
      onDone(await reserve(station.id, name.trim()))
    } catch {
      setError('reserve.failed')
      setSending(false)
    }
  }

  const nameInvalid = error === 'reserve.nameRequired'

  return (
    <section>
      <h1>{t('reserve.title')}</h1>

      <p>
        <label htmlFor="rider-name">{t('reserve.nameLabel')}</label>{' '}
        <input
          id="rider-name"
          value={name}
          aria-invalid={nameInvalid}
          aria-describedby={nameInvalid ? 'name-error' : undefined}
          onChange={(e) => setName(e.target.value)}
        />{' '}
        {nameInvalid && <span id="name-error" role="alert">{t(error)}</span>}
      </p>

      {stations === null ? (
        <p>{t('reserve.loading')}</p>
      ) : (
        <div onKeyDown={(e) => { if (e.key === 'Escape' && open) close() }}>
          <span id="station-label">{t('reserve.stationLabel')}</span>
          <div>
            <button
              ref={triggerRef}
              type="button"
              id="station-trigger"
              aria-labelledby="station-label station-trigger"
              aria-expanded={open}
              aria-controls="station-list"
              onClick={() => setOpen(!open)}
            >
              {station ? station.name : t('reserve.choose')}
            </button>
          </div>
          {open && (
            <ul className="picker-list" id="station-list" ref={listRef} aria-labelledby="station-label">
              {stations.map((s) => (
                <li key={s.id}>
                  <button
                    type="button"
                    onClick={() => {
                      setStation(s)
                      close()
                    }}
                  >
                    {t('reserve.option', {
                      station: s.name,
                      availability: t('reserve.bikesAvailable', { count: s.bikesAvailable }),
                    })}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {station && <p>{t('reserve.fee', { fee: formatFee(station.feeEur, i18n.language) })}</p>}
      {error === 'reserve.failed' && <p role="alert">{t(error)}</p>}

      <button type="button" className="reserve" disabled={!station || sending} onClick={submit}>
        <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
          <path d="M2 8l4 4 8-8" fill="none" stroke="currentColor" strokeWidth="2" />
        </svg>
        {t('reserve.submit')}
      </button>
    </section>
  )
}
