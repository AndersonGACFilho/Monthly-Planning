import { useTranslation } from 'react-i18next';
import type { MemberRow } from '../../api/generated/model';
import { MoneyText } from '../../shared/components/MoneyText';
import { useMemberMap } from '../../shared/hooks/useLookups';
import { formatPercent } from '../../shared/format';

/**
 * Totals by member: income, fixed, variable and card expenses, balance and % spent (RF-13, RF-25).
 *
 * @param props - member rows and the table caption
 * @returns the table
 */
export function MemberTotalsTable({ rows, caption }: { rows: MemberRow[]; caption: string }) {
  const { t } = useTranslation();
  const members = useMemberMap();
  return (
    <div className="overflow-x-auto">
      <table className="table">
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr>
            <th scope="col">{t('overview.member')}</th>
            <th scope="col" className="text-right">
              {t('overview.income_short')}
            </th>
            <th scope="col" className="hidden text-right wide:table-cell">
              {t('overview.fixed')}
            </th>
            <th scope="col" className="hidden text-right wide:table-cell">
              {t('overview.variable')}
            </th>
            <th scope="col" className="hidden text-right wide:table-cell">
              {t('overview.card')}
            </th>
            <th scope="col" className="text-right">
              {t('overview.expense_short')}
            </th>
            <th scope="col" className="text-right">
              {t('overview.balance_short')}
            </th>
            <th scope="col" className="text-right">
              {t('overview.spent')}
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const member = members.get(row.memberId ?? 0);
            return (
              <tr key={row.memberId}>
                <th scope="row" className="font-normal">
                  <span className="inline-flex items-center gap-2">
                    <span className="size-3 rounded-full" style={{ background: member?.color }} aria-hidden />
                    {member ? `${member.code} — ${member.name}` : row.memberId}
                  </span>
                </th>
                <td className="text-right">
                  <MoneyText value={row.income} />
                </td>
                <td className="hidden text-right wide:table-cell">
                  <MoneyText value={row.fixed} />
                </td>
                <td className="hidden text-right wide:table-cell">
                  <MoneyText value={row.variable} />
                </td>
                <td className="hidden text-right wide:table-cell">
                  <MoneyText value={row.card} />
                </td>
                <td className="text-right">
                  <MoneyText value={row.expense} />
                </td>
                <td className={`text-right ${(row.balance ?? 0) < 0 ? 'text-red-600' : ''}`}>
                  <MoneyText value={row.balance} />
                </td>
                <td className="text-right">{formatPercent(row.percentSpent)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
