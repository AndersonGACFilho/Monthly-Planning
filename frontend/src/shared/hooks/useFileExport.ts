import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { reportError } from '../../app/queryClient';
import { downloadFile } from '../desktop';
import { useNotifications } from '../state/notifications';

/** Time the export buttons stay disabled after an export ends, so a double click never exports twice. */
export const EXPORT_COOLDOWN_MS = 1000;

/** A file fetched from the API. */
interface ExportedFile {
  /** Content. */
  blob: Blob;
  /** Name suggested by the API, or `null`. */
  fileName: string | null;
}

/**
 * Runs file exports with feedback (IMP-62): while a file is being prepared and for one second after it is saved the
 * buttons that use `busy` stay disabled, and the end shows a notice with the file name (or the error).
 *
 * @returns `busy` and `run(fetchFile, fallbackName)`, which resolves to `true` when the file was saved
 */
export function useFileExport() {
  const { t } = useTranslation();
  const push = useNotifications((state) => state.push);
  const [busy, setBusy] = useState(false);
  const running = useRef(false);
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
  useEffect(() => () => clearTimeout(timer.current), []);

  const run = useCallback(
    async (fetchFile: () => Promise<ExportedFile>, fallbackName: string): Promise<boolean> => {
      if (running.current) return false;
      running.current = true;
      setBusy(true);
      try {
        const file = await fetchFile();
        const name = file.fileName ?? fallbackName;
        downloadFile(file.blob, name);
        push({ kind: 'success', title: t('export.done', { file: name }) });
        return true;
      } catch (error) {
        reportError(error);
        return false;
      } finally {
        timer.current = setTimeout(() => {
          running.current = false;
          setBusy(false);
        }, EXPORT_COOLDOWN_MS);
      }
    },
    [push, t],
  );
  return { busy, run };
}
