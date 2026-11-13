import { create } from 'zustand';

interface DebugModeState {
  enabled: boolean;
  set: (enabled: boolean) => void;
}

/**
 * Whether the debug mode is on (RNF-11); loaded from the backend when the app opens and changed in the settings.
 */
export const useDebugMode = create<DebugModeState>((set) => ({
  enabled: false,
  set: (enabled) => set({ enabled }),
}));
