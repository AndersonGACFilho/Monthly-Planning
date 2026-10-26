//! Portable app folder (POR-01, POR-03): everything the app reads or writes lives below it.

use std::env;
use std::path::{Path, PathBuf};

/// Resolves the app folder.
///
/// Order: `PM_APP_HOME` (tests and development), the folder of the AppImage file (`APPIMAGE`, because
/// the AppImage runs from a temporary mount), the folder of the executable. Debug builds use
/// `dist/dev-home` in the repository so development data never lands in `target/`.
pub fn resolve() -> PathBuf {
    if let Some(home) = env::var_os("PM_APP_HOME") {
        return PathBuf::from(home);
    }
    if cfg!(debug_assertions) {
        return Path::new(env!("CARGO_MANIFEST_DIR")).join("../dist/dev-home");
    }
    if let Some(appimage) = env::var_os("APPIMAGE") {
        if let Some(parent) = Path::new(&appimage).parent() {
            return parent.to_path_buf();
        }
    }
    env::current_exe()
        .ok()
        .and_then(|exe| exe.parent().map(Path::to_path_buf))
        .unwrap_or_else(|| PathBuf::from("."))
}

/// Folder shared between operating systems.
pub fn data_dir(home: &Path) -> PathBuf {
    home.join("data")
}

/// Name of the JRE folder for the current operating system (POR-01).
pub fn runtime_dir_name() -> &'static str {
    if cfg!(windows) {
        "runtime-windows"
    } else {
        "runtime-linux"
    }
}

/// Java executable of the bundled JRE, falling back to `JAVA_HOME` in debug builds.
pub fn java_executable(home: &Path) -> PathBuf {
    let exe = if cfg!(windows) { "java.exe" } else { "java" };
    let bundled = home.join("backend").join(runtime_dir_name()).join("bin").join(exe);
    if bundled.exists() || !cfg!(debug_assertions) {
        return bundled;
    }
    env::var_os("JAVA_HOME")
        .map(|java_home| PathBuf::from(java_home).join("bin").join(exe))
        .unwrap_or_else(|| PathBuf::from(exe))
}

/// Backend jar, falling back to the Maven build output in debug builds.
pub fn backend_jar(home: &Path) -> PathBuf {
    let bundled = home.join("backend").join("app.jar");
    if bundled.exists() || !cfg!(debug_assertions) {
        return bundled;
    }
    Path::new(env!("CARGO_MANIFEST_DIR")).join("../backend/target/app.jar")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn por_01_runtime_folder_matches_os() {
        let expected = if cfg!(windows) { "runtime-windows" } else { "runtime-linux" };
        assert_eq!(runtime_dir_name(), expected);
    }

    #[test]
    fn por_03_data_dir_is_below_home() {
        let home = Path::new("/media/ssd/PlanejamentoMensal");
        assert_eq!(data_dir(home), home.join("data"));
    }
}
