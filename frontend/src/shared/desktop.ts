import { isTauri } from '@tauri-apps/api/core';

/**
 * Opens a web link, such as a `wa.me` link, in the default browser or app (RF-87, POR-07).
 *
 * @param url - link to open
 */
export async function openExternal(url: string): Promise<void> {
  if (isTauri()) {
    const { openUrl } = await import('@tauri-apps/plugin-opener');
    await openUrl(url);
  } else {
    window.open(url, '_blank', 'noopener');
  }
}

/**
 * Opens a file saved by the app with its default program, such as a PDF for printing (RF-90). Only in the
 * desktop app; the browser preview cannot reach local files.
 *
 * @param path - absolute file path
 * @returns whether the file could be opened
 */
export async function openFile(path: string): Promise<boolean> {
  if (!isTauri()) return false;
  const { openPath } = await import('@tauri-apps/plugin-opener');
  await openPath(path);
  return true;
}

/**
 * Shows a file saved by the app in the system file manager (RF-89).
 *
 * @param path - absolute file path
 * @returns whether the folder could be opened
 */
export async function revealFile(path: string): Promise<boolean> {
  if (!isTauri()) return false;
  const { revealItemInDir } = await import('@tauri-apps/plugin-opener');
  await revealItemInDir(path);
  return true;
}

/**
 * Copies a PNG image to the clipboard (RF-87, RF-88).
 *
 * @param png - PNG image
 */
export async function copyImage(png: Blob): Promise<void> {
  if (isTauri()) {
    const { writeImage } = await import('@tauri-apps/plugin-clipboard-manager');
    await writeImage(new Uint8Array(await png.arrayBuffer()));
  } else {
    await navigator.clipboard.write([new ClipboardItem({ 'image/png': png })]);
  }
}

/**
 * Lets the user save a file fetched from the API.
 *
 * @param blob - file content
 * @param fileName - suggested file name
 */
export function downloadFile(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}
