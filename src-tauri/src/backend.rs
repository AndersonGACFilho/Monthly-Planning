//! Backend sidecar (POR-08): started with the bundled JRE on a free port with a session token, polled
//! until healthy and stopped cleanly when the window closes.

use crate::{app_home, http};
use serde::Serialize;
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
            // POR-03: the JVM temp dir (sqlite native library, uploads) stays inside the app folder
            .arg(format!("-Djava.io.tmpdir={}", temp.display()))
            .arg("-Dfile.encoding=UTF-8")
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
    Ok(bytes.iter().map(|b| format!("{b:02x}")).collect())
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

#[cfg(test)]
mod tests {
    use super::*;

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
