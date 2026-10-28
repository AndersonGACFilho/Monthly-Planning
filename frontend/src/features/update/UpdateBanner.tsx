import { X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal } from '../../shared/components/Modal';
import { useUpdateStore } from './updateStore';

/** Phases in which the notice shows; the others belong to the settings section. */
const SHOWN = new Set(['available', 'downloading', 'downloadFailed', 'ready']);

/**
 * Notice at the top of the window when a newer version exists (POR-16): release notes, download with progress and
 * restart. Looks once per start, stays silent when offline and can always be left for later.
 *
 * @returns the notice, or nothing
 */
export function UpdateBanner() {
  const { t } = useTranslation();
  const { phase, info, progress, error, dismissed, check, download, restart, dismiss } = useUpdateStore();
  const [notesOpen, setNotesOpen] = useState(false);

  useEffect(() => {
    // once per start: the layout can mount more than once, the store outlives it
    if (useUpdateStore.getState().phase === 'idle') void check();
  }, [check]);

  if (!info || dismissed || !SHOWN.has(phase)) return null;
  const percent = progress && progress.total > 0 ? Math.floor((progress.downloaded * 100) / progress.total) : 0;

  return (
    <div role="status" className="flex flex-wrap items-center gap-3 border-b border-(--color-border) bg-(--color-surface-muted) px-4 py-2 text-sm">
      {phase === 'available' && (
        <>
          <span className="font-medium">{t('update.available', { version: info.version })}</span>
          <button type="button" className="button button-primary" onClick={() => void download()}>
            {t('update.install')}
          </button>
        </>
      )}
      {phase === 'downloading' && (
        <>
          <span>{t('update.downloading', { version: info.version, percent })}</span>
          <progress className="w-48" max={100} value={percent} aria-label={t('update.progress')} />
        </>
      )}
      {phase === 'downloadFailed' && (
        <>
          <span className="text-red-700">{t('update.downloadFailed', { error })}</span>
          <button type="button" className="button" onClick={() => void download()}>
            {t('update.retry')}
          </button>
        </>
      )}
      {phase === 'ready' && (
        <>
          <span className="font-medium">{t('update.ready', { version: info.version })}</span>
          <button type="button" className="button button-primary" onClick={() => void restart()}>
            {t('update.restart')}
          </button>
          {error && <span className="text-red-700">{t('update.restartFailed', { error })}</span>}
        </>
      )}
      <button type="button" className="button" onClick={() => setNotesOpen(true)}>
        {t('update.notes')}
      </button>
      {phase !== 'downloading' && (
        <button type="button" className="icon-button ml-auto" aria-label={t('update.later')} title={t('update.later')} onClick={dismiss}>
          <X size={16} aria-hidden />
        </button>
      )}
      {notesOpen && (
        <Modal title={t('update.notesTitle', { version: info.version })} onClose={() => setNotesOpen(false)}>
          <pre className="max-h-[60vh] overflow-auto p-4 font-sans text-sm whitespace-pre-wrap">{info.notes}</pre>
        </Modal>
      )}
    </div>
  );
}
