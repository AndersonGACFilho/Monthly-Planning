//! The app's entry in the system menu: whether this computer has it, and adding, fixing or removing it (POR-18).

use super::{system_menu, Launch, SystemMenu};
use serde::Serialize;
use std::fmt;
use std::io;
use std::path::Path;

/// Whether this computer finds the app when the user searches for it.
#[derive(Clone, Debug, PartialEq, Eq, Serialize)]
#[serde(tag = "state", rename_all = "camelCase")]
pub enum ShortcutStatus {
    /// No entry of this app in the system menu.
    Absent,
    /// The entry starts this copy of the app, the way it was started now.
    Current,
    /// The entry starts the app from another folder (another drive letter or mount path) or in another way.
    Elsewhere { folder: String },
}

#[derive(Debug)]
pub enum ShortcutError {
    /// This system has no menu the app knows how to write to.
    Unsupported,
    /// Development builds run from the repository, which is not a folder to point the menu at.
    DevelopmentBuild,
    Io(io::Error),
}

impl fmt::Display for ShortcutError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ShortcutError::Unsupported => write!(f, "this system has no menu to add the app to"),
            ShortcutError::DevelopmentBuild => write!(f, "development builds do not add the app to the system menu"),
            ShortcutError::Io(e) => write!(f, "io: {e}"),
        }
    }
}

impl From<io::Error> for ShortcutError {
    fn from(e: io::Error) -> Self {
        ShortcutError::Io(e)
    }
}

/// Entry of this copy of the app in the system menu of the current user. Nothing is written unless asked.
pub struct MenuShortcut {
    launch: Launch,
    menu: Option<Box<dyn SystemMenu>>,
}

impl MenuShortcut {
    pub fn new(launch: Launch, menu: Option<Box<dyn SystemMenu>>) -> MenuShortcut {
        MenuShortcut { launch, menu }
    }

    /// For the app in `home`, started the way this process was.
    pub fn current(home: &Path) -> MenuShortcut {
        MenuShortcut::new(Launch::current(home), system_menu::for_current_user())
    }

    pub fn status(&self) -> Result<ShortcutStatus, ShortcutError> {
        Ok(match self.menu()?.read()? {
            None => ShortcutStatus::Absent,
            Some(installed) if installed.same_as(&self.launch) => ShortcutStatus::Current,
            Some(installed) => ShortcutStatus::Elsewhere { folder: installed.home.display().to_string() },
        })
    }

    /// Adds the entry, or points an existing one at this copy of the app.
    pub fn add(&self) -> Result<(), ShortcutError> {
        Ok(self.menu()?.write(&self.launch)?)
    }

    /// Removes the entry wherever it points, with every file it took.
    pub fn remove(&self) -> Result<(), ShortcutError> {
        Ok(self.menu()?.remove()?)
    }

    fn menu(&self) -> Result<&dyn SystemMenu, ShortcutError> {
        self.menu.as_deref().ok_or(ShortcutError::Unsupported)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;
    use std::sync::{Arc, Mutex};

    /// System menu kept in memory, shared with the test to look inside.
    #[derive(Clone, Default)]
    struct Memory(Arc<Mutex<Option<Launch>>>);

    impl SystemMenu for Memory {
        fn read(&self) -> io::Result<Option<Launch>> {
            Ok(self.0.lock().unwrap().clone())
        }
        fn write(&self, launch: &Launch) -> io::Result<()> {
            *self.0.lock().unwrap() = Some(launch.clone());
            Ok(())
        }
        fn remove(&self) -> io::Result<()> {
            *self.0.lock().unwrap() = None;
            Ok(())
        }
    }

    fn launch(home: &str) -> Launch {
        Launch { home: home.into(), program: PathBuf::from(home).join("PlanejamentoMensal"), script: None }
    }

    fn shortcut(home: &str, menu: &Memory) -> MenuShortcut {
        MenuShortcut::new(launch(home), Some(Box::new(menu.clone())))
    }

    #[test]
    fn por_18_nothing_is_written_until_asked() {
        let menu = Memory::default();
        let here = shortcut("E:/PlanejamentoMensal", &menu);
        assert_eq!(here.status().unwrap(), ShortcutStatus::Absent);
        assert_eq!(*menu.0.lock().unwrap(), None);
    }

    #[test]
    fn por_18_added_shortcut_is_current_then_removed() {
        let menu = Memory::default();
        let here = shortcut("E:/PlanejamentoMensal", &menu);
        here.add().unwrap();
        assert_eq!(here.status().unwrap(), ShortcutStatus::Current);
        here.remove().unwrap();
        assert_eq!(here.status().unwrap(), ShortcutStatus::Absent);
    }

    #[test]
    fn por_18_shortcut_from_another_drive_letter_is_offered_a_fix() {
        let menu = Memory::default();
        shortcut("E:/PlanejamentoMensal", &menu).add().unwrap();
        let other_session = shortcut("F:/PlanejamentoMensal", &menu);
        assert_eq!(other_session.status().unwrap(), ShortcutStatus::Elsewhere { folder: "E:/PlanejamentoMensal".into() });
        other_session.add().unwrap();
        assert_eq!(other_session.status().unwrap(), ShortcutStatus::Current);
    }

    #[test]
    fn por_18_systems_without_a_menu_say_so() {
        let nowhere = MenuShortcut::new(launch("/app"), None);
        assert!(matches!(nowhere.status(), Err(ShortcutError::Unsupported)));
        assert!(matches!(nowhere.add(), Err(ShortcutError::Unsupported)));
    }

    #[test]
    fn por_18_status_is_tagged_for_the_window() {
        let json = serde_json::to_string(&ShortcutStatus::Elsewhere { folder: "E:\\App".into() }).unwrap();
        assert_eq!(json, r#"{"state":"elsewhere","folder":"E:\\App"}"#);
        assert_eq!(serde_json::to_string(&ShortcutStatus::Current).unwrap(), r#"{"state":"current"}"#);
    }
}
