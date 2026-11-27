import { getBooking, getWorkspace, getWorkspaces } from '../src/api/client'

// Smoke test: confirms the mock API and test setup work.
test('the mock API lists workspaces, a workspace, and a booking', async () => {
  const page = await getWorkspaces('Nairobi', '2030-03-04', 1)
  expect(page.items).toHaveLength(4)
  expect(page.totalPages).toBe(2)
  expect((await getWorkspace('nai-1')).name).toBe('Nairobi Desk 1')
  expect((await getBooking('b-1')).workspaceName).toBe('Nairobi Desk 2')
})
