import { render, screen } from '@testing-library/react'
import App from '../src/App'
import { getStations } from '../src/api/client'

// Smoke test: confirms the page renders and the mock API answers in tests.
test('the page renders and the mock API serves stations', async () => {
  render(<App />)
  expect(screen.getByRole('heading', { name: 'Reserve a bike' })).toBeInTheDocument()
  expect(await getStations()).toHaveLength(3)
})
