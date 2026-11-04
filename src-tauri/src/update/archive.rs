//! Extracts the portable folder from a release zip (POR-16), keeping it exFAT-safe (POR-02).

use super::error::UpdateError;
use crate::app_home;
use std::fs::{self, File};
use std::io;
use std::path::{Component, Path, PathBuf};
use zip::ZipArchive;

/// Top-level folders of the app that a release never brings or replaces: the user's data and the update itself.
pub const RESERVED: [&str; 2] = ["data", "update"];

/// Extracts the zip at `zip_path` into `target`: the contents of its top folder, without the reserved folders.
///
/// Refuses entries that would land outside `target`, symbolic links (exFAT has none) and a zip that is not a
/// portable folder for this system (no backend jar or no JRE).
pub fn extract(zip_path: &Path, target: &Path) -> Result<(), UpdateError> {
    let mut archive = ZipArchive::new(File::open(zip_path)?).map_err(archive_error)?;
    for index in 0..archive.len() {
        let mut entry = archive.by_index(index).map_err(archive_error)?;
        if entry.is_symlink() {
            return Err(UpdateError::Archive(format!("symbolic link: {}", entry.name())));
        }
        let path = entry
            .enclosed_name()
            .ok_or_else(|| UpdateError::Archive(format!("path outside the folder: {}", entry.name())))?;
        let Some(relative) = below_top_folder(&path) else {
            continue;
        };
        if is_reserved(&relative) {
            continue;
        }
        let destination = target.join(&relative);
        if entry.is_dir() {
            fs::create_dir_all(&destination)?;
            continue;
        }
        if let Some(parent) = destination.parent() {
            fs::create_dir_all(parent)?;
        }
        let mut file = File::create(&destination)?;
        io::copy(&mut entry, &mut file)?;
        // flushed now: once the journal says "staged", a power loss must not leave half-written programs
        file.sync_all()?;
        restore_execute_bit(&destination, entry.unix_mode());
    }
    require_portable_folder(target)
}

/// The path without its first folder (`PlanejamentoMensal/`); `None` for that folder itself.
fn below_top_folder(path: &Path) -> Option<PathBuf> {
    let mut components = path.components();
    components.next()?;
    let rest = components.as_path();
    (!rest.as_os_str().is_empty()).then(|| rest.to_path_buf())
}

/// Whether `relative` is a reserved folder or inside one. exFAT and Windows ignore case, so `Data/` counts too.
pub fn is_reserved(relative: &Path) -> bool {
    match relative.components().next() {
        Some(Component::Normal(first)) => {
            RESERVED.iter().any(|reserved| first.to_str().is_some_and(|name| name.eq_ignore_ascii_case(reserved)))
        }
        _ => false,
    }
}

fn require_portable_folder(folder: &Path) -> Result<(), UpdateError> {
    let backend = folder.join("backend");
    for required in [backend.join("app.jar"), backend.join(app_home::runtime_dir_name())] {
        if !required.exists() {
            return Err(UpdateError::Archive(format!("not a portable folder, missing {}", required.display())));
        }
    }
    Ok(())
}

#[cfg(unix)]
fn restore_execute_bit(path: &Path, mode: Option<u32>) {
    use std::os::unix::fs::PermissionsExt;
    if let Some(mode) = mode.filter(|mode| mode & 0o111 != 0) {
        // exFAT has no permissions to set: start-linux.sh copies the programs before running them (POR-09)
        let _ = fs::set_permissions(path, fs::Permissions::from_mode(mode & 0o777));
    }
}

#[cfg(not(unix))]
fn restore_execute_bit(_path: &Path, _mode: Option<u32>) {}

fn archive_error(e: zip::result::ZipError) -> UpdateError {
    UpdateError::Archive(e.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::update::test_support::{read, ReleaseZip, REGULAR};

    /// Extracts `zip` into a fresh `staged/` folder and returns the result with that folder.
    fn extract_zip(zip: ReleaseZip) -> (tempfile::TempDir, Result<(), UpdateError>) {
        let folder = tempfile::tempdir().unwrap();
        let path = folder.path().join("release.zip");
        zip.save(&path);
        let result = extract(&path, &folder.path().join("staged"));
        (folder, result)
    }

    #[test]
    fn por_16_extracts_release_without_data() {
        let (folder, result) = extract_zip(ReleaseZip::portable("jar"));
        result.unwrap();
        let staged = folder.path().join("staged");
        assert_eq!(read(&staged.join("backend/app.jar")), "jar");
        assert_eq!(read(&staged.join("PlanejamentoMensal")), "shell");
        assert!(!staged.join("data").exists());
    }

    #[cfg(unix)]
    #[test]
    fn por_16_keeps_execute_bit() {
        use std::os::unix::fs::PermissionsExt;
        let (folder, result) = extract_zip(ReleaseZip::portable("jar"));
        result.unwrap();
        let mode = fs::metadata(folder.path().join("staged/PlanejamentoMensal")).unwrap().permissions().mode();
        assert_eq!(mode & 0o111, 0o111);
    }

    #[test]
    fn por_16_refuses_paths_outside_the_folder() {
        let (folder, result) = extract_zip(ReleaseZip::portable("jar").file("../../evil", "x", REGULAR));
        assert!(matches!(result, Err(UpdateError::Archive(_))));
        assert!(!folder.path().join("evil").exists());
    }

    #[test]
    fn por_02_refuses_symbolic_links() {
        let (_folder, result) = extract_zip(ReleaseZip::portable("jar").symlink("link", "backend/app.jar"));
        assert!(matches!(result, Err(UpdateError::Archive(_))));
    }

    #[test]
    fn por_16_refuses_zip_that_is_not_a_portable_folder() {
        let (_folder, result) = extract_zip(ReleaseZip::empty().file("readme.txt", "hi", REGULAR));
        assert!(matches!(result, Err(UpdateError::Archive(_))));
    }

    #[test]
    fn por_16_reserved_folders_ignore_case() {
        assert!(is_reserved(Path::new("data/planejamento-mensal.db")));
        assert!(is_reserved(Path::new("Data")));
        assert!(is_reserved(Path::new("update/state.json")));
        assert!(!is_reserved(Path::new("backend/data")));
        assert!(!is_reserved(Path::new("database.txt")));
    }
}
