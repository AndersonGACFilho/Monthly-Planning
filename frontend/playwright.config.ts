import { defineConfig, devices } from '@playwright/test';

// IMP-40: end-to-end tests against the real backend. Chromium stands for WebView2 (Windows), WebKit for WebKitGTK
// (Linux). E2E_ENGINES=chromium skips WebKit on hosts without its system libraries.
const engines = (process.env.E2E_ENGINES ?? 'chromium,webkit').split(',');

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 60_000,
  reporter: [['list']],
  globalSetup: './e2e/global-setup.ts',
  use: {
    baseURL: 'http://localhost:5174',
    trace: 'retain-on-failure',
    acceptDownloads: true,
  },
  projects: [
    { name: 'chromium-pt-1280', use: { ...devices['Desktop Chrome'], locale: 'pt-BR', viewport: { width: 1280, height: 800 } } },
    { name: 'chromium-en-800', use: { ...devices['Desktop Chrome'], locale: 'en-US', viewport: { width: 800, height: 600 } } },
    ...(engines.includes('webkit')
      ? [{ name: 'webkit-pt-1280', use: { ...devices['Desktop Safari'], locale: 'pt-BR', viewport: { width: 1280, height: 800 } } }]
      : []),
  ],
  webServer: [
    {
      command: 'bash ../scripts/e2e_backend.sh',
      url: 'http://127.0.0.1:18090/actuator/health',
      timeout: 120_000,
      reuseExistingServer: false,
    },
    {
      command: 'npx vite --port 5174 --strictPort',
      url: 'http://localhost:5174',
      timeout: 120_000,
      reuseExistingServer: false,
      env: { VITE_API_BASE_URL: 'http://127.0.0.1:18090', VITE_SESSION_TOKEN: 'e2e' },
    },
  ],
});
