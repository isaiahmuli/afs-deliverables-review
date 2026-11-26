import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './index.css'

async function start() {
  // The mock API runs in the browser through MSW.
  // Playwright blocks service workers and serves the same handlers through
  // @msw/playwright instead, so the app must still render if the worker can't start.
  const { worker } = await import('./mocks/browser')
  try {
    await worker.start({ onUnhandledFrame: 'bypass' })
  } catch {
    console.info('MSW worker not started; requests go to whatever serves them (tests: @msw/playwright).')
  }
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
}
start()
