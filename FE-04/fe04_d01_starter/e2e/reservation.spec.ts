import { http, HttpResponse } from 'msw'
import { expect, expectNoAxeViolations, expectVisibleFocus, picker, test } from './fixtures'

// Browser tests run in English; German is covered by the component tests.

test('D1-1: a rider reserves a bike from choosing a station to the confirmation', async ({ page }) => {
  await page.goto('/')
  await page.getByLabel('Your name').fill('Ada')
  await picker(page).click()
  await page.getByRole('button', { name: 'Harbour Gate (2 bikes available)' }).click()
  await expect(page.getByText('Reservation fee: €3.50')).toBeVisible()
  await page.getByRole('button', { name: 'Reserve' }).click()

  await expect(page.getByRole('heading', { name: 'Reservation confirmed' })).toBeVisible()
  await expect(
    page.getByText('Thank you, Ada. Your bike at Harbour Gate is held until March 4, 2030 at 9:30 AM.'),
  ).toBeVisible()
})

test('D1-2: the reservation works with the keyboard alone, in order, with visible focus', async ({ page }) => {
  await page.goto('/')
  await expect(picker(page)).toHaveText('Choose a station')

  await page.keyboard.press('Tab')
  await expectVisibleFocus(page.getByLabel('Language'))
  await page.keyboard.press('Tab')
  await expectVisibleFocus(page.getByLabel('Your name'))
  await page.keyboard.type('Ada')

  await page.keyboard.press('Tab')
  await expectVisibleFocus(picker(page))
  await page.keyboard.press('Enter') // opens the list; focus moves to the first station
  await expectVisibleFocus(page.getByRole('button', { name: 'Central Station (1 bike available)' }))
  await page.keyboard.press('Tab')
  await expectVisibleFocus(page.getByRole('button', { name: 'Harbour Gate (2 bikes available)' }))
  await page.keyboard.press('Enter') // chooses it; focus returns to the picker
  await expectVisibleFocus(picker(page))
  await expect(picker(page)).toHaveText('Harbour Gate')

  await page.keyboard.press('Tab')
  await expectVisibleFocus(page.getByRole('button', { name: 'Reserve' }))
  await page.keyboard.press('Enter')

  await expect(page.getByRole('heading', { name: 'Reservation confirmed' })).toBeFocused()
  await expect(page.getByText(/Thank you, Ada\. Your bike at Harbour Gate/)).toBeVisible()
})

test('D1-3: the station picker exposes its role, name and open state', async ({ page }) => {
  await page.goto('/')
  const trigger = picker(page)
  await expect(trigger).toHaveAccessibleName('Station Choose a station')
  await expect(trigger).toHaveAttribute('aria-expanded', 'false')

  await trigger.focus()
  await page.keyboard.press('Enter')
  await expect(trigger).toHaveAttribute('aria-expanded', 'true')
  await expect(page.getByRole('list', { name: 'Station' })).toBeVisible()

  await page.keyboard.press('Escape')
  await expect(trigger).toHaveAttribute('aria-expanded', 'false')
  await expect(trigger).toBeFocused()

  await trigger.click()
  await page.getByRole('button', { name: /^Park Square/ }).click()
  await expect(trigger).toHaveAccessibleName('Station Park Square')
  await expect(trigger).toHaveAttribute('aria-expanded', 'false')
  await expect(page.getByRole('button', { name: 'Reserve' })).toHaveAccessibleName('Reserve')
})

test('D1-4: every page of the flow, including the open picker, has no WCAG 2 A/AA violations', async ({ page }) => {
  await page.goto('/')
  await expect(picker(page)).toBeVisible()
  await expectNoAxeViolations(page, 'reserve page')

  await picker(page).click()
  await expect(page.getByRole('button', { name: /^Central Station/ })).toBeVisible()
  await expectNoAxeViolations(page, 'reserve page, picker open')

  await page.getByRole('button', { name: /^Central Station/ }).click()
  await page.getByRole('button', { name: 'Reserve' }).click() // empty name: error shows
  await expect(page.getByRole('alert')).toBeVisible()
  await expectNoAxeViolations(page, 'reserve page, name error')

  await page.getByLabel('Your name').fill('Ada')
  await page.getByRole('button', { name: 'Reserve' }).click()
  await expect(page.getByRole('heading', { name: 'Reservation confirmed' })).toBeVisible()
  await expectNoAxeViolations(page, 'confirmation page')
})

test('a refused reservation tells the rider and stays on the page', async ({ page, network }) => {
  network.use(http.post('/api/reservations', () => new HttpResponse(null, { status: 500 })))
  await page.goto('/')
  await page.getByLabel('Your name').fill('Ada')
  await picker(page).click()
  await page.getByRole('button', { name: /^Park Square/ }).click()
  await page.getByRole('button', { name: 'Reserve' }).click()
  await expect(page.getByRole('alert')).toHaveText('Could not reserve. Try again.')
  await expect(page.getByRole('heading', { name: 'Reserve a bike' })).toBeVisible()
})
