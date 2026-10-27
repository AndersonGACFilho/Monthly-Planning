import { create } from 'zustand';

/** One message shown in the corner of the window. */
export interface Notice {
  id: number;
  kind: 'error' | 'success';
  title: string;
  detail?: string;
}

interface NotificationState {
  notices: Notice[];
  push: (notice: Omit<Notice, 'id'>) => void;
  dismiss: (id: number) => void;
}

let nextId = 1;

/**
 * Store of the notices shown by {@link Toaster}; API errors arrive here through the single interceptor of
 * the query client (IMP-15).
 */
export const useNotifications = create<NotificationState>((set) => ({
  notices: [],
  push: (notice) => set((state) => ({ notices: [...state.notices.slice(-4), { ...notice, id: nextId++ }] })),
  dismiss: (id) => set((state) => ({ notices: state.notices.filter((n) => n.id !== id) })),
}));
