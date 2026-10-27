import { create } from 'zustand';

/** Interface state shared between the window frame and the screens. */
interface UiState {
  /** Whether the spreadsheet export dialog is open (UI-01 Ctrl+E). */
  exportOpen: boolean;
  /** Opens or closes the export dialog. */
  setExportOpen: (open: boolean) => void;
}

/**
 * Store of window-wide interface state.
 */
export const useUiStore = create<UiState>((set) => ({
  exportOpen: false,
  setExportOpen: (exportOpen) => set({ exportOpen }),
}));
