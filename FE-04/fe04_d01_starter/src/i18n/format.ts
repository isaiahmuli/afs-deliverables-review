// Values are formatted with Intl for the active locale. Times from the API are UTC.
export const formatFee = (eur: number, locale: string) =>
  new Intl.NumberFormat(locale, { style: 'currency', currency: 'EUR' }).format(eur)

export const formatDate = (iso: string, locale: string) =>
  new Intl.DateTimeFormat(locale, { dateStyle: 'long', timeZone: 'UTC' }).format(new Date(iso))

export const formatTime = (iso: string, locale: string) =>
  new Intl.DateTimeFormat(locale, { timeStyle: 'short', timeZone: 'UTC' }).format(new Date(iso))
