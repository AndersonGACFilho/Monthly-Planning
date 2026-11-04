//! Where an update lives inside the app folder (POR-03, POR-16): `update/` holds the download, the extracted
//! release, the replaced files kept for rollback and the journal. Names are exFAT-safe (POR-02).

use std::path::{Path, PathBuf};

/// Paths of an update for one app folder.
#[derive(Clone, Debug)]
pub struct Layout {
    home: PathBuf,
    root: PathBuf,
}

impl Layout {
    /// Layout below the app folder `home`.
    pub fn new(home: &Path) -> Layout {
        Layout { home: home.to_path_buf(), root: crate::app_home::update_dir(home) }
    }

    /// The app folder whose files are replaced.
    pub fn home(&self) -> &Path {
        &self.home
    }

    /// `update/` itself.
    pub fn root(&self) -> &Path {
        &self.root
    }

    /// Download in progress.
    pub fn download(&self) -> PathBuf {
        self.root.join("download")
    }

    /// The new release, extracted and ready to be moved in.
    pub fn staged(&self) -> PathBuf {
        self.root.join("staged")
    }

    /// Files the new release replaced, moved back on rollback.
    pub fn previous(&self) -> PathBuf {
        self.root.join("previous")
    }

    /// Files taken out on rollback; a running program can be moved but not deleted, so they go here first.
    pub fn trash(&self) -> PathBuf {
        self.root.join("trash")
    }

    /// Journal of the update in progress.
    pub fn journal(&self) -> PathBuf {
        self.root.join("state.json")
    }
}
