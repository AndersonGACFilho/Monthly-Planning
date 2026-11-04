import { ArrowRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useCategoryItems } from '../../api/generated/endpoints/monthly-summary-controller/monthly-summary-controller';
import type { CategoryItemRow } from '../../api/generated/model';
import { Modal } from '../../shared/components/Modal';
import { MoneyText } from '../../shared/components/MoneyText';
import { formatDate, fromCents, toCents } from '../../shared/format';
import { useMemberMap } from '../../shared/hooks/useLookups';
import { categoryEntriesPath } from './categoryEntriesPath';

/** Sources in the order they are listed, with the screen that edits each one. */
const SOURCES = [
  { source: 'FIXED', path: '/expenses' },
  { source: 'VARIABLE', path: '/expenses' },
  { source: 'CARD', path: '/entries' },
] as const;

/** Props of {@link CategoryItemsDialog}. */
export interface CategoryItemsDialogProps {
  /** Category, or `null` for expenses without one. */
  categoryId: number | null;
  /** Category name shown in the title. */
  name: string;
  /** Id used to open the card entries of the category (the system "not classified" one when `categoryId` is null). */
  entriesCategoryId: number | undefined;
  /** Billing period shown. */
  period: string;
  /** Member filter of the overview, or `null` for everyone. */
  member: number | null;
  /** Closes the dialog. */
  onClose: () => void;
}

const total = (items: CategoryItemRow[]) => fromCents(items.reduce((sum, item) => sum + toCents(item.amount), 0));

/**
 * Every item counted in a category of the overview: fixed expenses, variable expenses and card entries, each group
 * with its subtotal and a shortcut to the screen where it is edited (RF-12, RF-38).
 *
 * @param props - {@link CategoryItemsDialogProps}
 * @returns the dialog
 */
export function CategoryItemsDialog({ categoryId, name, entriesCategoryId, period, member, onClose }: CategoryItemsDialogProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const members = useMemberMap();
  const params = { ...(categoryId != null && { category: categoryId }), ...(member != null && { member }) };
  const { data: items, isPending } = useCategoryItems(period, params);
  const memberCodes = (ids: number[] | undefined) => (ids ?? []).map((id) => members.get(id)?.code ?? String(id)).join(', ');
  const open = (path: string) => {
    onClose();
    navigate(path);
  };
  const pathOf = (source: (typeof SOURCES)[number]) =>
    source.source === 'CARD' && entriesCategoryId != null
      ? categoryEntriesPath(period, entriesCategoryId, member)
      : source.path;

  return (
    <Modal title={t('categoryItems.title', { category: name })} onClose={onClose}>
      {isPending && <p className="p-2 text-sm text-(--color-text-muted)">{t('app.loading')}</p>}
      {items?.length === 0 && <p className="p-2 text-sm text-(--color-text-muted)">{t('categoryItems.empty')}</p>}
      <div className="flex max-h-[65vh] flex-col gap-4 overflow-auto p-2">
        {SOURCES.map((source) => {
          const group = (items ?? []).filter((item) => item.source === source.source);
          if (group.length === 0) return null;
          const headingId = `category-items-${source.source}`;
          return (
            <section key={source.source} aria-labelledby={headingId} className="flex flex-col gap-1">
              <div className="flex items-center justify-between gap-2">
                <h3 id={headingId} className="font-semibold">
                  {t(`categoryItems.sources.${source.source}`)}
                </h3>
                <button type="button" className="button" onClick={() => open(pathOf(source))}>
                  {t(`categoryItems.open.${source.source}`)} <ArrowRight size={14} aria-hidden />
                </button>
              </div>
              <table className="table">
                <thead>
                  <tr>
                    <th scope="col">{t('categoryItems.date')}</th>
                    <th scope="col">{t('categoryItems.description')}</th>
                    <th scope="col">{t('overview.member')}</th>
                    <th scope="col" className="text-right">
                      {t('recurrence.amount')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {group.map((item) => (
                    <tr key={item.referenceId}>
                      <td className="whitespace-nowrap">{formatDate(item.date)}</td>
                      <td>
                        {item.description}
                        {item.installment && item.installment !== '-' && (
                          <span className="ml-2 text-xs text-(--color-text-muted)">{item.installment}</span>
                        )}
                      </td>
                      <td>{memberCodes(item.memberIds)}</td>
                      <td className="text-right">
                        <MoneyText value={item.amount} />
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <th scope="row" colSpan={3} className="text-left">
                      {t('categoryItems.subtotal')}
                    </th>
                    <td className="text-right font-semibold">
                      <MoneyText value={total(group)} />
                    </td>
                  </tr>
                </tfoot>
              </table>
            </section>
          );
        })}
        {items && items.length > 0 && (
          <p className="flex justify-between border-t border-(--color-border) pt-2 font-semibold">
            <span>{t('categoryItems.total', { count: items.length })}</span>
            <MoneyText value={total(items)} />
          </p>
        )}
      </div>
    </Modal>
  );
}
