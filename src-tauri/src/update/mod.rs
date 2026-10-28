//! In-app updates of the portable folder (POR-16).
//!
//! A newer GitHub release is downloaded, checked against its SHA-256 and extracted into `update/staged/` while the
//! app runs. At the next start, before the window and the backend, its files replace the app's (the old ones go
//! to `update/previous/`) and the shell restarts into the new version. Once the new backend answers, the update
//! is kept; if it does not start, the old files are put back. `data/` is never touched.

pub mod commands;

mod archive;
mod download;
mod error;
mod journal;
mod layout;
mod net;
mod release;
mod relaunch;
mod swap;
mod version;

#[cfg(test)]
mod flow_tests;

pub use download::Progress;
pub use error::UpdateError;
pub use release::Release;

use journal::{Journal, Swap, UpdateState};
use layout::Layout;
use relaunch::Relaunch;
use semver::Version;
use std::env;
use std::fs;
use std::io;
use std::path::Path;
use std::sync::Mutex;

/// Updates of one app folder; managed as Tauri state.
pub struct Updater {
    layout: Layout,
    journal: Journal,
    relaunch: Relaunch,
    releases_url: String,
    found: Mutex<Option<Release>>,
    downloading: Mutex<()>,
    start_failure: Mutex<Option<String>>,
}

impl Updater {
    /// Updater for the app folder `home`. `PM_UPDATE_URL` replaces the GitHub release list (tests).
    ///
    /// Created first thing at start: it records how to relaunch the app before any file is renamed.
    pub fn new(home: &Path) -> Updater {
        let releases_url = env::var("PM_UPDATE_URL").unwrap_or_else(|_| release::RELEASES_URL.to_string());
        Updater::with_releases_url(home, releases_url)
    }

    fn with_releases_url(home: &Path, releases_url: String) -> Updater {
        let layout = Layout::new(home);
        Updater {
            journal: Journal::new(&layout.journal()),
            layout,
            relaunch: Relaunch::current(),
            releases_url,
            found: Mutex::new(None),
            downloading: Mutex::new(()),
            start_failure: Mutex::new(None),
        }
    }

    /// Runs at start, before the window and the backend: moves a staged release in, or undoes a move that was
    /// interrupted. Returns `true` when the process must restart into the version now in the folder.
    pub fn resume(&self) -> bool {
        match self.journal.read() {
            Ok(None) => {
                let _ = swap::clean(&self.layout);
                false
            }
            Ok(Some(UpdateState::Staged { version })) => self.install(&version),
            Ok(Some(UpdateState::Applying(swap))) => {
                // the move stopped midway (crash, power loss): this process may be either version
                self.undo(&swap);
                true
            }
            Ok(Some(UpdateState::Applied(_))) => false,
            Err(e) => {
                self.note_failure(&e.to_string());
                false
            }
        }
    }

    /// Called once the backend answered: the version that just started stays and `update/` goes away.
    pub fn confirm_started(&self) {
        if let Ok(Some(UpdateState::Applied(_))) = self.journal.read() {
            if self.journal.clear().is_ok() {
                let _ = swap::clean(&self.layout);
            }
        }
    }

    /// Called when the backend did not start. Undoes the update applied by the previous run, if any, and returns
    /// its version; the shell then restarts into the previous version.
    pub fn undo_failed_start(&self) -> Option<String> {
        match self.journal.read() {
            Ok(Some(UpdateState::Applied(swap))) if self.undo(&swap) => Some(swap.version),
            _ => None,
        }
    }

    /// Why installing a staged update failed at start, reported once.
    pub fn take_start_failure(&self) -> Option<String> {
        self.start_failure.lock().unwrap().take()
    }

    /// Looks for a release newer than `current` and remembers it for `download`.
    pub fn check(&self, current: &Version) -> Result<Option<Release>, UpdateError> {
        let found = release::fetch_newer(&self.releases_url, current)?;
        *self.found.lock().unwrap() = found.clone();
        Ok(found)
    }

    /// Downloads, verifies and extracts the release found by the last check; it is moved in at the next start.
    pub fn download(&self, progress: Progress) -> Result<(), UpdateError> {
        let _busy = self.downloading.try_lock().map_err(|_| UpdateError::Busy)?;
        let release = self.found.lock().unwrap().clone().ok_or(UpdateError::NothingToInstall)?;
        let staged = self.stage(&release, progress);
        if staged.is_err() {
            let _ = fs::remove_dir_all(self.layout.download());
            let _ = fs::remove_dir_all(self.layout.staged());
        }
        staged
    }

    /// Starts the app again and returns the exit code this process must end with.
    pub fn restart(&self) -> io::Result<i32> {
        self.relaunch.start()
    }

    fn stage(&self, release: &Release, progress: Progress) -> Result<(), UpdateError> {
        self.journal.clear()?;
        swap::remove_dir_if_present(&self.layout.staged())?;
        swap::remove_dir_if_present(&self.layout.download())?;
        fs::create_dir_all(self.layout.download())?;
        let sha256 = download::fetch_checksum(&release.checksum)?;
        let zip = self.layout.download().join(&release.package.name);
        download::download(&release.package, &sha256, &zip, progress)?;
        archive::extract(&zip, &self.layout.staged())?;
        fs::remove_dir_all(self.layout.download())?;
        self.journal.write(&UpdateState::Staged { version: release.version.to_string() })?;
        Ok(())
    }

    fn install(&self, version: &str) -> bool {
        let swap = match swap::prepare(&self.layout, version) {
            Ok(swap) => swap,
            Err(e) => return self.abandon(&e),
        };
        if let Err(e) = self.journal.write(&UpdateState::Applying(swap.clone())) {
            return self.abandon(&e);
        }
        match swap::apply(&self.layout, &swap).and_then(|()| self.journal.write(&UpdateState::Applied(swap.clone()))) {
            Ok(()) => true,
            Err(e) => {
                self.note_failure(&e.to_string());
                self.undo(&swap);
                false
            }
        }
    }

    /// Gives up a staged update that could not start moving; the app folder is unchanged.
    fn abandon(&self, error: &io::Error) -> bool {
        self.note_failure(&error.to_string());
        let _ = self.journal.clear();
        let _ = swap::clean(&self.layout);
        false
    }

    /// Puts the previous version back and ends the update. When that fails the journal stays, so the next start
    /// tries again.
    fn undo(&self, swap: &Swap) -> bool {
        match swap::roll_back(&self.layout, swap) {
            Ok(()) => {
                let _ = self.journal.clear();
                let _ = swap::clean(&self.layout);
                true
            }
            Err(e) => {
                self.note_failure(&e.to_string());
                false
            }
        }
    }

    fn note_failure(&self, reason: &str) {
        *self.start_failure.lock().unwrap() = Some(reason.to_string());
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;

    fn updater_with_staged_release() -> (tempfile::TempDir, PathBuf, Updater) {
        let folder = tempfile::tempdir().unwrap();
        let home = folder.path().join("PlanejamentoMensal");
        let updater = Updater::new(&home);
        for (base, version) in [(home.clone(), "old"), (updater.layout.staged(), "new")] {
            fs::create_dir_all(base.join("backend")).unwrap();
            fs::write(base.join("backend/app.jar"), version).unwrap();
        }
        fs::create_dir_all(home.join("data")).unwrap();
        fs::write(home.join("data/planejamento-mensal.db"), "user data").unwrap();
        updater.journal.write(&UpdateState::Staged { version: "0.2.0".into() }).unwrap();
        (folder, home, updater)
    }

    fn jar(home: &Path) -> String {
        fs::read_to_string(home.join("backend/app.jar")).unwrap()
    }

    #[test]
    fn por_16_staged_release_is_moved_in_and_kept_after_a_good_start() {
        let (_folder, home, updater) = updater_with_staged_release();
        assert!(updater.resume());
        assert_eq!(jar(&home), "new");
        assert!(!updater.resume());
        updater.confirm_started();
        assert!(!home.join("update").exists());
        assert_eq!(fs::read_to_string(home.join("data/planejamento-mensal.db")).unwrap(), "user data");
    }

    #[test]
    fn por_16_failed_start_puts_previous_version_back() {
        let (_folder, home, updater) = updater_with_staged_release();
        updater.resume();
        assert_eq!(updater.undo_failed_start().as_deref(), Some("0.2.0"));
        assert_eq!(jar(&home), "old");
        assert!(!home.join("update").exists());
        assert_eq!(updater.undo_failed_start(), None);
        assert_eq!(fs::read_to_string(home.join("data/planejamento-mensal.db")).unwrap(), "user data");
    }

    #[test]
    fn por_16_interrupted_move_is_undone_at_next_start() {
        let (_folder, home, updater) = updater_with_staged_release();
        let swap = swap::prepare(&updater.layout, "0.2.0").unwrap();
        updater.journal.write(&UpdateState::Applying(swap)).unwrap();
        swap::move_path(&home.join("backend"), &updater.layout.previous().join("backend")).unwrap();
        assert!(updater.resume());
        assert_eq!(jar(&home), "old");
        assert!(!home.join("update").exists());
    }

    #[test]
    fn por_16_unreadable_journal_leaves_folder_untouched() {
        let (_folder, home, updater) = updater_with_staged_release();
        fs::write(updater.layout.journal(), "{broken").unwrap();
        assert!(!updater.resume());
        assert!(updater.take_start_failure().is_some());
        assert!(updater.layout.staged().exists());
        assert_eq!(jar(&home), "old");
    }

    #[test]
    fn por_16_leftovers_without_journal_are_removed() {
        let (_folder, home, updater) = updater_with_staged_release();
        updater.journal.clear().unwrap();
        assert!(!updater.resume());
        assert!(!home.join("update").exists());
        assert_eq!(jar(&home), "old");
    }
}
