import { QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { RouterProvider } from 'react-router';
import { BackendGate } from './BackendGate';
import { createQueryClient } from './queryClient';
import { createAppRouter } from './router';

/**
 * Root component: server-state cache, wait for the local service, router.
 *
 * @returns the app
 */
export function App() {
  const [queryClient] = useState(createQueryClient);
  const [router] = useState(createAppRouter);
  return (
    <QueryClientProvider client={queryClient}>
      <BackendGate>
        <RouterProvider router={router} />
      </BackendGate>
    </QueryClientProvider>
  );
}
