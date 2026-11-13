import { create } from 'zustand';

/** Interface state shared between the window frame and the screens. */
interface UiState {
  /** Whether the spreadsheet export dialog is open (UI-01 Ctrl+E). */
  exportOpen: boolean;
  /** Opens or closes the export dialog. */
  setExportOpen: (open: boolean) => void;
  /** Database exports asked from the keyboard (IMP-69 Ctrl+Shift+E); the top bar button runs each new one. */
  databaseExportRequests: number;
  /** Asks the top bar to export the database. */
  requestDatabaseExport: () => void;
  /** Whether the billing period was already asked in this launch (IMP-56). */
  periodAsked: boolean;
  /** Records that the billing period was asked. */
  setPeriodAsked: () => void;
}

/**
 * Store of window-wide interface state.
 */
export const useUiStore = create<UiState>((set) => ({
  exportOpen: false,
  setExportOpen: (exportOpen) => set({ exportOpen }),
  databaseExportRequests: 0,
  requestDatabaseExport: () => set((state) => ({ databaseExportRequests: state.databaseExportRequests + 1 })),
  periodAsked: false,
  setPeriodAsked: () => set({ periodAsked: true }),
}));
