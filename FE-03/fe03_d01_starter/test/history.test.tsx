import { screen, within } from '@testing-library/react'
import { count, recordRequests, renderApp } from './utils'

const items = () => within(screen.getByRole('list')).queryAllByRole('listitem').map((li) => li.textContent)

test('history shows the first page and Load more appends the next pages until none remain', async () => {
  const seen = recordRequests()
  const { user } = renderApp('/history')

  const loadMore = await screen.findByRole('button', { name: 'Load more' })
  const page1 = items()
  expect(page1).toHaveLength(5)
  expect(count(seen, 'GET /api/bookings/history')).toBe(1)

  await user.click(loadMore)
  await screen.findByText((_, el) => el?.tagName === 'UL' && el.children.length === 10)
  expect(items().slice(0, 5)).toEqual(page1)

  await user.click(screen.getByRole('button', { name: 'Load more' }))
  await screen.findByText((_, el) => el?.tagName === 'UL' && el.children.length === 12)
  expect(screen.queryByRole('button', { name: /Load more|Loading/ })).not.toBeInTheDocument()
  expect(count(seen, 'GET /api/bookings/history')).toBe(3)
})
