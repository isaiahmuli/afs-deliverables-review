import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  // One retry, and a trace of that retry, so a failing run leaves evidence behind.
  retries: 1,
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    // The app's MSW service worker would answer requests before page.route sees them.
    // Block it; @msw/playwright serves the same handlers through page.route instead.
    serviceWorkers: 'block',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'pnpm dev --port 5173 --strictPort',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
  },
})
