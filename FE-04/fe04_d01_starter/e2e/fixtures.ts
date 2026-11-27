import { test as base, expect, type Locator, type Page } from '@playwright/test'
import { defineNetworkFixture, type NetworkFixture } from '@msw/playwright'
import type { AnyHandler } from 'msw'
import AxeBuilder from '@axe-core/playwright'
import { handlers } from '../src/mocks/handlers'

// Every test gets the shared mock API handlers through @msw/playwright.
export const test = base.extend<{ handlers: AnyHandler[]; network: NetworkFixture }>({
  handlers: [handlers, { option: true }],
  network: [
    async ({ context, handlers }, use) => {
      const network = defineNetworkFixture({ context, handlers })
      await network.enable()
      await use(network)
      await network.disable()
    },
    { auto: true },
  ],
})
export { expect }

export const picker = (page: Page) => page.getByRole('button', { name: /^Station/ })

// The focused element must have a real, visible outline.
export async function expectVisibleFocus(locator: Locator) {
  await expect(locator).toBeFocused()
  const outline = await locator.evaluate((el) => {
    const s = getComputedStyle(el)
    return { style: s.outlineStyle, width: parseFloat(s.outlineWidth) }
  })
  expect(outline.style, 'focused control has an outline').not.toBe('none')
  expect(outline.width, 'focus outline is at least 2px').toBeGreaterThanOrEqual(2)
}

export async function expectNoAxeViolations(page: Page, label: string) {
  const { violations } = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze()
  expect(violations.map((v) => `${v.id}: ${v.help}`), `axe violations on ${label}`).toEqual([])
}
