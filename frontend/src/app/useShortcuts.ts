import { useEffect } from 'react';
import { useNavigate } from 'react-router';
import { useUiStore } from '../shared/state/ui';

/**
 * Keyboard shortcuts of the window (UI-01): Ctrl+N new card entry, Ctrl+I import, Ctrl+E export the monthly
 * spreadsheet, Ctrl+Shift+E export the database (IMP-69), Ctrl+F search on the current screen. Alt+←/→ (billing
 * period) lives in the period selector.
 */
export function useShortcuts(): void {
  const navigate = useNavigate();
  const setExportOpen = useUiStore((state) => state.setExportOpen);
  const requestDatabaseExport = useUiStore((state) => state.requestDatabaseExport);
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (!(event.ctrlKey || event.metaKey) || event.altKey) return;
      const key = event.key.toLowerCase();
      if (event.shiftKey) {
        if (key === 'e') {
          event.preventDefault();
          requestDatabaseExport();
        }
        return;
      }
      if (key === 'n') {
        event.preventDefault();
        navigate('/entries?new=1');
      } else if (key === 'i') {
        event.preventDefault();
        navigate('/import');
      } else if (key === 'e') {
        event.preventDefault();
        setExportOpen(true);
      } else if (key === 'f') {
        const search = document.querySelector<HTMLInputElement>('main input[type=search]');
        if (search) {
          event.preventDefault();
          search.focus();
          search.select();
        }
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [navigate, setExportOpen, requestDatabaseExport]);
}
