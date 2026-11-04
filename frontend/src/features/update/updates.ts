import { invoke, isTauri } from '@tauri-apps/api/core';

/** A newer version published on GitHub (POR-16). */
export interface UpdateInfo {
  /** Version such as `0.2.0`. */
  version: string;
  /** Release notes of this version and of every skipped one, newest first. */
  notes: string;
}

/** Bytes of the release zip downloaded so far. */
export interface DownloadProgress {
  downloaded: number;
  total: number;
}

/**
 * Asks the desktop shell for a newer version. The browser preview has nothing to update.
 *
 * @returns the newer version, or `null` when up to date or outside the desktop app
 */
export async function checkForUpdate(): Promise<UpdateInfo | null> {
  if (!isTauri()) return null;
  return invoke<UpdateInfo | null>('update_check');
}

/**
 * Downloads, verifies and prepares the version found by {@link checkForUpdate}; it is installed when the app
 * restarts.
 *
 * @param onProgress - called as the download advances
 */
export async function downloadUpdate(onProgress: (progress: DownloadProgress) => void): Promise<void> {
  const { listen } = await import('@tauri-apps/api/event');
  const stop = await listen<DownloadProgress>('update-progress', (event) => onProgress(event.payload));
  try {
    await invoke('update_download');
  } finally {
    stop();
  }
}

/**
 * Closes the app and opens it again, which installs the prepared update before the window shows.
 */
export async function restartToUpdate(): Promise<void> {
  await invoke('update_restart');
}

/**
 * Version of the running app.
 *
 * @returns the version, or `null` outside the desktop app
 */
export async function installedVersion(): Promise<string | null> {
  if (!isTauri()) return null;
  const { getVersion } = await import('@tauri-apps/api/app');
  return getVersion();
}
