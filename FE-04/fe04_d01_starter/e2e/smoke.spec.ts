import { expect, test } from './fixtures'

// Smoke test: confirms the dev server, the mock API and Playwright work.
test('the reservation page loads', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Reserve a bike' })).toBeVisible()
})
