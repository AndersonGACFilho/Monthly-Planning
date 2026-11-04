//! File writes that never leave a half-written file behind, and removals that accept a file already gone.

use std::ffi::OsString;
use std::fs::{self, File};
use std::io::{self, Write};
use std::path::{Path, PathBuf};

/// Writes `bytes` to a temporary file next to `path`, flushes it and renames it over `path`, so a crash or a power
/// loss leaves either the old content or the new one.
pub fn write_replacing(path: &Path, bytes: &[u8]) -> io::Result<()> {
    let temporary = temporary_for(path);
    let mut file = File::create(&temporary)?;
    file.write_all(bytes)?;
    file.sync_all()?;
    fs::rename(&temporary, path)
}

/// Deletes the file at `path`; a file that does not exist counts as deleted.
pub fn remove_file_if_present(path: &Path) -> io::Result<()> {
    match fs::remove_file(path) {
        Err(e) if e.kind() != io::ErrorKind::NotFound => Err(e),
        _ => Ok(()),
    }
}

/// `name.ext.tmp`: the extension is kept, so the temporary file never takes the place of a sibling.
fn temporary_for(path: &Path) -> PathBuf {
    let mut name = path.file_name().map(OsString::from).unwrap_or_default();
    name.push(".tmp");
    path.with_file_name(name)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn writes_and_replaces_without_leaving_the_temporary_file() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("state.json");
        write_replacing(&path, b"old").unwrap();
        write_replacing(&path, b"new").unwrap();
        assert_eq!(fs::read(&path).unwrap(), b"new");
        assert_eq!(fs::read_dir(dir.path()).unwrap().count(), 1);
    }

    #[test]
    fn removing_a_missing_file_is_not_an_error() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("gone.txt");
        remove_file_if_present(&path).unwrap();
        fs::write(&path, b"x").unwrap();
        remove_file_if_present(&path).unwrap();
        assert!(!path.exists());
    }
}
