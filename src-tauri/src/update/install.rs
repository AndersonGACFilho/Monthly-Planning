//! Installing a staged release at start and keeping or undoing it after the backend starts (POR-16).

use super::journal::{Journal, Swap, UpdateState};
use super::layout::Layout;
use super::swap;
use std::io;
use std::path::Path;
use std::sync::Mutex;

/// Moves a staged release into the app folder and decides, from the journal, what happens to it.
pub struct Installer {
    layout: Layout,
    journal: Journal,
    failure: Mutex<Option<String>>,
}

impl Installer {
    pub fn new(home: &Path) -> Installer {
        let layout = Layout::new(home);
        Installer { journal: Journal::new(&layout.journal()), layout, failure: Mutex::new(None) }
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
                // an unreadable journal leaves update/ as it is, for whoever looks into it
                self.note_failure(&e);
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
    pub fn take_failure(&self) -> Option<String> {
        self.failure.lock().unwrap().take()
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
                self.note_failure(&e);
                self.undo(&swap);
                false
            }
        }
    }

    /// Gives up a staged update that could not start moving; the app folder is unchanged.
    fn abandon(&self, error: &io::Error) -> bool {
        self.note_failure(error);
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
                self.note_failure(&e);
                false
            }
        }
    }

    fn note_failure(&self, error: &io::Error) {
        *self.failure.lock().unwrap() = Some(error.to_string());
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::update::test_support::{read, write};
    use std::fs;
    use std::path::PathBuf;

    const USER_DATA: &str = "data/planejamento-mensal.db";

    struct Fixture {
        _folder: tempfile::TempDir,
        home: PathBuf,
        installer: Installer,
    }

    impl Fixture {
        /// App folder with an "old" jar and user data, and a staged release with a "new" jar.
        fn staged() -> Fixture {
            let folder = tempfile::tempdir().unwrap();
            let home = folder.path().join("PlanejamentoMensal");
            let installer = Installer::new(&home);
            write(&home.join("backend/app.jar"), "old");
            write(&installer.layout.staged().join("backend/app.jar"), "new");
            write(&home.join(USER_DATA), "user data");
            installer.journal.write(&UpdateState::Staged { version: "0.2.0".into() }).unwrap();
            Fixture { _folder: folder, home, installer }
        }

        fn jar(&self) -> String {
            read(&self.home.join("backend/app.jar"))
        }

        fn assert_untouched_data_and_no_update_folder(&self) {
            assert_eq!(read(&self.home.join(USER_DATA)), "user data");
            assert!(!self.home.join("update").exists());
        }
    }

    #[test]
    fn por_16_staged_release_is_moved_in_and_kept_after_a_good_start() {
        let fixture = Fixture::staged();
        assert!(fixture.installer.resume());
        assert_eq!(fixture.jar(), "new");
        assert!(!fixture.installer.resume());
        fixture.installer.confirm_started();
        fixture.assert_untouched_data_and_no_update_folder();
    }

    #[test]
    fn por_16_failed_start_puts_previous_version_back() {
        let fixture = Fixture::staged();
        fixture.installer.resume();
        assert_eq!(fixture.installer.undo_failed_start().as_deref(), Some("0.2.0"));
        assert_eq!(fixture.jar(), "old");
        assert_eq!(fixture.installer.undo_failed_start(), None);
        fixture.assert_untouched_data_and_no_update_folder();
    }

    #[test]
    fn por_16_interrupted_move_is_undone_at_next_start() {
        let fixture = Fixture::staged();
        let layout = &fixture.installer.layout;
        let swap = swap::prepare(layout, "0.2.0").unwrap();
        fixture.installer.journal.write(&UpdateState::Applying(swap)).unwrap();
        swap::move_path(&fixture.home.join("backend"), &layout.previous().join("backend")).unwrap();
        assert!(fixture.installer.resume());
        assert_eq!(fixture.jar(), "old");
        fixture.assert_untouched_data_and_no_update_folder();
    }

    #[test]
    fn por_16_unreadable_journal_leaves_folder_untouched() {
        let fixture = Fixture::staged();
        fs::write(fixture.installer.layout.journal(), "{broken").unwrap();
        assert!(!fixture.installer.resume());
        assert!(fixture.installer.take_failure().is_some());
        assert!(fixture.installer.layout.staged().exists());
        assert_eq!(fixture.jar(), "old");
    }

    #[test]
    fn por_16_leftovers_without_journal_are_removed() {
        let fixture = Fixture::staged();
        fixture.installer.journal.clear().unwrap();
        assert!(!fixture.installer.resume());
        assert_eq!(fixture.jar(), "old");
        fixture.assert_untouched_data_and_no_update_folder();
    }
}
