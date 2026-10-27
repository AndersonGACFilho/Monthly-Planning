import { Check, Link2, Pencil, Plus, RotateCcw, X } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  changeOccurrenceStatus,
  overrideOccurrenceAmount,
  useOccurrences,
} from '../../api/generated/endpoints/occurrence-controller/occurrence-controller';
import { useRecurrenceRules } from '../../api/generated/endpoints/recurrence-rule-controller/recurrence-rule-controller';
import type { OccurrenceModel, RecurrenceRuleModel } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { reportError } from '../../app/queryClient';
import { MoneyInput } from '../../shared/components/MoneyInput';
import { MoneyText } from '../../shared/components/MoneyText';
import { ActiveToggle, DeleteButton } from '../../shared/components/ResourceActions';
import { formatMoney, fromCents, toCents } from '../../shared/format';
import { embedded, linkOf } from '../../shared/hal';
import { useMemberMap } from '../../shared/hooks/useLookups';
import { useSaved } from '../../shared/hooks/useSaved';
import { RecurrenceRuleForm, type RuleKind } from './RecurrenceRuleForm';

/** Props of {@link OccurrencesSection}. */
export interface OccurrencesSectionProps {
  /** Incomes or fixed expenses. */
  kind: RuleKind;
  /** Billing period shown. */
  period: string;
}

/**
 * Incomes (RF-22 to RF-24) or fixed expenses (RF-31 to RF-33) of the billing period, generated from their
 * rules, with the amount of the month, received/paid toggle with date and the rule registry.
 *
 * @param props - {@link OccurrencesSectionProps}
 * @returns the section
 */
export function OccurrencesSection({ kind, period }: OccurrencesSectionProps) {
  const { t } = useTranslation();
  const income = kind === 'INCOME';
  const { data } = useOccurrences({ billingPeriod: period, kind });
  const { data: ruleData } = useRecurrenceRules({ kind });
  const members = useMemberMap();
  const occurrences = embedded<OccurrenceModel>(data, 'occurrences');
  const rules = embedded<RecurrenceRuleModel>(ruleData, 'recurrenceRules');
  const [form, setForm] = useState<{ rule?: RecurrenceRuleModel; oneOff?: boolean } | null>(null);
  const [showRules, setShowRules] = useState(false);
  const counted = occurrences.filter((o) => o.paymentMethod !== 'CARD');
  const total = fromCents(counted.reduce((sum, o) => sum + toCents(o.amount), 0));
  const titleId = `${kind.toLowerCase()}-title`;

  return (
    <section className="flex flex-col gap-3" aria-labelledby={titleId}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id={titleId} className="text-lg font-semibold">
          {income ? t('income.title') : t('fixedExpense.title')}
        </h2>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="button" onClick={() => setShowRules((v) => !v)}>
            {showRules ? t('recurrence.hideRules') : t('recurrence.showRules')}
          </button>
          {income && (
            <button type="button" className="button" onClick={() => setForm({ oneOff: true })}>
              <Plus size={16} aria-hidden /> {t('income.oneOff')}
            </button>
          )}
          <button type="button" className="button button-primary" onClick={() => setForm({})}>
            <Plus size={16} aria-hidden /> {income ? t('income.new') : t('fixedExpense.new')}
          </button>
        </div>
      </div>
      {form && (
        <RecurrenceRuleForm
          key={form.rule?.id ?? (form.oneOff ? 'one-off' : 'new')}
          kind={kind}
          rule={form.rule}
          oneOff={form.oneOff}
          period={period}
          onDone={() => setForm(null)}
        />
      )}
      {!income && <p className="text-sm text-(--color-text-muted)">{t('fixedExpense.cardNote')}</p>}
      <div className="overflow-x-auto">
        <table className="table">
          <thead>
            <tr>
              <th scope="col">{t('recurrence.description')}</th>
              <th scope="col">{t('recurrence.member')}</th>
              <th scope="col" className="hidden wide:table-cell">
                {t('recurrence.day')}
              </th>
              <th scope="col" className="text-right">
                {t('recurrence.amount')}
              </th>
              <th scope="col">{t('recurrence.status')}</th>
            </tr>
          </thead>
          <tbody>
            {occurrences.length === 0 && (
              <tr>
                <td colSpan={5} className="text-(--color-text-muted)">
                  {t('recurrence.noneThisPeriod')}
                </td>
              </tr>
            )}
            {occurrences.map((o) => (
              <tr key={o.id}>
                <td>
                  {o.description}
                  {o.paymentMethod && (
                    <span className="ml-2 text-xs text-(--color-text-muted)">{t(`recurrence.paymentMethods.${o.paymentMethod}`)}</span>
                  )}
                </td>
                <td>{members.get(o.memberId ?? 0)?.code}</td>
                <td className="hidden wide:table-cell">{o.day}</td>
                <td className="text-right">
                  <AmountCell occurrence={o} />
                </td>
                <td>
                  <StatusCell occurrence={o} income={income} />
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row" colSpan={3} className="text-left">
                {t('recurrence.total')}
              </th>
              <td className="text-right font-semibold">
                <MoneyText value={total} />
              </td>
              <td />
            </tr>
          </tfoot>
        </table>
      </div>
      {showRules && (
        <div className="panel flex flex-col gap-2" aria-label={t('recurrence.rules')}>
          <h3 className="font-semibold">{t('recurrence.rules')}</h3>
          <ul className="flex flex-col divide-y divide-(--color-border)">
            {rules.map((rule) => (
              <li key={rule.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                <span className={rule.active ? '' : 'text-(--color-text-muted) line-through'}>
                  {rule.description} · {members.get(rule.memberId ?? 0)?.code} · {formatMoney(rule.defaultAmount)} ·{' '}
                  {rule.recurrence === 'EVERY_N_MONTHS'
                    ? t('recurrence.everyN', { count: rule.intervalMonths })
                    : t(`recurrence.recurrences.${rule.recurrence ?? 'MONTHLY'}`)}{' '}
                  · {rule.validFrom}
                  {rule.validUntil && rule.validUntil !== rule.validFrom ? ` → ${rule.validUntil}` : ''}
                </span>
                <span className="flex gap-1">
                  <button type="button" className="icon-button" aria-label={t('action.edit')} onClick={() => setForm({ rule })}>
                    <Pencil size={16} aria-hidden />
                  </button>
                  <ActiveToggle resource={rule} />
                  <DeleteButton resource={rule} />
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

/**
 * Amount of the month with an inline editor that changes only this period (RF-22, RF-31).
 *
 * @param props - the occurrence
 * @returns the cell content
 */
function AmountCell({ occurrence }: { occurrence: OccurrenceModel }) {
  const { t } = useTranslation();
  const saved = useSaved();
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState<number | null>(occurrence.amount ?? null);
  if (editing) {
    return (
      <span className="inline-flex items-center gap-1">
        <MoneyInput aria-label={t('recurrence.editAmount')} value={value} onChange={setValue} />
        <button
          type="button"
          className="icon-button"
          aria-label={t('action.save')}
          onClick={async () => {
            if (value == null || occurrence.id === undefined) return;
            try {
              await overrideOccurrenceAmount(occurrence.id, { amount: value }, ifMatch(occurrence.version));
              await saved();
              setEditing(false);
            } catch (error) {
              reportError(error);
            }
          }}
        >
          <Check size={16} aria-hidden />
        </button>
        <button type="button" className="icon-button" aria-label={t('action.cancel')} onClick={() => setEditing(false)}>
          <X size={16} aria-hidden />
        </button>
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1">
      <span
        className={occurrence.amountOverridden ? 'font-semibold' : ''}
        title={occurrence.amountOverridden ? t('recurrence.changedThisMonth', { amount: formatMoney(occurrence.defaultAmount) }) : undefined}
      >
        <MoneyText value={occurrence.amount} />
        {occurrence.amountOverridden && '*'}
      </span>
      {linkOf(occurrence, 'override-amount') && (
        <button
          type="button"
          className="icon-button"
          aria-label={t('recurrence.editAmount')}
          title={t('recurrence.editAmount')}
          onClick={() => {
            setValue(occurrence.amount ?? null);
            setEditing(true);
          }}
        >
          <Pencil size={14} aria-hidden />
        </button>
      )}
    </span>
  );
}

/**
 * Received/paid state with its date (RF-24, RF-32) and the card link of card-paid expenses (RF-33, RN-30).
 *
 * @param props - the occurrence and its kind
 * @returns the cell content
 */
function StatusCell({ occurrence, income }: { occurrence: OccurrenceModel; income: boolean }) {
  const { t } = useTranslation();
  const saved = useSaved();
  const change = async (done: boolean, date?: string) => {
    if (occurrence.id === undefined) return;
    try {
      await changeOccurrenceStatus(occurrence.id, { done, date });
      await saved();
    } catch (error) {
      reportError(error);
    }
  };
  if (occurrence.paymentMethod === 'CARD') {
    if (occurrence.linkedEntryId != null) {
      return (
        <span className="inline-flex items-center gap-1 text-sm" title={t('recurrence.linkedEntry')}>
          <Link2 size={14} aria-hidden /> {t('recurrence.inBill')}
        </span>
      );
    }
    return (
      <span className="text-sm text-(--color-text-muted)">
        {occurrence.linkCandidates && occurrence.linkCandidates.length > 0 ? t('recurrence.waitingChoice') : t('recurrence.notLinked')}
      </span>
    );
  }
  if (occurrence.status === 'DONE') {
    const undo = income ? t('income.markExpected') : t('fixedExpense.markPending');
    return (
      <span className="inline-flex items-center gap-1">
        <span className="badge badge-success">{income ? t('income.received') : t('fixedExpense.paid')}</span>
        <input
          type="date"
          className="input w-auto"
          aria-label={t('recurrence.date')}
          value={occurrence.effectiveDate ?? ''}
          onChange={(event) => event.target.value && void change(true, event.target.value)}
        />
        <button type="button" className="icon-button" aria-label={undo} title={undo} onClick={() => void change(false)}>
          <RotateCcw size={14} aria-hidden />
        </button>
      </span>
    );
  }
  return (
    <button type="button" className="button" onClick={() => void change(true)}>
      <Check size={14} aria-hidden /> {income ? t('income.markReceived') : t('fixedExpense.markPaid')}
    </button>
  );
}
