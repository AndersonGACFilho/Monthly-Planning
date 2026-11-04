//! Moves the staged release into the app folder and back (POR-16).
//!
//! Only renames inside the app folder, so every entry changes at once, and the reserved folders (`data/`,
//! `update/`) are never touched. A running program can be renamed on Windows and Linux, which is how the shell
//! replaces itself.

use super::archive;
use super::journal::{Entry, Swap};
use super::layout::Layout;
use std::fs;
use std::io;
use std::path::Path;
use std::thread;
use std::time::Duration;

/// Antivirus scanners on Windows hold new files for a moment; a rename is retried for about three seconds.
const RENAME_ATTEMPTS: u32 = 10;
const RENAME_PAUSE: Duration = Duration::from_millis(300);

/// Lists the entries of the staged release for `version`, each noted with whether the app folder has it now.
///
/// Leftovers of an earlier update in `previous/` and `trash/` are removed first: `previous/` must hold only what
/// this swap moves there, or a rollback would bring back stale files.
pub fn prepare(layout: &Layout, version: &str) -> io::Result<Swap> {
    remove_dir_if_present(&layout.previous())?;
    remove_dir_if_present(&layout.trash())?;
    let mut entries = Vec::new();
    for item in fs::read_dir(layout.staged())? {
        let name = item?
            .file_name()
            .into_string()
            .map_err(|name| io::Error::new(io::ErrorKind::InvalidData, format!("file name {name:?} is not UTF-8")))?;
        if archive::is_reserved(Path::new(&name)) {
            continue;
        }
        let existed = exists(&layout.home().join(&name));
        entries.push(Entry { name, existed });
    }
    entries.sort_by(|a, b| a.name.cmp(&b.name));
    Ok(Swap { version: version.to_string(), entries })
}

/// Moves each entry of the app folder to `previous/` and the staged one into its place.
pub fn apply(layout: &Layout, swap: &Swap) -> io::Result<()> {
    for entry in &swap.entries {
        let current = layout.home().join(&entry.name);
        let previous = layout.previous().join(&entry.name);
        if entry.existed && exists(&current) && !exists(&previous) {
            move_path(&current, &previous)?;
        }
        let staged = layout.staged().join(&entry.name);
        if exists(&staged) {
            move_path(&staged, &current)?;
        }
    }
    Ok(())
}

/// Puts back what `apply` replaced and takes out what it added, whether `apply` finished or stopped midway.
/// Running it again changes nothing.
pub fn roll_back(layout: &Layout, swap: &Swap) -> io::Result<()> {
    for entry in swap.entries.iter().rev() {
        let current = layout.home().join(&entry.name);
        let previous = layout.previous().join(&entry.name);
        if entry.existed && !exists(&previous) {
            continue;
        }
        if exists(&current) {
            move_to_trash(layout, &current)?;
        }
        if entry.existed {
            move_path(&previous, &current)?;
        }
    }
    Ok(())
}

/// Removes `update/` once nothing needs it. Best effort: a program that is still closing may hold a file, and
/// the next start tries again.
pub fn clean(layout: &Layout) -> io::Result<()> {
    remove_dir_if_present(layout.root())
}

fn move_to_trash(layout: &Layout, path: &Path) -> io::Result<()> {
    let name = path.file_name().unwrap_or_default();
    let mut index = 0u32;
    let target = loop {
        let candidate = layout.trash().join(index.to_string()).join(name);
        if !exists(&candidate) {
            break candidate;
        }
        index += 1;
    };
    move_path(path, &target)
}

/// Renames `from` to `to` (both inside the app folder), creating the parent of `to` and retrying briefly.
pub fn move_path(from: &Path, to: &Path) -> io::Result<()> {
    if let Some(parent) = to.parent() {
        fs::create_dir_all(parent)?;
    }
    let mut attempt = 1;
    loop {
        match fs::rename(from, to) {
            Ok(()) => return Ok(()),
            Err(e) if attempt >= RENAME_ATTEMPTS || e.kind() == io::ErrorKind::NotFound => {
                return Err(io::Error::new(e.kind(), format!("{} -> {}: {e}", from.display(), to.display())));
            }
            Err(_) => {
                attempt += 1;
                thread::sleep(RENAME_PAUSE);
            }
        }
    }
}

pub fn remove_dir_if_present(path: &Path) -> io::Result<()> {
    match fs::remove_dir_all(path) {
        Err(e) if e.kind() != io::ErrorKind::NotFound => Err(e),
        _ => Ok(()),
    }
}

fn exists(path: &Path) -> bool {
    path.symlink_metadata().is_ok()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::update::test_support::{read, write};
    use std::path::PathBuf;

    struct Fixture {
        _folder: tempfile::TempDir,
        layout: Layout,
    }

    impl Fixture {
        /// App folder with version "old" files and user data, and a staged release with "new" files plus one
        /// file the old version did not have.
        fn new() -> Fixture {
            let folder = tempfile::tempdir().unwrap();
            let home = folder.path().join("PlanejamentoMensal");
            let layout = Layout::new(&home);
            for (base, version) in [(home.clone(), "old"), (layout.staged(), "new")] {
                write(&base.join("PlanejamentoMensal"), version);
                write(&base.join("backend/app.jar"), version);
                write(&base.join("LEIA-ME.md"), version);
            }
            write(&layout.staged().join("NOVIDADES.md"), "new");
            write(&layout.staged().join("data/planejamento-mensal.db"), "empty");
            write(&home.join("data/planejamento-mensal.db"), "user data");
            Fixture { _folder: folder, layout }
        }

        fn home(&self, relative: &str) -> PathBuf {
            self.layout.home().join(relative)
        }

        fn read(&self, relative: &str) -> String {
            read(&self.home(relative))
        }
    }

    #[test]
    fn por_16_lists_staged_entries_except_reserved_folders() {
        let fixture = Fixture::new();
        let swap = prepare(&fixture.layout, "0.2.0").unwrap();
        let names: Vec<(&str, bool)> = swap.entries.iter().map(|e| (e.name.as_str(), e.existed)).collect();
        assert_eq!(
            names,
            [("LEIA-ME.md", true), ("NOVIDADES.md", false), ("PlanejamentoMensal", true), ("backend", true)]
        );
    }

    #[test]
    fn por_16_apply_moves_new_files_in_and_keeps_old_ones_and_data() {
        let fixture = Fixture::new();
        let swap = prepare(&fixture.layout, "0.2.0").unwrap();
        apply(&fixture.layout, &swap).unwrap();
        assert_eq!(fixture.read("PlanejamentoMensal"), "new");
        assert_eq!(fixture.read("backend/app.jar"), "new");
        assert_eq!(fixture.read("NOVIDADES.md"), "new");
        assert_eq!(fixture.read("data/planejamento-mensal.db"), "user data");
        assert_eq!(read(&fixture.layout.previous().join("backend/app.jar")), "old");
    }

    #[test]
    fn por_16_roll_back_restores_old_files_and_removes_added_ones() {
        let fixture = Fixture::new();
        let swap = prepare(&fixture.layout, "0.2.0").unwrap();
        apply(&fixture.layout, &swap).unwrap();
        roll_back(&fixture.layout, &swap).unwrap();
        roll_back(&fixture.layout, &swap).unwrap();
        assert_eq!(fixture.read("PlanejamentoMensal"), "old");
        assert_eq!(fixture.read("backend/app.jar"), "old");
        assert!(!fixture.home("NOVIDADES.md").exists());
        assert_eq!(fixture.read("data/planejamento-mensal.db"), "user data");
    }

    #[test]
    fn por_16_roll_back_after_interrupted_apply() {
        let fixture = Fixture::new();
        let swap = prepare(&fixture.layout, "0.2.0").unwrap();
        // stopped after the first two entries and halfway through the third: the old shell is in previous/,
        // the new one not yet in place
        let done = Swap { version: swap.version.clone(), entries: swap.entries[..2].to_vec() };
        apply(&fixture.layout, &done).unwrap();
        move_path(&fixture.home("PlanejamentoMensal"), &fixture.layout.previous().join("PlanejamentoMensal")).unwrap();
        roll_back(&fixture.layout, &swap).unwrap();
        assert_eq!(fixture.read("PlanejamentoMensal"), "old");
        assert_eq!(fixture.read("LEIA-ME.md"), "old");
        assert_eq!(fixture.read("backend/app.jar"), "old");
        assert!(!fixture.home("NOVIDADES.md").exists());
        assert_eq!(fixture.read("data/planejamento-mensal.db"), "user data");
    }

    #[test]
    fn por_16_prepare_drops_leftovers_of_an_earlier_update() {
        let fixture = Fixture::new();
        write(&fixture.layout.previous().join("backend/app.jar"), "stale");
        let swap = prepare(&fixture.layout, "0.2.0").unwrap();
        apply(&fixture.layout, &swap).unwrap();
        roll_back(&fixture.layout, &swap).unwrap();
        assert_eq!(fixture.read("backend/app.jar"), "old");
    }

    #[test]
    fn por_16_clean_removes_update_folder_only() {
        let fixture = Fixture::new();
        clean(&fixture.layout).unwrap();
        clean(&fixture.layout).unwrap();
        assert!(!fixture.layout.root().exists());
        assert_eq!(fixture.read("data/planejamento-mensal.db"), "user data");
    }

    /// The shell renames its own running file during an update; both systems allow it.
    #[test]
    fn por_16_running_program_can_be_moved_aside() {
        let folder = tempfile::tempdir().unwrap();
        let (source, args): (PathBuf, &[&str]) = if cfg!(windows) {
            (PathBuf::from(std::env::var("SystemRoot").unwrap()).join("System32").join("PING.EXE"), &["-n", "30", "127.0.0.1"])
        } else {
            (PathBuf::from("/bin/sleep"), &["30"])
        };
        let program = folder.path().join(source.file_name().unwrap());
        fs::copy(&source, &program).unwrap();
        let mut running = std::process::Command::new(&program).args(args).spawn().unwrap();
        let moved = folder.path().join("previous").join(program.file_name().unwrap());
        let result = move_path(&program, &moved);
        let _ = running.kill();
        let _ = running.wait();
        result.unwrap();
        assert!(moved.exists() && !program.exists());
    }
}
