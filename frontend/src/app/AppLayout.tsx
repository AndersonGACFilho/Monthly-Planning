import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router';
import { EXTRA_SCREENS, MAIN_TABS, type NavItem } from './navigation';

/**
 * Window frame: sidebar with the tabs and the main area.
 *
 * The sidebar shows icons only from 800 to 1199 px and icons with labels from 1200 px (SPEC section 14,
 * RNF-06).
 *
 * @returns the layout with the current route rendered in the main area
 */
export function AppLayout() {
  const { t } = useTranslation();
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
      <main className="min-w-0 flex-1 overflow-auto p-4">
        <Outlet />
      </main>
    </div>
  );
}

function SidebarLink({ item }: { item: NavItem }) {
  const { t } = useTranslation();
  const label = t(item.labelKey);
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
      <Icon aria-hidden size={18} className="shrink-0" />
      <span className="sr-only wide:not-sr-only">{label}</span>
    </NavLink>
  );
}
