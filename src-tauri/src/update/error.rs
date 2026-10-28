//! Why an update could not be found, downloaded or staged (POR-16).

use std::fmt;
use std::io;

#[derive(Debug)]
pub enum UpdateError {
    /// GitHub could not be reached or answered with an error.
    Network(String),
    /// The release does not have the files an update needs, or they are malformed.
    Release(String),
    /// The download does not match the published size or SHA-256.
    Integrity(String),
    /// The zip holds something that is not a portable folder.
    Archive(String),
    /// Reading or writing the app folder failed.
    Io(io::Error),
    /// Nothing to install: no newer release was found by the last check.
    NothingToInstall,
    /// Another download is running.
    Busy,
    /// Development builds never replace their own files.
    Unsupported,
}

impl fmt::Display for UpdateError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            UpdateError::Network(reason) => write!(f, "network: {reason}"),
            UpdateError::Release(reason) => write!(f, "release: {reason}"),
            UpdateError::Integrity(reason) => write!(f, "integrity: {reason}"),
            UpdateError::Archive(reason) => write!(f, "archive: {reason}"),
            UpdateError::Io(e) => write!(f, "io: {e}"),
            UpdateError::NothingToInstall => write!(f, "no newer version to install"),
            UpdateError::Busy => write!(f, "an update is already downloading"),
            UpdateError::Unsupported => write!(f, "development builds do not install updates"),
        }
    }
}

impl From<io::Error> for UpdateError {
    fn from(e: io::Error) -> Self {
        UpdateError::Io(e)
    }
}

impl From<ureq::Error> for UpdateError {
    fn from(e: ureq::Error) -> Self {
        match e {
            ureq::Error::Io(e) => UpdateError::Io(e),
            other => UpdateError::Network(other.to_string()),
        }
    }
}
