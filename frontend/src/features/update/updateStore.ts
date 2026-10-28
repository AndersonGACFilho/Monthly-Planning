import { create } from 'zustand';
import { checkForUpdate, downloadUpdate, restartToUpdate, type DownloadProgress, type UpdateInfo } from './updates';

/** Where the update stands: shared by the notice at the top of the window and the settings section. */
export type UpdatePhase =
  | 'idle'
  | 'checking'
  | 'upToDate'
  | 'checkFailed'
  | 'available'
  | 'downloading'
  | 'downloadFailed'
  | 'ready';

interface UpdateState {
  phase: UpdatePhase;
  info?: UpdateInfo;
  progress?: DownloadProgress;
  error?: string;
  /** The user chose "later": the notice stays hidden until the next start or a new action. */
  dismissed: boolean;
  check: () => Promise<void>;
  download: () => Promise<void>;
  restart: () => Promise<void>;
  dismiss: () => void;
}

/**
 * Store of the in-app update (POR-16): check, download with progress, restart. Never forced: every step waits
 * for the user, and an offline check only changes the phase.
 */
export const useUpdateStore = create<UpdateState>((set, get) => ({
  phase: 'idle',
  dismissed: false,
  check: async () => {
    if (get().phase === 'checking' || get().phase === 'downloading' || get().phase === 'ready') return;
    set({ phase: 'checking', error: undefined });
    try {
      const info = await checkForUpdate();
      set(info ? { phase: 'available', info, dismissed: false } : { phase: 'upToDate', info: undefined });
    } catch (error) {
      set({ phase: 'checkFailed', error: String(error) });
    }
  },
  download: async () => {
    if (get().phase === 'downloading') return;
    set({ phase: 'downloading', progress: undefined, error: undefined, dismissed: false });
    try {
      await downloadUpdate((progress) => set({ progress }));
      set({ phase: 'ready' });
    } catch (error) {
      set({ phase: 'downloadFailed', error: String(error) });
    }
  },
  restart: async () => {
    try {
      await restartToUpdate();
    } catch (error) {
      // the update stays prepared and is installed at the next start
      set({ error: String(error) });
    }
  },
  dismiss: () => set({ dismissed: true }),
}));
