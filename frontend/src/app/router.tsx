import { createBrowserRouter, type RouteObject } from 'react-router';
import { PlaceholderPage } from '../shared/components/PlaceholderPage';
import { AppLayout } from './AppLayout';
import { EXTRA_SCREENS, MAIN_TABS } from './navigation';

/**
 * Routes of the app. The overview is the index route, so the app opens straight on it (RNF-10).
 * Each placeholder is replaced by its real page in the milestone that builds it.
 */
export const routes: RouteObject[] = [
  {
    path: '/',
    element: <AppLayout />,
    children: [...MAIN_TABS, ...EXTRA_SCREENS].map((item) =>
      item.path === '/'
        ? { index: true, element: <PlaceholderPage titleKey={item.labelKey} /> }
        : { path: item.path.slice(1), element: <PlaceholderPage titleKey={item.labelKey} /> },
    ),
  },
];

/**
 * Creates the browser router for the app window.
 *
 * @returns router for {@link routes}
 */
export function createAppRouter() {
  return createBrowserRouter(routes);
}
