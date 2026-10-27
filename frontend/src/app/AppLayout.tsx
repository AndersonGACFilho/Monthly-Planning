import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router';
import { useReviewSummary } from '../api/generated/endpoints/review-queue-controller/review-queue-controller';
import { WorkbookExportButton } from '../features/workbook/WorkbookExportButton';
import { BillingPeriodSelector } from '../shared/components/BillingPeriodSelector';
import { Toaster } from '../shared/components/Toaster';
import { EXTRA_SCREENS, MAIN_TABS, type NavItem } from './navigation';
import { useStartup } from './useStartup';

/**
 * Window frame: sidebar with the tabs, header with the global billing period, main area and notices.
 *
 * The sidebar shows icons only from 800 to 1199 px and icons with labels from 1200 px (SPEC section 14,
 * RNF-06); the billing period selector stays at the top (UI-02).
 *
 * @returns the layout with the current route rendered in the main area
 */
export function AppLayout() {
  const { t } = useTranslation();
  useStartup();
  return (
    <div className="flex h-full">
      <nav
        aria-label={t('app.menu')}
        className="flex w-14 shrink-0 flex-col gap-1 border-r border-(--color-border) bg-(--color-surface-muted) p-2 wide:w-60"
      >
        <span className="mb-3 hidden px-2 text-sm font-semibold wide:block">{t('app.name')}</span>
        {MAIN_TABS.map((item) => (
          <SidebarLink key={item.path} item={item} />
        ))}
        <hr className="my-2 border-(--color-border)" />
        {EXTRA_SCREENS.map((item) => (
          <SidebarLink key={item.path} item={item} />
        ))}
      </nav>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="grid grid-cols-[1fr_auto_1fr] items-center border-b border-(--color-border) px-4 py-2">
          <span />
          <BillingPeriodSelector />
          <span className="flex justify-end">
            <WorkbookExportButton />
          </span>
        </header>
        <main className="min-w-0 flex-1 overflow-auto p-4">
          <Outlet />
        </main>
      </div>
      <Toaster />
    </div>
  );
}

function SidebarLink({ item }: { item: NavItem }) {
  const { t } = useTranslation();
  const label = t(item.labelKey);
  const pending = usePendingCount(item.path === '/review-queue');
  const Icon = item.icon;
  return (
    <NavLink
      to={item.path}
      end={item.path === '/'}
      title={label}
      className={({ isActive }) =>
        `flex items-center gap-3 rounded px-2 py-2 text-sm hover:bg-(--color-border) ${
          isActive ? 'bg-(--color-border) font-medium text-(--color-accent)' : ''
        }`
      }
    >
      <span className="relative shrink-0">
        <Icon aria-hidden size={18} />
        {pending > 0 && (
          <span className="absolute -top-2 -right-2 rounded-full bg-amber-500 px-1 text-[10px] leading-4 text-white" aria-label={t('review.pendingCount', { count: pending })}>
            {pending > 99 ? '99+' : pending}
          </span>
        )}
      </span>
      <span className="sr-only wide:not-sr-only">{label}</span>
    </NavLink>
  );
}

/** Number of items waiting in the review queue, for the sidebar badge (RF-79). */
function usePendingCount(enabled: boolean): number {
  const { data } = useReviewSummary(undefined, { query: { enabled, refetchInterval: 60_000 } });
  return enabled ? (data?.count ?? 0) : 0;
}
