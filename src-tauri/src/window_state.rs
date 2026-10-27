//! Remembers window size and position in `data/window-state.json` (UI-10), never in the OS config dir
//! (POR-03).

use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};
use tauri::{LogicalPosition, LogicalSize, Runtime, WebviewWindow};

#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub struct WindowState {
    pub x: i32,
    pub y: i32,
    pub width: u32,
    pub height: u32,
    pub maximized: bool,
}

fn file(data_dir: &Path) -> PathBuf {
    data_dir.join("window-state.json")
}

/// Reads the saved state, if any.
pub fn load(data_dir: &Path) -> Option<WindowState> {
    serde_json::from_str(&fs::read_to_string(file(data_dir)).ok()?).ok()
}

/// Writes the state; errors are ignored because losing it only resets the window size.
pub fn save(data_dir: &Path, state: &WindowState) {
    if let Ok(json) = serde_json::to_string_pretty(state) {
        let _ = fs::create_dir_all(data_dir);
        let _ = fs::write(file(data_dir), json);
    }
}

/// Captures the current state of a window in logical pixels, so it restores the same on any display scale.
/// Wayland does not tell windows where they are, so the position may be missing; the size and the maximized
/// flag are kept anyway.
pub fn capture<R: Runtime>(window: &WebviewWindow<R>) -> Option<WindowState> {
    let maximized = window.is_maximized().unwrap_or(false);
    let scale = window.scale_factor().unwrap_or(1.0);
    let size = window.inner_size().ok()?.to_logical::<f64>(scale);
    let (x, y) = window
        .outer_position()
        .map(|p| {
            let p = p.to_logical::<f64>(scale);
            (p.x.round() as i32, p.y.round() as i32)
        })
        .unwrap_or((0, 0));
    Some(WindowState { x, y, width: size.width.round() as u32, height: size.height.round() as u32, maximized })
}

/// Applies a saved state; the position is only used if it is on a connected monitor.
pub fn apply<R: Runtime>(window: &WebviewWindow<R>, state: &WindowState) {
    let on_screen = window
        .available_monitors()
        .map(|monitors| {
            monitors.iter().any(|m| {
                let scale = m.scale_factor();
                let p = m.position().to_logical::<f64>(scale);
                let s = m.size().to_logical::<f64>(scale);
                let (x, y) = (f64::from(state.x), f64::from(state.y));
                x >= p.x && y >= p.y && x < p.x + s.width && y < p.y + s.height
            })
        })
        .unwrap_or(false);
    let _ = window.set_size(LogicalSize::new(state.width.max(800), state.height.max(600)));
    if on_screen {
        let _ = window.set_position(LogicalPosition::new(state.x, state.y));
    }
    if state.maximized {
        let _ = window.maximize();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ui_10_state_round_trips_through_data_dir() {
        let dir = std::env::temp_dir().join(format!("pm-window-state-{}", std::process::id()));
        let state = WindowState { x: 10, y: 20, width: 1280, height: 800, maximized: false };
        save(&dir, &state);
        assert_eq!(load(&dir), Some(state));
        let _ = fs::remove_dir_all(dir);
    }
}
