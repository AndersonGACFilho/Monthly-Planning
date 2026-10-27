import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNotifications } from '../state/notifications';

/**
 * Shows API errors and confirmations in the corner of the window (IMP-15).
 *
 * @returns the notice stack
 */
export function Toaster() {
  const { t } = useTranslation();
  const { notices, dismiss } = useNotifications();
  return (
    <div aria-live="polite" className="fixed right-4 bottom-16 z-50 flex w-96 flex-col gap-2">
      {notices.map((notice) => (
        <div
          key={notice.id}
          role={notice.kind === 'error' ? 'alert' : 'status'}
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
      ))}
    </div>
  );
}
