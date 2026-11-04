//! Backend sidecar (POR-08): started with the bundled JRE on a free port with a session token, polled
//! until healthy and stopped cleanly when the window closes.

use crate::{app_home, hex, http};
use serde::Serialize;
use std::env;
use std::fs::{self, OpenOptions};
use std::io;
use std::net::{Ipv4Addr, TcpListener};
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::thread;
use std::time::{Duration, Instant};

/// Exit code of the backend when another running instance holds the database lock (POR-10).
pub const EXIT_LOCK_HELD: i32 = 10;
/// Exit code of the backend when the lock is orphaned and needs the user's confirmation (POR-10).
pub const EXIT_LOCK_ORPHANED: i32 = 11;

const STARTUP_TIMEOUT: Duration = Duration::from_secs(120);
const SHUTDOWN_TIMEOUT: Duration = Duration::from_secs(20);

/// What the window needs to call the API; returned by the `backend_config` command.
#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct BackendConfig {
    pub base_url: String,
    pub token: String,
}

/// Why the backend did not become healthy.
#[derive(Debug)]
pub enum StartError {
    /// The process ended during startup with this exit code.
    Exited(Option<i32>),
    /// Health did not answer within the startup timeout.
    Timeout,
    /// The process could not be launched.
    Io(io::Error),
}

impl std::fmt::Display for StartError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            StartError::Exited(Some(code)) => write!(f, "backend exited with code {code}"),
            StartError::Exited(None) => write!(f, "backend was terminated"),
            StartError::Timeout => write!(f, "backend did not become healthy in time"),
            StartError::Io(e) => write!(f, "backend could not run: {e}"),
        }
    }
}

/// A running backend process.
pub struct Backend {
    child: Child,
    port: u16,
    token: String,
}

impl Backend {
    /// Launches the backend for the app folder `home`.
    ///
    /// `force_lock` is set after the user confirmed taking over an orphaned lock (POR-10).
    pub fn start(home: &Path, force_lock: bool) -> io::Result<Backend> {
        let port = free_port()?;
        let token = new_token()?;
        let data = app_home::data_dir(home);
        let temp = data.join("tmp");
        let logs = data.join("logs");
        fs::create_dir_all(&temp)?;
        fs::create_dir_all(&logs)?;
        let log = OpenOptions::new().create(true).append(true).open(logs.join("backend.log"))?;

        let mut command = Command::new(app_home::java_executable(home));
        command
            .args(jvm_options(&temp, env::var_os("PM_NATIVE_DIR").map(PathBuf::from).as_deref()))
            .arg("-jar")
            .arg(app_home::backend_jar(home))
            .arg(format!("--app.home={}", home.display()))
            .arg(format!("--server.port={port}"))
            // the backend stops itself if this shell dies without stopping it
            .arg(format!("--app.parent-pid={}", std::process::id()))
            // relative paths resolve inside the app folder, not inside the AppImage mount or a system folder
            .current_dir(home)
            .env("APP_SESSION_TOKEN", &token)
            .stdin(Stdio::null())
            .stdout(Stdio::from(log.try_clone()?))
            .stderr(Stdio::from(log));
        if force_lock {
            command.arg("--app.lock.force=true");
        }
        hide_console(&mut command);
        let child = command.spawn()?;
        Ok(Backend { child, port, token })
    }

    /// Waits until `/actuator/health` reports UP, or until the process exits or times out.
    pub fn wait_until_healthy(&mut self) -> Result<(), StartError> {
        let deadline = Instant::now() + STARTUP_TIMEOUT;
        while Instant::now() < deadline {
            match self.child.try_wait() {
                Ok(Some(status)) => return Err(StartError::Exited(status.code())),
                Ok(None) => {}
                Err(e) => return Err(StartError::Io(e)),
            }
            if let Ok((200, body)) = http::request(self.port, "GET", "/actuator/health", None) {
                if body.contains("\"UP\"") {
                    return Ok(());
                }
            }
            thread::sleep(Duration::from_millis(250));
        }
        let _ = self.child.kill();
        Err(StartError::Timeout)
    }

    /// Address and token for the window.
    pub fn config(&self) -> BackendConfig {
        BackendConfig { base_url: format!("http://127.0.0.1:{}", self.port), token: self.token.clone() }
    }

    /// Asks the backend to shut down cleanly (lock release, WAL checkpoint) and kills it after a timeout.
    pub fn stop(mut self) {
        if matches!(self.child.try_wait(), Ok(Some(_))) {
            return;
        }
        let _ = http::request(self.port, "POST", "/actuator/shutdown", Some(&self.token));
        let deadline = Instant::now() + SHUTDOWN_TIMEOUT;
        while Instant::now() < deadline {
            if matches!(self.child.try_wait(), Ok(Some(_))) {
                return;
            }
            thread::sleep(Duration::from_millis(100));
        }
        let _ = self.child.kill();
        let _ = self.child.wait();
    }
}

/// Picks a free TCP port on 127.0.0.1.
pub fn free_port() -> io::Result<u16> {
    Ok(TcpListener::bind((Ipv4Addr::LOCALHOST, 0))?.local_addr()?.port())
}

/// Creates a random 256-bit session token, hex encoded (IMP-04).
pub fn new_token() -> io::Result<String> {
    let mut bytes = [0u8; 32];
    getrandom::fill(&mut bytes).map_err(|e| io::Error::other(e.to_string()))?;
    Ok(hex::encode(&bytes))
}

/// Path of the backend log, shown in error dialogs.
pub fn log_file(home: &Path) -> PathBuf {
    app_home::data_dir(home).join("logs").join("backend.log")
}

#[cfg(windows)]
fn hide_console(command: &mut Command) {
    use std::os::windows::process::CommandExt;
    const CREATE_NO_WINDOW: u32 = 0x0800_0000;
    command.creation_flags(CREATE_NO_WINDOW);
}

#[cfg(not(windows))]
fn hide_console(_command: &mut Command) {}

/// JVM options of the backend.
///
/// POR-03: the JVM temp dir (uploads, Tomcat) stays inside the app folder, and the JVM writes no performance
/// data file (`hsperfdata_<user>`) to the system temp folder. `native` is where native libraries are unpacked
/// when the app folder cannot run programs (exFAT mounted `noexec`, POR-09): `start-linux.sh` points it to its
/// temporary copy through `PM_NATIVE_DIR`.
fn jvm_options(temp: &Path, native: Option<&Path>) -> Vec<String> {
    let mut options = vec![
        format!("-Djava.io.tmpdir={}", temp.display()),
        "-Dfile.encoding=UTF-8".to_string(),
        "-XX:-UsePerfData".to_string(),
    ];
    if let Some(native) = native {
        options.push(format!("-Dorg.sqlite.tmpdir={}", native.display()));
    }
    options
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn por_03_jvm_writes_nothing_outside_and_unpacks_natives_where_programs_run() {
        let options = jvm_options(Path::new("/app/data/tmp"), None);
        assert!(options.contains(&"-Djava.io.tmpdir=/app/data/tmp".to_string()));
        assert!(options.contains(&"-XX:-UsePerfData".to_string()));
        assert!(!options.iter().any(|o| o.starts_with("-Dorg.sqlite.tmpdir")));
        let options = jvm_options(Path::new("/app/data/tmp"), Some(Path::new("/tmp/pm/native")));
        assert!(options.contains(&"-Dorg.sqlite.tmpdir=/tmp/pm/native".to_string()));
    }

    #[test]
    fn por_08_free_port_is_bindable() {
        let port = free_port().unwrap();
        assert!(port > 0);
        assert!(TcpListener::bind((Ipv4Addr::LOCALHOST, port)).is_ok());
    }

    #[test]
    fn imp_04_tokens_are_random_and_long() {
        let a = new_token().unwrap();
        let b = new_token().unwrap();
        assert_eq!(a.len(), 64);
        assert!(a.chars().all(|c| c.is_ascii_hexdigit()));
        assert_ne!(a, b);
    }
}
