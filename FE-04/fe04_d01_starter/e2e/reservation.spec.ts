import { http, HttpResponse } from 'msw'
import { expect, expectNoAxeViolations, expectVisibleFocus, picker, test } from './fixtures'

test('D1-1: a rider reserves a bike from choosing a station to the confirmation', async ({ page }) => {
  await page.goto('/')
  await picker(page).click()
  await page.getByRole('button', { name: 'Harbour Gate (2 available)' }).click()
  await page.getByLabel('Bike').selectOption({ label: 'Bike 22' })
  await page.getByRole('button', { name: 'Reserve' }).click()

  await expect(page.getByRole('heading', { name: 'Reservation confirmed' })).toBeVisible()
  await expect(page.getByText('Bike 22 is held for you at Harbour Gate.')).toBeVisible()
})

test('D1-3: the reservation works with the keyboard alone, in order, with visible focus', async ({ page }) => {
  await page.goto('/')
  await expect(picker(page)).toHaveText('Choose a station')

  await page.keyboard.press('Tab')
  await expectVisibleFocus(picker(page))

  await page.keyboard.press('Enter') // open the list; focus moves to the first station
  await expectVisibleFocus(page.getByRole('button', { name: 'Central Station (3 available)' }))
  await page.keyboard.press('Tab')
  await expectVisibleFocus(page.getByRole('button', { name: 'Harbour Gate (2 available)' }))

  await page.keyboard.press('Enter') // choose it; focus returns to the picker
  await expectVisibleFocus(picker(page))
  await expect(picker(page)).toHaveText('Harbour Gate')

  await page.keyboard.press('Tab')
  const bike = page.getByLabel('Bike')
  await expectVisibleFocus(bike)
  // Keep the first bike: ArrowDown on a closed <select> did not change it on macOS Chromium,
  // and native select keys differ by platform, so changing it would make the test fragile.
  await expect(bike).toHaveValue('bk-21')

  await page.keyboard.press('Tab')
  const reserveButton = page.getByRole('button', { name: 'Reserve' })
  await expectVisibleFocus(reserveButton)
  await page.keyboard.press('Enter')

  const heading = page.getByRole('heading', { name: 'Reservation confirmed' })
  await expect(heading).toBeFocused()
  await expect(page.getByText('Bike 21 is held for you at Harbour Gate.')).toBeVisible()
})

test('D1-4: the station picker exposes its role, name and open state', async ({ page }) => {
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

test('D1-5: every page of the flow, including the open picker, has no WCAG 2 A/AA violations', async ({ page }) => {
  await page.goto('/')
  await expect(picker(page)).toBeVisible()
  await expectNoAxeViolations(page, 'reserve page')

  await picker(page).click()
  await expect(page.getByRole('button', { name: /^Central Station/ })).toBeVisible()
  await expectNoAxeViolations(page, 'reserve page, picker open')

  await page.getByRole('button', { name: /^Central Station/ }).click()
  await expect(page.getByLabel('Bike')).toHaveValue('bk-11')
  await expectNoAxeViolations(page, 'reserve page, station chosen')

  await page.getByRole('button', { name: 'Reserve' }).click()
  await expect(page.getByRole('heading', { name: 'Reservation confirmed' })).toBeVisible()
  await expectNoAxeViolations(page, 'confirmation page')
})

test('a failed reservation tells the rider and stays on the page', async ({ page, network }) => {
  network.use(http.post('/api/reservations', () => new HttpResponse(null, { status: 500 })))
  await page.goto('/')
  await picker(page).click()
  await page.getByRole('button', { name: /^Park Square/ }).click()
  await expect(page.getByLabel('Bike')).toHaveValue('bk-31')
  await page.getByRole('button', { name: 'Reserve' }).click()
  await expect(page.getByRole('alert')).toHaveText('The reservation failed. Please try again.')
  await expect(page.getByRole('heading', { name: 'Reserve a bike' })).toBeVisible()
})
