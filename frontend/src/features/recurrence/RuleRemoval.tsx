import { CalendarX } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { DeleteButton } from '../../shared/components/ResourceActions';
import { formatPeriod } from '../../shared/format';
import { linkOf, type HalResource } from '../../shared/hal';
import { useAction } from '../../shared/hooks/useAction';

/** Props of {@link RuleRemoval}. */
export interface RuleRemovalProps {
  /** Income or fixed expense rule. */
  rule: HalResource;
  /** First billing period without the rule when it is ended. */
  from: string;
}

/**
 * Removes an income or fixed expense: deletes it while nothing was received or paid, otherwise ends it from a month
 * (IMP-63). The backend decides which one through the `delete` and `end` links (API-09).
 *
 * @param props - {@link RuleRemovalProps}
 * @returns the button, or nothing
 */
export function RuleRemoval({ rule, from }: RuleRemovalProps) {
  const { t } = useTranslation();
  const end = useAction(rule, 'end');
  if (linkOf(rule, 'delete')) return <DeleteButton resource={rule} />;
  if (!end.available) return null;
  const label = t('recurrence.endFrom', { period: formatPeriod(from) });
  return (
    <button
      type="button"
      className="icon-button button-danger"
      title={label}
      aria-label={label}
      onClick={() => window.confirm(t('recurrence.confirmEnd', { action: label })) && end.mutate({ body: { billingPeriod: from } })}
    >
      <CalendarX size={16} aria-hidden />
    </button>
  );
}
