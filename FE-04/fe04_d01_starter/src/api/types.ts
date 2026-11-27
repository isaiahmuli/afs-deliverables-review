export type Station = { id: string; name: string; bikesAvailable: number; feeEur: number }
export type Reservation = {
  id: string
  stationName: string
  riderName: string
  feeEur: number
  pickupBy: string // ISO 8601 date-time, UTC
}
