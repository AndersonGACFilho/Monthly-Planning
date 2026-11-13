import { isTauri } from '@tauri-apps/api/core';
import { FolderOpen } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { changeDiagnostics, useDiagnostics } from '../../api/generated/endpoints/diagnostics-controller/diagnostics-controller';
import { reportError } from '../../app/queryClient';
import { revealFile } from '../../shared/desktop';
import { useSaved } from '../../shared/hooks/useSaved';
import { useDebugMode } from '../../shared/state/debugMode';

/**
 * Debug mode to trace errors and where the log is (RNF-11): while on, the app logs in detail and every error notice
 * shows its code, request id and address, with a button to copy them.
 *
 * @returns the section
 */
export function DiagnosticsSettings() {
  const { t } = useTranslation();
  const saved = useSaved();
  const { data } = useDiagnostics();
  const setDebugMode = useDebugMode((state) => state.set);

  const change = async (debugMode: boolean) => {
    try {
      const result = await changeDiagnostics({ debugMode });
      setDebugMode(Boolean(result.debugMode));
      await saved();
    } catch (error) {
      reportError(error);
    }
  };

  return (
    <section className="panel flex flex-col gap-3" aria-labelledby="diagnostics-title">
      <h2 id="diagnostics-title" className="text-lg font-semibold">
        {t('diagnostics.title')}
      </h2>
      <label className="check-field">
        <input type="checkbox" checked={Boolean(data?.debugMode)} onChange={(e) => void change(e.target.checked)} />
        {t('diagnostics.debugMode')}
      </label>
      <p className="text-sm text-(--color-text-muted)">{t('diagnostics.debugModeHint')}</p>
      {data?.logFolder && (
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <span>
            {t('diagnostics.logFolder')}: <code className="break-all">{data.logFolder}</code>
          </span>
          {isTauri() && (
            <button type="button" className="button" onClick={() => void revealFile(data.logFolder!)}>
              <FolderOpen size={14} aria-hidden /> {t('diagnostics.openLogFolder')}
            </button>
          )}
        </div>
      )}
    </section>
  );
}
