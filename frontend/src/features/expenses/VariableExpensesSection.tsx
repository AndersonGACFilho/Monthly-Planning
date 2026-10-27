import { Pencil, Plus } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useVariableExpenses } from '../../api/generated/endpoints/variable-expense-controller/variable-expense-controller';
import type { VariableExpenseModel } from '../../api/generated/model';
import { CategoryIcon } from '../../shared/components/CategoryIcon';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { Field } from '../../shared/components/Field';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { MoneyText } from '../../shared/components/MoneyText';
import { DeleteButton } from '../../shared/components/ResourceActions';
import { formatDate } from '../../shared/format';
import { useCategoryMap, useMemberMap } from '../../shared/hooks/useLookups';
import { VariableExpenseForm } from './VariableExpenseForm';

/**
 * Variable expenses of the period with member and category filters and the filter total (RF-34, RF-35).
 *
 * @param props - selected billing period
 * @returns the section
 */
export function VariableExpensesSection({ period }: { period: string }) {
  const { t } = useTranslation();
  const [member, setMember] = useState<number | null>(null);
  const [category, setCategory] = useState<number | null>(null);
  const [editing, setEditing] = useState<VariableExpenseModel | 'new' | null>(null);
  const { data } = useVariableExpenses({ billingPeriod: period, member: member ?? undefined, category: category ?? undefined });
  const members = useMemberMap();
  const categories = useCategoryMap();
  const items = data?.items ?? [];

  return (
    <section className="flex flex-col gap-3" aria-labelledby="variable-title">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="variable-title" className="text-lg font-semibold">
          {t('variableExpense.title')}
        </h2>
        <button type="button" className="button button-primary" onClick={() => setEditing('new')}>
          <Plus size={16} aria-hidden /> {t('variableExpense.new')}
        </button>
      </div>
      {editing && (
        <VariableExpenseForm
          key={editing === 'new' ? 'new' : editing.id}
          expense={editing === 'new' ? undefined : editing}
          period={period}
          onDone={() => setEditing(null)}
        />
      )}
      <div className="flex flex-wrap items-end gap-3">
        <Field label={t('recurrence.member')} htmlFor="variable-member">
          <MemberPicker id="variable-member" allowEmpty emptyLabel={t('variableExpense.allMembers')} includeInactive value={member} onChange={setMember} />
        </Field>
        <Field label={t('recurrence.category')} htmlFor="variable-category">
          <CategoryPicker id="variable-category" allowEmpty emptyLabel={t('variableExpense.allCategories')} value={category} onChange={setCategory} />
        </Field>
        <p className="ml-auto text-sm" aria-live="polite">
          {t('variableExpense.filterTotal')}: <MoneyText className="font-semibold" value={data?.total ?? 0} />
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="table">
          <thead>
            <tr>
              <th scope="col">{t('variableExpense.date')}</th>
              <th scope="col">{t('recurrence.description')}</th>
              <th scope="col">{t('recurrence.member')}</th>
              <th scope="col" className="hidden wide:table-cell">
                {t('recurrence.category')}
              </th>
              <th scope="col" className="hidden wide:table-cell">
                {t('recurrence.paymentMethod')}
              </th>
              <th scope="col" className="text-right">
                {t('recurrence.amount')}
              </th>
              <th scope="col">
                <span className="sr-only">{t('common.actions')}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && (
              <tr>
                <td colSpan={7} className="text-(--color-text-muted)">
                  {t('common.empty')}
                </td>
              </tr>
            )}
            {items.map((e) => {
              const cat = e.categoryId != null ? categories.get(e.categoryId) : undefined;
              return (
                <tr key={e.id}>
                  <td>{formatDate(e.expenseDate)}</td>
                  <td>{e.description}</td>
                  <td>{members.get(e.memberId ?? 0)?.code}</td>
                  <td className="hidden wide:table-cell">
                    {cat && (
                      <span className="inline-flex items-center gap-1">
                        <CategoryIcon icon={cat.icon} color={cat.color} /> {cat.name}
                      </span>
                    )}
                  </td>
                  <td className="hidden wide:table-cell">{e.paymentMethod && t(`recurrence.paymentMethods.${e.paymentMethod}`)}</td>
                  <td className="text-right">
                    <MoneyText value={e.amount} />
                  </td>
                  <td className="whitespace-nowrap">
                    <button type="button" className="icon-button" aria-label={t('action.edit')} onClick={() => setEditing(e)}>
                      <Pencil size={16} aria-hidden />
                    </button>
                    <DeleteButton resource={e} />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
