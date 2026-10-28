//! Commands the window calls to find and download an update (POR-16). Restarting lives in the shell, which owns
//! the backend that must stop first.

use super::{UpdateError, Updater};
use serde::Serialize;
use tauri::{AppHandle, Emitter, Manager};

/// Event with the download progress, sent at most once per `PROGRESS_STEP` bytes.
pub const PROGRESS_EVENT: &str = "update-progress";
const PROGRESS_STEP: u64 = 1024 * 1024;

/// A newer version shown in the update notice.
#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UpdateInfo {
    pub version: String,
    /// Release notes of this version and of every skipped one.
    pub notes: String,
}

/// Bytes downloaded so far out of the zip size.
#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DownloadProgress {
    pub downloaded: u64,
    pub total: u64,
}

/// Looks for a release newer than this app; `None` when up to date.
#[tauri::command]
pub async fn update_check(app: AppHandle) -> Result<Option<UpdateInfo>, String> {
    let current = app.package_info().version.clone();
    tauri::async_runtime::spawn_blocking(move || {
        let found = app.state::<Updater>().check(&current).map_err(|e| e.to_string())?;
        Ok(found.map(|release| UpdateInfo { version: release.version.to_string(), notes: release.notes }))
    })
    .await
    .map_err(|e| e.to_string())?
}

/// Downloads and stages the release found by `update_check`, emitting `update-progress` along the way. Release
/// builds only.
#[tauri::command]
pub async fn update_download(app: AppHandle) -> Result<(), String> {
    // a development build runs from the repository, which is not a portable folder to replace
    if cfg!(debug_assertions) {
        return Err(UpdateError::Unsupported.to_string());
    }
    tauri::async_runtime::spawn_blocking(move || {
        let mut reported = 0u64;
        app.state::<Updater>()
            .download(&mut |downloaded, total| {
                if downloaded == total || downloaded - reported >= PROGRESS_STEP {
                    reported = downloaded;
                    let _ = app.emit(PROGRESS_EVENT, DownloadProgress { downloaded, total });
                }
            })
            .map_err(|e| e.to_string())
    })
    .await
    .map_err(|e| e.to_string())?
}
