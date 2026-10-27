import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { parseMoney } from '../format';

/** Props of {@link MoneyInput}. */
export interface MoneyInputProps {
  /** Current amount, or `null` when empty. */
  value: number | null | undefined;
  /** Called with the parsed amount, or `null` when the text is not a valid amount. */
  onChange: (value: number | null) => void;
  /** Accessible label. */
  'aria-label'?: string;
  /** Input id, for an external label. */
  id?: string;
  /** Allow negative amounts (credits). */
  allowNegative?: boolean;
}

/**
 * Text input for amounts in reais typed in the interface language: `1.234,56` or `1,234.56` (I18N-05).
 * While focused it keeps what the person types; otherwise it shows the formatted value.
 *
 * @param props - {@link MoneyInputProps}
 * @returns the input
 */
export function MoneyInput({ value, onChange, id, allowNegative = false, ...rest }: MoneyInputProps) {
  const { i18n } = useTranslation();
  const [draft, setDraft] = useState<string | null>(null);
  const formatted =
    value === null || value === undefined
      ? ''
      : new Intl.NumberFormat(i18n.language, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);

  return (
    <input
      id={id}
      inputMode="decimal"
      className="input text-right tabular-nums"
      value={draft ?? formatted}
      aria-label={rest['aria-label']}
      onFocus={() => setDraft(formatted)}
      onChange={(event) => {
        setDraft(event.target.value);
        const parsed = parseMoney(event.target.value, i18n.language);
        onChange(parsed !== null && !allowNegative && parsed < 0 ? null : parsed);
      }}
      onBlur={() => setDraft(null)}
    />
  );
}
