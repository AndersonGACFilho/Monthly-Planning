//! Linux app launcher entry (POR-18): `planejamento-mensal.desktop` and its icon in the user's `applications`
//! folder, which every desktop (GNOME, KDE, XFCE…) searches.

use super::{desktop_entry, Launch, SystemMenu};
use crate::files;
use std::env;
use std::ffi::OsString;
use std::fs;
use std::io;
use std::path::PathBuf;

const ENTRY_FILE: &str = "planejamento-mensal.desktop";
/// Kept next to the entry, not in the app folder, so the launcher shows it even while the drive is absent.
const ICON_FILE: &str = "planejamento-mensal.png";
const ICON: &[u8] = include_bytes!("../../icons/128x128@2x.png");

/// The `applications` folder of one user.
pub struct Applications {
    folder: PathBuf,
}

impl Applications {
    pub fn new(folder: PathBuf) -> Applications {
        Applications { folder }
    }

    /// `$XDG_DATA_HOME/applications`, by default `~/.local/share/applications`; `None` without a home folder.
    pub fn for_current_user() -> Option<Applications> {
        let data = data_home(env::var_os("XDG_DATA_HOME"), env::var_os("HOME"))?;
        Some(Applications::new(data.join("applications")))
    }

    fn entry(&self) -> PathBuf {
        self.folder.join(ENTRY_FILE)
    }

    fn icon(&self) -> PathBuf {
        self.folder.join(ICON_FILE)
    }
}

/// Base folder of user data files: `XDG_DATA_HOME` when it is an absolute path, else `$HOME/.local/share`.
fn data_home(xdg_data_home: Option<OsString>, home: Option<OsString>) -> Option<PathBuf> {
    xdg_data_home
        .map(PathBuf::from)
        .filter(|path| path.is_absolute())
        .or_else(|| home.map(|home| PathBuf::from(home).join(".local").join("share")))
}

impl SystemMenu for Applications {
    fn read(&self) -> io::Result<Option<Launch>> {
        match fs::read_to_string(self.entry()) {
            Ok(content) => Ok(desktop_entry::parse(&content)),
            Err(e) if e.kind() == io::ErrorKind::NotFound => Ok(None),
            Err(e) => Err(e),
        }
    }

    /// The icon goes first, so the entry never shows without it.
    fn write(&self, launch: &Launch) -> io::Result<()> {
        let entry = desktop_entry::render(launch, &self.icon())?;
        fs::create_dir_all(&self.folder)?;
        files::write_replacing(&self.icon(), ICON)?;
        files::write_replacing(&self.entry(), entry.as_bytes())
    }

    /// The `applications` folder stays: it belongs to the system and holds other programs.
    fn remove(&self) -> io::Result<()> {
        files::remove_file_if_present(&self.entry())?;
        files::remove_file_if_present(&self.icon())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn launch(home: &str) -> Launch {
        Launch { home: home.into(), program: format!("{home}/PlanejamentoMensal.AppImage").into(), script: None }
    }

    fn names(folder: &std::path::Path) -> Vec<String> {
        let mut names: Vec<String> =
            fs::read_dir(folder).unwrap().map(|e| e.unwrap().file_name().to_string_lossy().into_owned()).collect();
        names.sort();
        names
    }

    #[test]
    fn por_18_writes_the_entry_and_its_icon_only() {
        let user = tempfile::tempdir().unwrap();
        let menu = Applications::new(user.path().join("applications"));
        assert_eq!(menu.read().unwrap(), None);
        menu.write(&launch("/media/ssd/PlanejamentoMensal")).unwrap();
        assert_eq!(names(&user.path().join("applications")), ["planejamento-mensal.desktop", "planejamento-mensal.png"]);
        assert_eq!(fs::read(menu.icon()).unwrap(), ICON);
        assert!(fs::read_to_string(menu.entry()).unwrap().contains(&format!("Icon={}", menu.icon().display())));
        assert_eq!(menu.read().unwrap(), Some(launch("/media/ssd/PlanejamentoMensal")));
    }

    #[test]
    fn por_18_writing_again_replaces_the_entry() {
        let user = tempfile::tempdir().unwrap();
        let menu = Applications::new(user.path().to_path_buf());
        menu.write(&launch("/media/ssd/PlanejamentoMensal")).unwrap();
        menu.write(&launch("/run/media/ssd/PlanejamentoMensal")).unwrap();
        assert_eq!(menu.read().unwrap(), Some(launch("/run/media/ssd/PlanejamentoMensal")));
        assert_eq!(names(user.path()).len(), 2);
    }

    #[test]
    fn por_18_removal_deletes_everything_written_and_keeps_other_programs() {
        let user = tempfile::tempdir().unwrap();
        fs::write(user.path().join("other.desktop"), "[Desktop Entry]\n").unwrap();
        let menu = Applications::new(user.path().to_path_buf());
        menu.write(&launch("/media/ssd/PlanejamentoMensal")).unwrap();
        menu.remove().unwrap();
        assert_eq!(names(user.path()), ["other.desktop"]);
        menu.remove().unwrap();
    }

    #[test]
    fn por_18_data_home_follows_xdg_then_home() {
        assert_eq!(data_home(Some("/x/data".into()), Some("/home/a".into())), Some(PathBuf::from("/x/data")));
        assert_eq!(data_home(Some("relative".into()), Some("/home/a".into())), Some(PathBuf::from("/home/a/.local/share")));
        assert_eq!(data_home(None, Some("/home/a".into())), Some(PathBuf::from("/home/a/.local/share")));
        assert_eq!(data_home(None, None), None);
    }
}
