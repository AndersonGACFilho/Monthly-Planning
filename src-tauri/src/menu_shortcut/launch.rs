//! What the menu entry starts (POR-18).

use std::env;
use std::ffi::OsString;
use std::path::{Path, PathBuf};

/// Program run when the menu entry is chosen, the launcher script it runs (if any) and the app folder.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Launch {
    /// App folder, also the working directory of the entry.
    pub home: PathBuf,
    /// Executable, AppImage or `sh`.
    pub program: PathBuf,
    /// `start-linux.sh` in the app folder when the drive does not allow running programs (POR-09).
    pub script: Option<PathBuf>,
}

impl Launch {
    /// Decided at start, like `update::Relaunch::current`: after an update renames the running file, the system
    /// reports where the old file went instead of the app folder.
    pub fn current(home: &Path) -> Launch {
        decide(home, env::var_os("PM_LAUNCHER"), env::var_os("APPIMAGE"), env::current_exe().ok())
    }

    /// Whether `other` starts the same program from the same folder; Windows paths ignore case.
    pub fn same_as(&self, other: &Launch) -> bool {
        same_path(&self.home, &other.home)
            && same_path(&self.program, &other.program)
            && match (&self.script, &other.script) {
                (Some(mine), Some(theirs)) => same_path(mine, theirs),
                (None, None) => true,
                _ => false,
            }
    }
}

fn same_path(a: &Path, b: &Path) -> bool {
    if cfg!(windows) {
        a.to_string_lossy().to_lowercase() == b.to_string_lossy().to_lowercase()
    } else {
        a == b
    }
}

fn decide(home: &Path, launcher: Option<OsString>, appimage: Option<OsString>, exe: Option<PathBuf>) -> Launch {
    // started by start-linux.sh: the running file is a temporary copy, the script is what lasts
    if let Some(script) = launcher {
        return Launch { home: home.to_path_buf(), program: PathBuf::from("sh"), script: Some(home.join(script)) };
    }
    // the executable of an AppImage lives on a temporary mount that ends with this process
    let program = appimage.map(PathBuf::from).or(exe).unwrap_or_else(|| home.join(default_program()));
    Launch { home: home.to_path_buf(), program, script: None }
}

fn default_program() -> &'static str {
    if cfg!(windows) {
        "PlanejamentoMensal.exe"
    } else {
        "PlanejamentoMensal"
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const HOME: &str = "/media/ssd/PlanejamentoMensal";

    #[test]
    fn por_18_launcher_script_is_run_through_sh() {
        let launch = decide(Path::new(HOME), Some("start-linux.sh".into()), None, Some("/tmp/copy/PlanejamentoMensal".into()));
        assert_eq!(launch.program, PathBuf::from("sh"));
        assert_eq!(launch.script, Some(Path::new(HOME).join("start-linux.sh")));
    }

    #[test]
    fn por_18_appimage_is_started_from_its_file_not_its_mount() {
        let appimage = format!("{HOME}/PlanejamentoMensal.AppImage");
        let launch = decide(Path::new(HOME), None, Some(appimage.clone().into()), Some("/tmp/.mount_abc/usr/bin/x".into()));
        assert_eq!(launch, Launch { home: HOME.into(), program: appimage.into(), script: None });
    }

    #[test]
    fn por_18_executable_starts_itself() {
        let exe = PathBuf::from("D:/PlanejamentoMensal/PlanejamentoMensal.exe");
        let launch = decide(Path::new("D:/PlanejamentoMensal"), None, None, Some(exe.clone()));
        assert_eq!(launch.program, exe);
        assert_eq!(launch.home, PathBuf::from("D:/PlanejamentoMensal"));
    }

    #[test]
    fn por_18_another_folder_or_way_of_starting_is_not_the_same() {
        let here = decide(Path::new(HOME), None, None, Some(format!("{HOME}/PlanejamentoMensal").into()));
        let moved = decide(Path::new("/run/media/ssd/PlanejamentoMensal"), None, None, None);
        let script = decide(Path::new(HOME), Some("start-linux.sh".into()), None, None);
        assert!(here.same_as(&here.clone()));
        assert!(!here.same_as(&moved));
        assert!(!here.same_as(&script));
    }

    #[cfg(windows)]
    #[test]
    fn por_18_windows_paths_ignore_case() {
        let here = decide(Path::new("E:/Planejamento"), None, None, Some("E:/Planejamento/PlanejamentoMensal.exe".into()));
        let shouted = decide(Path::new("e:/PLANEJAMENTO"), None, None, Some("e:/planejamento/planejamentomensal.EXE".into()));
        assert!(here.same_as(&shouted));
    }
}
