import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../src/App'
import { server } from '../src/mocks/server'

export function renderApp() {
  const user = userEvent.setup()
  render(<App />)
  return { user }
}

export const picker = () => screen.getByRole('button', { name: /^(Station)\b/ })

// Records requests that reach the mock API, as "METHOD /path".
export function recordRequests() {
  const seen: string[] = []
  server.events.on('request:start', ({ request }) => {
    seen.push(`${request.method} ${new URL(request.url).pathname}`)
  })
  return seen
}

// Fill in the name, choose a station and reserve, using only roles and labels.
export async function reserveAs(user: ReturnType<typeof userEvent.setup>, opts: {
  nameLabel: string
  station: RegExp
  reserveLabel: string
  name?: string
}) {
  await user.type(screen.getByLabelText(opts.nameLabel), opts.name ?? 'Ada')
  await user.click(await screen.findByRole('button', { name: /^Station\b/ }))
  await user.click(screen.getByRole('button', { name: opts.station }))
  await user.click(screen.getByRole('button', { name: opts.reserveLabel }))
}
