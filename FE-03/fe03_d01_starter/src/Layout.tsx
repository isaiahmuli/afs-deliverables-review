import { NavLink, Outlet } from 'react-router'

export function SiteHeader() {
  return (
    <header>
      <h1>Workspace booking</h1>
      <nav>
        <NavLink to="/" end>Browse</NavLink>{' '}
        <NavLink to="/bookings" end>My bookings</NavLink>{' '}
        <NavLink to="/history">History</NavLink>
      </nav>
    </header>
  )
}

export default function Layout() {
  return (
    <main>
      <SiteHeader />
      <Outlet />
    </main>
  )
}
