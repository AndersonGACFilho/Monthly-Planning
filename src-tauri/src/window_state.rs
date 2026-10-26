//! Remembers window size and position in `data/window-state.json` (UI-10), never in the OS config dir
//! (POR-03).

use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};
use tauri::{PhysicalPosition, PhysicalSize, Runtime, WebviewWindow};

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

/// Captures the current state of a window.
pub fn capture<R: Runtime>(window: &WebviewWindow<R>) -> Option<WindowState> {
    let maximized = window.is_maximized().unwrap_or(false);
    let position = window.outer_position().ok()?;
    let size = window.inner_size().ok()?;
    Some(WindowState { x: position.x, y: position.y, width: size.width, height: size.height, maximized })
}

/// Applies a saved state; the position is only used if it is on a connected monitor.
pub fn apply<R: Runtime>(window: &WebviewWindow<R>, state: &WindowState) {
    let on_screen = window
        .available_monitors()
        .map(|monitors| {
            monitors.iter().any(|m| {
                let (p, s) = (m.position(), m.size());
                state.x >= p.x && state.y >= p.y && state.x < p.x + s.width as i32 && state.y < p.y + s.height as i32
            })
        })
        .unwrap_or(false);
    let _ = window.set_size(PhysicalSize::new(state.width.max(800), state.height.max(600)));
    if on_screen {
        let _ = window.set_position(PhysicalPosition::new(state.x, state.y));
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
