//! In-app updates of the portable folder (POR-16).
//!
//! A newer GitHub release is downloaded, checked against its SHA-256 and extracted into `update/staged/` while the
//! app runs (`Updater`). At the next start, before the window and the backend, its files replace the app's (the old
//! ones go to `update/previous/`) and the shell restarts into the new version (`Installer`, `Relaunch`). Once the
//! new backend answers, the update is kept; if it does not start, the old files are put back. `data/` is never
//! touched.

pub mod commands;

mod archive;
mod download;
mod error;
mod install;
mod journal;
mod layout;
mod net;
mod release;
mod relaunch;
mod swap;
mod updater;
mod version;

#[cfg(test)]
mod flow_tests;
#[cfg(test)]
mod test_support;

pub use error::UpdateError;
pub use install::Installer;
pub use relaunch::Relaunch;
pub use updater::Updater;
