//! Starting the app again after an update or a rollback (POR-16).

use std::env;
use std::ffi::OsString;
use std::io;
use std::path::PathBuf;
use std::process::Command;

/// Exit code that asks `start-linux.sh` to copy the app again and restart it (POR-09).
pub const EXIT_RESTART: i32 = 75;

/// How this process starts the app again.
#[derive(Clone, Debug, PartialEq, Eq)]
pub enum Relaunch {
    /// Run this file (the executable or the AppImage in the app folder) and exit.
    Spawn(PathBuf),
    /// Exit with this code; the launcher script starts the app again from a fresh copy.
    ExitCode(i32),
}

impl Relaunch {
    /// Decided at start, before an update renames the running file: afterwards the system reports the new
    /// location of the old file instead of the app folder.
    pub fn current() -> Relaunch {
        decide(env::var_os("PM_LAUNCHER"), env::var_os("APPIMAGE"), env::current_exe().ok())
    }

    /// Starts the app again and returns the exit code this process must end with.
    pub fn start(&self) -> io::Result<i32> {
        match self {
            Relaunch::Spawn(program) => {
                Command::new(program).args(env::args_os().skip(1)).spawn()?;
                Ok(0)
            }
            Relaunch::ExitCode(code) => Ok(*code),
        }
    }
}

fn decide(launcher: Option<OsString>, appimage: Option<OsString>, exe: Option<PathBuf>) -> Relaunch {
    if launcher.is_some() {
        return Relaunch::ExitCode(EXIT_RESTART);
    }
    // the executable of an AppImage lives on a temporary mount that ends with this process
    appimage
        .map(PathBuf::from)
        .or(exe)
        .map(Relaunch::Spawn)
        .unwrap_or(Relaunch::ExitCode(EXIT_RESTART))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn por_16_launcher_script_restarts_the_app() {
        let relaunch = decide(Some("start-linux".into()), None, Some(PathBuf::from("/tmp/copy/PlanejamentoMensal")));
        assert_eq!(relaunch, Relaunch::ExitCode(EXIT_RESTART));
        assert_eq!(relaunch.start().unwrap(), EXIT_RESTART);
    }

    #[test]
    fn por_16_appimage_restarts_from_its_file_not_its_mount() {
        let relaunch = decide(
            None,
            Some("/media/ssd/PlanejamentoMensal/PlanejamentoMensal.AppImage".into()),
            Some(PathBuf::from("/tmp/.mount_abc/usr/bin/PlanejamentoMensal")),
        );
        assert_eq!(relaunch, Relaunch::Spawn(PathBuf::from("/media/ssd/PlanejamentoMensal/PlanejamentoMensal.AppImage")));
    }

    #[test]
    fn por_16_executable_restarts_itself() {
        let exe = PathBuf::from("D:/PlanejamentoMensal/PlanejamentoMensal.exe");
        assert_eq!(decide(None, None, Some(exe.clone())), Relaunch::Spawn(exe));
    }
}
