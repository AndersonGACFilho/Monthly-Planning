import { invoke } from '@tauri-apps/api/core';

/** Whether this computer finds the app in its system menu (POR-18). */
export type ShortcutStatus =
  | { state: 'absent' }
  | { state: 'current' }
  /** The entry opens the app from another folder, such as the drive under another letter or mount path. */
  | { state: 'elsewhere'; folder: string };

/**
 * Asks the desktop shell whether the system menu of this computer has the app.
 *
 * @returns where the entry stands; rejects where the system has no menu the app can use
 */
export async function shortcutStatus(): Promise<ShortcutStatus> {
  return invoke<ShortcutStatus>('menu_shortcut_status');
}

/**
 * Adds the app to the system menu (Start Menu on Windows, app launcher on Linux), or points the existing entry at
 * this copy of the app.
 */
export async function addShortcut(): Promise<void> {
  await invoke('menu_shortcut_add');
}

/**
 * Removes the app from the system menu, with every file the entry took.
 */
export async function removeShortcut(): Promise<void> {
  await invoke('menu_shortcut_remove');
}
