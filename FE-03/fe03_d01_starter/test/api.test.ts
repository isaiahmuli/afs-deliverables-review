import { getWorkspaces } from '../src/api/client'

// Smoke test: confirms the mock API and test setup work.
test('the mock API lists workspaces for a city and date', async () => {
  const page = await getWorkspaces('Nairobi', '2030-03-04', 1)
  expect(page.items).toHaveLength(4)
  expect(page.totalPages).toBe(2)
})
