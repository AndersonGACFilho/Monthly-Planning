import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { changeSettings } from '../api/generated/endpoints/settings-controller/settings-controller';
import { useSettingsMap } from '../features/settings/GeneralSettings';
import { Field } from '../shared/components/Field';
import { Modal } from '../shared/components/Modal';
import { formatPeriod, periodOf, shiftPeriod } from '../shared/format';
import { useBillingPeriodStore } from '../shared/state/billingPeriod';
import { useUiStore } from '../shared/state/ui';
import { reportError } from './queryClient';

/** Months offered around the current one, oldest first. */
const OFFSETS = [-2, -1, 0, 1] as const;

/**
 * Asks which billing period to open when the app starts (IMP-56): the months around the current one, the current
 * one highlighted and focused so Enter opens it, any other month, and a way to stop asking. Shown once per launch,
 * unless the `ask-period-on-start` setting is `false`.
 *
 * @returns the dialog, or nothing
 */
export function StartupPeriodDialog() {
  const { t, i18n } = useTranslation();
  const settings = useSettingsMap();
  const { periodAsked, setPeriodAsked } = useUiStore();
  const setPeriod = useBillingPeriodStore((state) => state.setPeriod);
  const today = periodOf();
  const [other, setOther] = useState(today);
  const [dontAsk, setDontAsk] = useState(false);
  const currentButton = useRef<HTMLButtonElement>(null);
  const loaded = settings['ask-period-on-start'] !== undefined;
  const open = loaded && !periodAsked && settings['ask-period-on-start'] !== 'false';

  useEffect(() => {
    // After the modal focused its first field: the current month is the usual choice
    if (open) requestAnimationFrame(() => currentButton.current?.focus());
  }, [open]);

  if (!open) return null;
  const choose = (period: string) => {
    setPeriod(period);
    setPeriodAsked();
    if (dontAsk) changeSettings({ 'ask-period-on-start': 'false' }).catch(reportError);
  };

  return (
    <Modal title={t('period.askTitle')} onClose={() => choose(today)}>
      <div className="flex flex-col gap-4 p-2">
        <p className="text-sm text-(--color-text-muted)">{t('period.askHint')}</p>
        <ul className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {OFFSETS.map((offset) => {
            const period = shiftPeriod(today, offset);
            const current = offset === 0;
            return (
              <li key={period}>
                <button
                  ref={current ? currentButton : undefined}
                  type="button"
                  className={`flex w-full flex-col items-center rounded-md border px-3 py-3 ${
                    current
                      ? 'border-(--color-accent) bg-(--color-accent) text-(--color-surface) shadow'
                      : 'border-(--color-border) hover:bg-(--color-surface-muted)'
                  }`}
                  onClick={() => choose(period)}
                >
                  <span className="font-semibold first-letter:uppercase">{formatPeriod(period, i18n.language)}</span>
                  {current && <span className="text-xs uppercase">{t('period.current')}</span>}
                </button>
              </li>
            );
          })}
        </ul>
        <form
          className="flex flex-wrap items-end gap-2"
          onSubmit={(event) => {
            event.preventDefault();
            if (other) choose(other);
          }}
        >
          <Field label={t('period.other')} htmlFor="startup-period">
            <input id="startup-period" type="month" className="input" value={other} onChange={(e) => setOther(e.target.value)} />
          </Field>
          <button type="submit" className="button">
            {t('period.open')}
          </button>
        </form>
        <label className="check-field text-sm">
          <input type="checkbox" checked={dontAsk} onChange={(e) => setDontAsk(e.target.checked)} />
          {t('period.dontAsk')}
        </label>
      </div>
    </Modal>
  );
}
