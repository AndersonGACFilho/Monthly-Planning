import {
  ArrowDownUp,
  CheckSquare,
  CreditCard,
  FileUp,
  LayoutDashboard,
  type LucideIcon,
  Palette,
  Send,
  Settings,
  Wallet,
} from 'lucide-react';

/** One entry of the sidebar. */
export interface NavItem {
  /** Route path. */
  path: string;
  /** Translation key of the label. */
  labelKey: string;
  /** Icon shown alone when the sidebar is collapsed (800–1199 px). */
  icon: LucideIcon;
}

/** The five tabs of SPEC section 1, in order. */
export const MAIN_TABS: readonly NavItem[] = [
  { path: '/', labelKey: 'nav.overview', icon: LayoutDashboard },
  { path: '/incomes', labelKey: 'nav.incomes', icon: Wallet },
  { path: '/expenses', labelKey: 'nav.expenses', icon: ArrowDownUp },
  { path: '/entries', labelKey: 'nav.entries', icon: CreditCard },
  { path: '/import', labelKey: 'nav.import', icon: FileUp },
];

/** Screens implied by the SPEC that are not numbered tabs (open question Q15). */
export const EXTRA_SCREENS: readonly NavItem[] = [
  { path: '/review-queue', labelKey: 'nav.reviewQueue', icon: CheckSquare },
  { path: '/templates', labelKey: 'nav.templates', icon: Palette },
  { path: '/deliveries', labelKey: 'nav.deliveries', icon: Send },
  { path: '/settings', labelKey: 'nav.settings', icon: Settings },
];
