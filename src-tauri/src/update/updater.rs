//! Finding a newer release and staging it while the app runs (POR-16).

use super::download::{self, Progress};
use super::error::UpdateError;
use super::journal::{Journal, UpdateState};
use super::layout::Layout;
use super::release::{self, Release};
use super::{archive, swap};
use semver::Version;
use std::env;
use std::fs;
use std::path::Path;
use std::sync::Mutex;

/// Looks for a newer release and downloads it into `update/staged/`; the `Installer` moves it in at next start.
pub struct Updater {
    layout: Layout,
    journal: Journal,
    releases_url: String,
    found: Mutex<Option<Release>>,
    downloading: Mutex<()>,
}

impl Updater {
    /// Updater for the app folder `home`. `PM_UPDATE_URL` replaces the GitHub release list (tests).
    pub fn new(home: &Path) -> Updater {
        let releases_url = env::var("PM_UPDATE_URL").unwrap_or_else(|_| release::RELEASES_URL.to_string());
        Updater::with_releases_url(home, releases_url)
    }

    pub(super) fn with_releases_url(home: &Path, releases_url: String) -> Updater {
        let layout = Layout::new(home);
        Updater {
            journal: Journal::new(&layout.journal()),
            layout,
            releases_url,
            found: Mutex::new(None),
            downloading: Mutex::new(()),
        }
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
            let _ = swap::remove_dir_if_present(&self.layout.download());
            let _ = swap::remove_dir_if_present(&self.layout.staged());
        }
        staged
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

    #[cfg(test)]
    pub(super) fn layout(&self) -> &Layout {
        &self.layout
    }
}
