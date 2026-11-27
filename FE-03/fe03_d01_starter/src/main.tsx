import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, RouterProvider } from 'react-router'
import { createRoutes } from './routes'

const queryClient = new QueryClient()
const router = createBrowserRouter(createRoutes(queryClient))

async function start() {
  // In development the mock API runs in the browser.
  const { worker } = await import('./mocks/browser')
  await worker.start({ onUnhandledFrame: 'bypass' })
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </StrictMode>,
  )
}
start()
