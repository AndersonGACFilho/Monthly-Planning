//! Opt-in entry of the app in the system menu (POR-18), so searching the computer finds it.
//!
//! The only file the shell writes outside the app folder besides the temporary copy of POR-09, and only when the
//! user asks for it in Ajustes: a Start Menu shortcut on Windows, a `.desktop` file and its icon in the
//! `applications` folder on Linux. It is never written at start. The drive may get another letter or mount path on
//! the next computer or session, so the shell compares the entry with what this run would write and the window
//! offers to fix it. Updates (POR-16) keep the executable names, so the entry keeps working across them.

pub mod commands;

#[cfg(target_os = "linux")]
mod applications;
#[cfg(target_os = "linux")]
mod desktop_entry;
mod launch;
mod shortcut;
#[cfg(windows)]
mod start_menu;
mod system_menu;

pub use launch::Launch;
pub use shortcut::{MenuShortcut, ShortcutError, ShortcutStatus};
pub use system_menu::SystemMenu;
