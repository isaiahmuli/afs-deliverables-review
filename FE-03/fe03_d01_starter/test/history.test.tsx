import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import History from '../src/screens/History'
import { server } from '../src/mocks/server'
import { countRequests, renderWithClient } from './utils'

afterEach(() => server.events.removeAllListeners())

const items = () => within(screen.getByRole('list')).queryAllByRole('listitem').map((li) => li.textContent)

test('history shows the first page and Load more appends the next pages until none remain', async () => {
  const user = userEvent.setup()
  const requests = countRequests('/api/bookings/history')
  renderWithClient(<History />)

  const loadMore = await screen.findByRole('button', { name: 'Load more' })
  const page1 = items()
  expect(page1).toHaveLength(5)
  expect(requests).toHaveLength(1)

  await user.click(loadMore)
  await screen.findByText((_, el) => el?.tagName === 'UL' && el.children.length === 10)
  expect(items().slice(0, 5)).toEqual(page1) // page 2 appended below page 1

  await user.click(screen.getByRole('button', { name: 'Load more' }))
  await screen.findByText((_, el) => el?.tagName === 'UL' && el.children.length === 12)
  expect(screen.queryByRole('button', { name: /Load more|Loading/ })).not.toBeInTheDocument()
  expect(requests).toHaveLength(3)
})
