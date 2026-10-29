//! Desktop shell: opens the window, runs the backend sidecar and closes both together (SPEC section 12), and
//! installs updates of the portable folder (POR-16) and, on request, adds the app to the system menu (POR-18).

mod app_home;
mod backend;
mod files;
mod hex;
mod http;
mod menu_shortcut;
mod messages;
mod update;
mod window_state;

use backend::{Backend, BackendConfig, StartError, EXIT_LOCK_HELD, EXIT_LOCK_ORPHANED};
use menu_shortcut::MenuShortcut;
use std::path::PathBuf;
use std::sync::{Condvar, Mutex};
use std::thread;
use tauri::{AppHandle, Manager, RunEvent, WindowEvent};
use tauri_plugin_dialog::{DialogExt, MessageDialogButtons, MessageDialogKind};
use update::{Installer, Relaunch, Updater};

/// Startup progress of the backend, read by the `backend_config` command.
enum Status {
    Starting,
    Ready(BackendConfig),
    Failed(String),
}

/// Shared state: app folder, backend process and its readiness.
struct Shell {
    home: PathBuf,
    status: Mutex<Status>,
    ready: Condvar,
    backend: Mutex<Option<Backend>>,
}

impl Shell {
    fn set_status(&self, status: Status) {
        *self.status.lock().unwrap() = status;
        self.ready.notify_all();
    }
}

/// Returns the backend address and session token once `/actuator/health` answered (POR-08).
#[tauri::command]
async fn backend_config(app: AppHandle) -> Result<BackendConfig, String> {
    tauri::async_runtime::spawn_blocking(move || {
        let shell = app.state::<Shell>();
        let mut status = shell.status.lock().unwrap();
        loop {
            match &*status {
                Status::Starting => status = shell.ready.wait(status).unwrap(),
                Status::Ready(config) => return Ok(config.clone()),
                Status::Failed(reason) => return Err(reason.clone()),
            }
        }
    })
    .await
    .map_err(|e| e.to_string())?
}

/// Starts the backend, handling the single-instance lock (POR-10) with native dialogs.
fn start_backend(app: AppHandle) {
    let shell = app.state::<Shell>();
    let text = messages::current();
    let mut force_lock = false;
    loop {
        let mut backend = match Backend::start(&shell.home, force_lock) {
            Ok(backend) => backend,
            Err(e) => return fail(&app, &format!("{e}")),
        };
        match backend.wait_until_healthy() {
            Ok(()) => {
                app.state::<Installer>().confirm_started();
                shell.set_status(Status::Ready(backend.config()));
                *shell.backend.lock().unwrap() = Some(backend);
                return;
            }
            Err(StartError::Exited(Some(EXIT_LOCK_HELD))) => {
                app.dialog()
                    .message(text.lock_held)
                    .title(text.title)
                    .kind(MessageDialogKind::Warning)
                    .blocking_show();
                shell.set_status(Status::Failed("locked".into()));
                app.exit(0);
                return;
            }
            Err(StartError::Exited(Some(EXIT_LOCK_ORPHANED))) if !force_lock => {
                let take_over = app
                    .dialog()
                    .message(text.lock_orphaned)
                    .title(text.title)
                    .kind(MessageDialogKind::Warning)
                    .buttons(MessageDialogButtons::OkCancel)
                    .blocking_show();
                if !take_over {
                    shell.set_status(Status::Failed("locked".into()));
                    app.exit(0);
                    return;
                }
                force_lock = true;
            }
            Err(e) => return fail(&app, &e.to_string()),
        }
    }
}

fn fail(app: &AppHandle, reason: &str) {
    let shell = app.state::<Shell>();
    let text = messages::current();
    shell.set_status(Status::Failed(reason.to_string()));
    if let Some(version) = app.state::<Installer>().undo_failed_start() {
        app.dialog()
            .message(text.update_undone.replace("{version}", &version))
            .title(text.title)
            .kind(MessageDialogKind::Warning)
            .blocking_show();
        return restart(app);
    }
    app.dialog()
        .message(format!("{}\n{}", text.start_failed, backend::log_file(&shell.home).display()))
        .title(text.title)
        .kind(MessageDialogKind::Error)
        .blocking_show();
    app.exit(1);
}

fn stop_backend(app: &AppHandle) {
    if let Some(backend) = app.state::<Shell>().backend.lock().unwrap().take() {
        backend.stop();
    }
}

/// Stops the backend and restarts the app, which moves a downloaded update in before opening (POR-16). Off the
/// main thread, so the window stays responsive while the backend closes.
#[tauri::command]
async fn update_restart(app: AppHandle) -> Result<(), String> {
    tauri::async_runtime::spawn_blocking(move || {
        stop_backend(&app);
        let code = app.state::<Relaunch>().start().map_err(|e| e.to_string())?;
        app.exit(code);
        Ok(())
    })
    .await
    .map_err(|e| e.to_string())?
}

/// Starts the app again and ends this process.
fn restart(app: &AppHandle) {
    let code = app.state::<Relaunch>().start().unwrap_or(1);
    app.exit(code);
}

/// Shows why a staged update could not be moved in at start, without blocking the startup.
fn report_update_failure(app: &AppHandle) {
    if let Some(reason) = app.state::<Installer>().take_failure() {
        let text = messages::current();
        app.dialog()
            .message(format!("{}\n{reason}", text.update_failed))
            .title(text.title)
            .kind(MessageDialogKind::Warning)
            .show(|_| {});
    }
}

/// Entry point of the desktop app.
pub fn run() {
    let home = app_home::resolve();
    // captured before an update renames the running file
    let relaunch = Relaunch::current();
    let shortcut = MenuShortcut::current(&home);
    let installer = Installer::new(&home);
    if installer.resume() {
        // the folder now holds another version: run that one instead of this process
        if let Ok(code) = relaunch.start() {
            std::process::exit(code);
        }
    }
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_clipboard_manager::init())
        .plugin(tauri_plugin_notification::init())
        .manage(Shell {
            home: home.clone(),
            status: Mutex::new(Status::Starting),
            ready: Condvar::new(),
            backend: Mutex::new(None),
        })
        .manage(installer)
        .manage(Updater::new(&home))
        .manage(relaunch)
        .manage(shortcut)
        .invoke_handler(tauri::generate_handler![
            backend_config,
            update::commands::update_check,
            update::commands::update_download,
            update_restart,
            menu_shortcut::commands::menu_shortcut_status,
            menu_shortcut::commands::menu_shortcut_add,
            menu_shortcut::commands::menu_shortcut_remove
        ])
        .setup(move |app| {
            if let Some(window) = app.get_webview_window("main") {
                if let Some(state) = window_state::load(&app_home::data_dir(&home)) {
                    window_state::apply(&window, &state);
                }
                window.show()?;
            }
            report_update_failure(app.handle());
            let handle = app.handle().clone();
            thread::spawn(move || start_backend(handle));
            Ok(())
        })
        .on_window_event(|window, event| {
            // Saved on close and after every move or resize, so a crash or a forced stop keeps the last layout
            if matches!(event, WindowEvent::CloseRequested { .. } | WindowEvent::Resized(_) | WindowEvent::Moved(_)) {
                let shell = window.state::<Shell>();
                if let Some(webview) = window.app_handle().get_webview_window(window.label()) {
                    if let Some(state) = window_state::capture(&webview) {
                        window_state::save(&app_home::data_dir(&shell.home), &state);
                    }
                }
            }
        })
        .build(tauri::generate_context!())
        .expect("error while building the Tauri application")
        .run(|app, event| {
            if let RunEvent::Exit = event {
                stop_backend(app);
            }
        });
}
