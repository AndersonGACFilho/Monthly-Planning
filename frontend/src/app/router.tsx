import { createBrowserRouter, type RouteObject } from 'react-router';
import { EntriesPage } from '../features/entries/EntriesPage';
import { ExpensesPage } from '../features/expenses/ExpensesPage';
import { ImportPage } from '../features/import/ImportPage';
import { IncomesPage } from '../features/members/IncomesPage';
import { OverviewPage } from '../features/overview/OverviewPage';
import { ReviewQueuePage } from '../features/review/ReviewQueuePage';
import { SettingsPage } from '../features/settings/SettingsPage';
import { StatementsPage } from '../features/statements/StatementsPage';
import { TemplatesPage } from '../features/templates/TemplatesPage';
import { PlaceholderPage } from '../shared/components/PlaceholderPage';
import type { ReactElement } from 'react';
import { AppLayout } from './AppLayout';
import { EXTRA_SCREENS, MAIN_TABS } from './navigation';

/** Real pages by path; the rest are placeholders until their milestone. */
const PAGES: Record<string, ReactElement> = {
  '/': <OverviewPage />,
  '/incomes': <IncomesPage />,
  '/expenses': <ExpensesPage />,
  '/entries': <EntriesPage />,
  '/import': <ImportPage />,
  '/review-queue': <ReviewQueuePage />,
  '/deliveries': <StatementsPage />,
  '/templates': <TemplatesPage />,
  '/settings': <SettingsPage />,
};

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
        ? { index: true, element: PAGES[item.path] ?? <PlaceholderPage titleKey={item.labelKey} /> }
        : { path: item.path.slice(1), element: PAGES[item.path] ?? <PlaceholderPage titleKey={item.labelKey} /> },
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
