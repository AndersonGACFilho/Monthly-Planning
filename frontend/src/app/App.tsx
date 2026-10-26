import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { RouterProvider } from 'react-router';
import { BackendGate } from './BackendGate';
import { createAppRouter } from './router';

/**
 * Root component: server-state cache, wait for the local service, router.
 *
 * @returns the app
 */
export function App() {
  const [queryClient] = useState(
    () => new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1 } } }),
  );
  const [router] = useState(createAppRouter);
  return (
    <QueryClientProvider client={queryClient}>
      <BackendGate>
        <RouterProvider router={router} />
      </BackendGate>
    </QueryClientProvider>
  );
}
