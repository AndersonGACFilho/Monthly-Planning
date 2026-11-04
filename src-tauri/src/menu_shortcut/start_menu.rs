//! Windows Start Menu shortcut (POR-18): `Planejamento Mensal.lnk` in the user's Programs folder, written and read
//! through the shell's own `IShellLinkW`, so Windows indexes and resolves it like any installed program.

use super::{Launch, SystemMenu};
use crate::files;
use std::ffi::OsString;
use std::fs;
use std::io;
use std::os::windows::ffi::OsStringExt;
use std::path::PathBuf;
use std::ptr;
use windows::core::{Interface, HSTRING};
use windows::Win32::System::Com::{
    CoCreateInstance, CoInitializeEx, CoTaskMemFree, CoUninitialize, IPersistFile, CLSCTX_INPROC_SERVER,
    COINIT_APARTMENTTHREADED, STGM_READ,
};
use windows::Win32::UI::Shell::{FOLDERID_Programs, IShellLinkW, SHGetKnownFolderPath, ShellLink, KF_FLAG_DEFAULT};

const LINK_FILE: &str = "Planejamento Mensal.lnk";
const DESCRIPTION: &str = "Finanças da casa";
/// Characters read back from the link; the shell keeps paths and arguments far shorter.
const TEXT_CAPACITY: usize = 1024;

/// The Start Menu Programs folder of one user.
pub struct StartMenu {
    folder: PathBuf,
}

impl StartMenu {
    pub fn new(folder: PathBuf) -> StartMenu {
        StartMenu { folder }
    }

    /// `%APPDATA%\Microsoft\Windows\Start Menu\Programs` as the system reports it.
    pub fn for_current_user() -> Option<StartMenu> {
        // SAFETY: the folder text is allocated by the shell for the caller, copied, then freed once
        unsafe {
            let text = SHGetKnownFolderPath(&FOLDERID_Programs, KF_FLAG_DEFAULT, None).ok()?;
            let folder = PathBuf::from(OsString::from_wide(text.as_wide()));
            CoTaskMemFree(Some(text.0 as *const _));
            Some(StartMenu::new(folder))
        }
    }

    fn link(&self) -> HSTRING {
        HSTRING::from(self.folder.join(LINK_FILE).as_path())
    }
}

impl SystemMenu for StartMenu {
    fn read(&self) -> io::Result<Option<Launch>> {
        if !self.folder.join(LINK_FILE).exists() {
            return Ok(None);
        }
        let _com = Com::enter();
        // SAFETY: every buffer handed to the shell is a live slice of the length it is told
        unsafe {
            let link: IShellLinkW = CoCreateInstance(&ShellLink, None, CLSCTX_INPROC_SERVER)?;
            link.cast::<IPersistFile>()?.Load(&self.link(), STGM_READ)?;
            let program = read_text(|buffer| link.GetPath(buffer, ptr::null_mut(), 0))?;
            let home = read_text(|buffer| link.GetWorkingDirectory(buffer))?;
            let arguments = read_text(|buffer| link.GetArguments(buffer))?;
            let script = (!arguments.is_empty()).then(|| PathBuf::from(arguments.trim_matches('"')));
            Ok(Some(Launch { home: home.into(), program: program.into(), script }))
        }
    }

    /// The icon is the one inside the executable; with the drive absent Windows shows a blank one.
    fn write(&self, launch: &Launch) -> io::Result<()> {
        fs::create_dir_all(&self.folder)?;
        let _com = Com::enter();
        // SAFETY: plain COM calls on an object created on this thread
        unsafe {
            let link: IShellLinkW = CoCreateInstance(&ShellLink, None, CLSCTX_INPROC_SERVER)?;
            link.SetPath(&HSTRING::from(launch.program.as_path()))?;
            if let Some(script) = &launch.script {
                link.SetArguments(&HSTRING::from(format!("\"{}\"", script.display())))?;
            }
            link.SetWorkingDirectory(&HSTRING::from(launch.home.as_path()))?;
            link.SetIconLocation(&HSTRING::from(launch.program.as_path()), 0)?;
            link.SetDescription(&HSTRING::from(DESCRIPTION))?;
            link.cast::<IPersistFile>()?.Save(&self.link(), true)?;
        }
        Ok(())
    }

    fn remove(&self) -> io::Result<()> {
        files::remove_file_if_present(&self.folder.join(LINK_FILE))
    }
}

/// Text the shell writes into a buffer, up to its first NUL.
fn read_text(read: impl FnOnce(&mut [u16]) -> windows::core::Result<()>) -> io::Result<String> {
    let mut buffer = vec![0u16; TEXT_CAPACITY];
    read(&mut buffer)?;
    let length = buffer.iter().position(|&c| c == 0).unwrap_or(buffer.len());
    Ok(OsString::from_wide(&buffer[..length]).to_string_lossy().into_owned())
}

/// COM for the calling thread while the guard lives; a thread already in COM keeps its own setup.
struct Com {
    entered: bool,
}

impl Com {
    fn enter() -> Com {
        // SAFETY: balanced by `CoUninitialize` in `drop` only when this call succeeded
        let entered = unsafe { CoInitializeEx(None, COINIT_APARTMENTTHREADED) }.is_ok();
        Com { entered }
    }
}

impl Drop for Com {
    fn drop(&mut self) {
        if self.entered {
            // SAFETY: matches the successful `CoInitializeEx` of `enter`
            unsafe { CoUninitialize() };
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn launch(home: &std::path::Path) -> Launch {
        Launch { home: home.to_path_buf(), program: home.join("PlanejamentoMensal.exe"), script: None }
    }

    /// App folder with long names: the system temp folder may be reported in 8.3 form (`RUNNER~1`), which the
    /// shell expands when it reads the link back.
    fn app_folder() -> tempfile::TempDir {
        tempfile::tempdir_in(env!("CARGO_MANIFEST_DIR")).unwrap()
    }

    #[test]
    fn por_18_link_points_at_the_executable_in_the_app_folder() {
        let user = tempfile::tempdir().unwrap();
        let app = app_folder();
        fs::write(app.path().join("PlanejamentoMensal.exe"), b"").unwrap();
        let menu = StartMenu::new(user.path().join("Programs"));
        assert_eq!(menu.read().unwrap(), None);
        menu.write(&launch(app.path())).unwrap();
        let read = menu.read().unwrap().unwrap();
        assert!(read.same_as(&launch(app.path())), "{read:?}");
        let names: Vec<_> = fs::read_dir(user.path().join("Programs")).unwrap().map(|e| e.unwrap().file_name()).collect();
        assert_eq!(names, [OsString::from(LINK_FILE)]);
    }

    #[test]
    fn por_18_removal_deletes_the_link() {
        let user = tempfile::tempdir().unwrap();
        let app = app_folder();
        fs::write(app.path().join("PlanejamentoMensal.exe"), b"").unwrap();
        let menu = StartMenu::new(user.path().to_path_buf());
        menu.write(&launch(app.path())).unwrap();
        menu.remove().unwrap();
        assert_eq!(fs::read_dir(user.path()).unwrap().count(), 0);
        menu.remove().unwrap();
    }
}
