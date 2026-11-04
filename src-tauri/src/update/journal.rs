//! Journal of an update (POR-16): which step it reached, so a crash or a power loss at any point ends in either
//! the old or the new version, never in a mix.

use crate::files;
use serde::{Deserialize, Serialize};
use std::fs;
use std::io;
use std::path::{Path, PathBuf};

/// A top-level file or folder of the app replaced by the update.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct Entry {
    pub name: String,
    /// Whether the app folder had it before; entries new in the release are removed on rollback.
    pub existed: bool,
}

/// The files an update swaps.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct Swap {
    pub version: String,
    pub entries: Vec<Entry>,
}

/// Step reached by the update.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "step", rename_all = "camelCase")]
pub enum UpdateState {
    /// The release is downloaded, verified and extracted; it is moved in at the next start.
    Staged { version: String },
    /// Files are being moved; finding this at start means the move was interrupted.
    Applying(Swap),
    /// The files are in place; the new version is kept once its backend starts.
    Applied(Swap),
}

/// The journal file, replaced atomically on every step.
pub struct Journal {
    path: PathBuf,
}

impl Journal {
    pub fn new(path: &Path) -> Journal {
        Journal { path: path.to_path_buf() }
    }

    /// The step reached; `None` without an update in progress, an error when the file cannot be read.
    pub fn read(&self) -> io::Result<Option<UpdateState>> {
        match fs::read(&self.path) {
            Ok(bytes) => serde_json::from_slice(&bytes).map(Some).map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e)),
            Err(e) if e.kind() == io::ErrorKind::NotFound => Ok(None),
            Err(e) => Err(e),
        }
    }

    /// Records `state`: written to a temporary file, flushed, then renamed over the journal.
    pub fn write(&self, state: &UpdateState) -> io::Result<()> {
        files::write_replacing(&self.path, &serde_json::to_vec_pretty(state).map_err(io::Error::other)?)
    }

    /// Ends the update: without a journal the app starts as usual.
    pub fn clear(&self) -> io::Result<()> {
        files::remove_file_if_present(&self.path)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn por_16_journal_round_trip() {
        let folder = tempfile::tempdir().unwrap();
        let journal = Journal::new(&folder.path().join("state.json"));
        assert_eq!(journal.read().unwrap(), None);
        let applied = UpdateState::Applied(Swap {
            version: "0.2.0".into(),
            entries: vec![Entry { name: "backend".into(), existed: true }],
        });
        journal.write(&UpdateState::Staged { version: "0.2.0".into() }).unwrap();
        journal.write(&applied).unwrap();
        assert_eq!(journal.read().unwrap(), Some(applied));
        assert!(!folder.path().join("state.json.tmp").exists());
        journal.clear().unwrap();
        journal.clear().unwrap();
        assert_eq!(journal.read().unwrap(), None);
    }

    #[test]
    fn por_16_unreadable_journal_is_an_error() {
        let folder = tempfile::tempdir().unwrap();
        fs::write(folder.path().join("state.json"), "{not json").unwrap();
        assert!(Journal::new(&folder.path().join("state.json")).read().is_err());
    }
}
