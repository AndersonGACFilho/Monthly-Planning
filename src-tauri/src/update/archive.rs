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
    use std::io::Write;
    use zip::write::SimpleFileOptions;
    use zip::ZipWriter;

    fn runtime() -> String {
        format!("PlanejamentoMensal/backend/{}/bin/java", app_home::runtime_dir_name())
    }

    fn write_zip(path: &Path, build: impl FnOnce(&mut ZipWriter<File>)) {
        let mut writer = ZipWriter::new(File::create(path).unwrap());
        build(&mut writer);
        writer.finish().unwrap();
    }

    fn add(writer: &mut ZipWriter<File>, name: &str, content: &[u8], mode: u32) {
        let options = SimpleFileOptions::default().compression_method(zip::CompressionMethod::Stored).unix_permissions(mode);
        writer.start_file(name, options).unwrap();
        writer.write_all(content).unwrap();
    }

    fn portable_release(writer: &mut ZipWriter<File>) {
        add(writer, "PlanejamentoMensal/PlanejamentoMensal", b"shell", 0o755);
        add(writer, "PlanejamentoMensal/backend/app.jar", b"jar", 0o644);
        add(writer, &runtime(), b"java", 0o755);
        add(writer, "PlanejamentoMensal/data/planejamento-mensal.db", b"empty db", 0o644);
    }

    #[test]
    fn por_16_extracts_release_without_data() {
        let folder = tempfile::tempdir().unwrap();
        let zip = folder.path().join("release.zip");
        write_zip(&zip, portable_release);
        let staged = folder.path().join("staged");
        extract(&zip, &staged).unwrap();
        assert_eq!(fs::read(staged.join("backend/app.jar")).unwrap(), b"jar");
        assert_eq!(fs::read(staged.join("PlanejamentoMensal")).unwrap(), b"shell");
        assert!(!staged.join("data").exists());
    }

    #[cfg(unix)]
    #[test]
    fn por_16_keeps_execute_bit() {
        use std::os::unix::fs::PermissionsExt;
        let folder = tempfile::tempdir().unwrap();
        let zip = folder.path().join("release.zip");
        write_zip(&zip, portable_release);
        let staged = folder.path().join("staged");
        extract(&zip, &staged).unwrap();
        let mode = fs::metadata(staged.join("PlanejamentoMensal")).unwrap().permissions().mode();
        assert_eq!(mode & 0o111, 0o111);
    }

    #[test]
    fn por_16_refuses_paths_outside_the_folder() {
        let folder = tempfile::tempdir().unwrap();
        let zip = folder.path().join("release.zip");
        write_zip(&zip, |writer| {
            portable_release(writer);
            add(writer, "PlanejamentoMensal/../../evil", b"x", 0o644);
        });
        assert!(matches!(extract(&zip, &folder.path().join("staged")), Err(UpdateError::Archive(_))));
        assert!(!folder.path().join("evil").exists());
    }

    #[test]
    fn por_02_refuses_symbolic_links() {
        let folder = tempfile::tempdir().unwrap();
        let zip = folder.path().join("release.zip");
        write_zip(&zip, |writer| {
            portable_release(writer);
            writer.add_symlink("PlanejamentoMensal/link", "backend/app.jar", SimpleFileOptions::default()).unwrap();
        });
        assert!(matches!(extract(&zip, &folder.path().join("staged")), Err(UpdateError::Archive(_))));
    }

    #[test]
    fn por_16_refuses_zip_that_is_not_a_portable_folder() {
        let folder = tempfile::tempdir().unwrap();
        let zip = folder.path().join("release.zip");
        write_zip(&zip, |writer| add(writer, "PlanejamentoMensal/readme.txt", b"hi", 0o644));
        assert!(matches!(extract(&zip, &folder.path().join("staged")), Err(UpdateError::Archive(_))));
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
