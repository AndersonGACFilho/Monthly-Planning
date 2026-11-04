//! Commands of the settings section that adds the app to the system menu (POR-18). Each runs off the main thread:
//! the shell link of Windows is a COM object, and the menu folder may be slow to answer.

use super::{MenuShortcut, ShortcutError, ShortcutStatus};
use tauri::{AppHandle, Manager};

/// Whether this computer has the app in its system menu, and where the entry points.
#[tauri::command]
pub async fn menu_shortcut_status(app: AppHandle) -> Result<ShortcutStatus, String> {
    with_shortcut(app, MenuShortcut::status).await
}

/// Adds the app to the system menu, or points the entry at this copy. Release builds only.
#[tauri::command]
pub async fn menu_shortcut_add(app: AppHandle) -> Result<(), String> {
    if cfg!(debug_assertions) {
        return Err(ShortcutError::DevelopmentBuild.to_string());
    }
    with_shortcut(app, MenuShortcut::add).await
}

/// Removes the app from the system menu.
#[tauri::command]
pub async fn menu_shortcut_remove(app: AppHandle) -> Result<(), String> {
    with_shortcut(app, MenuShortcut::remove).await
}

async fn with_shortcut<T: Send + 'static>(
    app: AppHandle,
    action: fn(&MenuShortcut) -> Result<T, ShortcutError>,
) -> Result<T, String> {
    tauri::async_runtime::spawn_blocking(move || action(&app.state::<MenuShortcut>()).map_err(|e| e.to_string()))
        .await
        .map_err(|e| e.to_string())?
}
