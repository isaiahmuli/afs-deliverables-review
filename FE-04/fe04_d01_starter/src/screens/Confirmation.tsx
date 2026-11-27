import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import type { Reservation } from '../api/types'
import { formatDate, formatFee, formatTime } from '../i18n/format'

export default function Confirmation({ reservation }: { reservation: Reservation }) {
  const { t, i18n } = useTranslation()
  const headingRef = useRef<HTMLHeadingElement>(null)
  const locale = i18n.language

  // Move focus to the result so keyboard and screen-reader users land on it.
  useEffect(() => headingRef.current?.focus(), [])

  return (
    <section>
      <h1 ref={headingRef} tabIndex={-1}>{t('confirmation.title')}</h1>
      <p>
        {t('confirmation.message', {
          name: reservation.riderName,
          station: reservation.stationName,
          date: formatDate(reservation.pickupBy, locale),
          time: formatTime(reservation.pickupBy, locale),
        })}
      </p>
      <p>{t('reserve.fee', { fee: formatFee(reservation.feeEur, locale) })}</p>
    </section>
  )
}
