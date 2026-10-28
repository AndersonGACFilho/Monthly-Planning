import { X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { type Notice, useNotifications } from '../state/notifications';

/** How long a notice stays on screen, in milliseconds, by kind. */
export const NOTICE_TIMEOUT: Record<Notice['kind'], number> = { success: 4000, error: 8000 };

/**
 * Shows API errors and confirmations in the corner of the window (IMP-15). Each notice closes by itself
 * after {@link NOTICE_TIMEOUT}, except while the pointer or the focus is on it.
 *
 * @returns the notice stack
 */
export function Toaster() {
  const { notices, dismiss } = useNotifications();
  return (
    <div aria-live="polite" className="fixed right-4 bottom-16 z-50 flex w-96 flex-col gap-2">
      {notices.map((notice) => (
        <Toast key={notice.id} notice={notice} dismiss={dismiss} />
      ))}
    </div>
  );
}

/**
 * One notice with its close button and its own timer.
 *
 * @param props - the notice and the store action that removes it (stable, so new notices keep the timer)
 * @returns the notice
 */
function Toast({ notice, dismiss }: { notice: Notice; dismiss: (id: number) => void }) {
  const { t } = useTranslation();
  const [held, setHeld] = useState(false);

  useEffect(() => {
    if (held) return undefined;
    const timer = setTimeout(() => dismiss(notice.id), NOTICE_TIMEOUT[notice.kind]);
    return () => clearTimeout(timer);
  }, [held, notice.id, notice.kind, dismiss]);

  return (
    <div
      role={notice.kind === 'error' ? 'alert' : 'status'}
      onPointerEnter={() => setHeld(true)}
      onPointerLeave={() => setHeld(false)}
      onFocus={() => setHeld(true)}
      onBlur={() => setHeld(false)}
      className={`flex items-start gap-2 rounded border p-3 text-sm shadow ${
        notice.kind === 'error'
          ? 'border-red-300 bg-red-50 text-red-900 dark:border-red-800 dark:bg-red-950 dark:text-red-100'
          : 'border-emerald-300 bg-emerald-50 text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-100'
      }`}
    >
      <div className="flex-1">
        <p className="font-medium">{notice.title}</p>
        {notice.detail && <p>{notice.detail}</p>}
      </div>
      <button type="button" onClick={() => dismiss(notice.id)} aria-label={t('action.close')}>
        <X size={16} aria-hidden />
      </button>
    </div>
  );
}
