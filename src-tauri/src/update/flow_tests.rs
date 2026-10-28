//! The whole update against a local server (POR-16): release list, checksum and zip over HTTP, then the move into
//! the app folder at the next start.

use super::*;
use sha2::{Digest, Sha256};
use std::collections::HashMap;
use std::io::{BufRead, BufReader, Cursor, Write};
use std::net::TcpListener;
use std::path::PathBuf;
use std::thread;
use zip::write::SimpleFileOptions;
use zip::ZipWriter;

const TAG: &str = "v0.2.0";

/// Answers each request with the body registered for its path, or 404.
fn serve(listener: TcpListener, routes: HashMap<String, Vec<u8>>) {
    thread::spawn(move || {
        for stream in listener.incoming() {
            let Ok(mut stream) = stream else { continue };
            let mut reader = BufReader::new(stream.try_clone().unwrap());
            let mut request_line = String::new();
            let _ = reader.read_line(&mut request_line);
            let mut header = String::new();
            while reader.read_line(&mut header).is_ok_and(|read| read > 2) {
                header.clear();
            }
            let path = request_line.split_whitespace().nth(1).unwrap_or("/");
            let _ = match routes.get(path) {
                Some(body) => write!(stream, "HTTP/1.1 200 OK\r\nContent-Length: {}\r\nConnection: close\r\n\r\n", body.len())
                    .and_then(|()| stream.write_all(body)),
                None => write!(stream, "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"),
            };
        }
    });
}

fn release_zip() -> Vec<u8> {
    let mut writer = ZipWriter::new(Cursor::new(Vec::new()));
    let options = SimpleFileOptions::default();
    let runtime = format!("PlanejamentoMensal/backend/{}/bin/java", crate::app_home::runtime_dir_name());
    for (name, content) in [
        ("PlanejamentoMensal/backend/app.jar", "new"),
        (runtime.as_str(), "java"),
        ("PlanejamentoMensal/LEIA-ME.md", "new"),
        ("PlanejamentoMensal/data/planejamento-mensal.db", "empty"),
    ] {
        writer.start_file(name, options).unwrap();
        writer.write_all(content.as_bytes()).unwrap();
    }
    writer.finish().unwrap().into_inner()
}

/// Starts a server with a release `v0.2.0` whose checksum file holds `published_sha256` (the zip's own hash
/// when `None`), and returns the URL of the release list.
fn release_server(published_sha256: Option<String>) -> String {
    let listener = TcpListener::bind("127.0.0.1:0").unwrap();
    let base = format!("http://{}", listener.local_addr().unwrap());
    let zip = release_zip();
    let sha256 = published_sha256.unwrap_or_else(|| Sha256::digest(&zip).iter().map(|b| format!("{b:02x}")).collect());
    let package = format!("PlanejamentoMensal-{TAG}.zip");
    let releases = format!(
        r#"[{{"tag_name":"{TAG}","body":"- faster import","draft":false,"prerelease":false,"assets":[
            {{"name":"{package}","browser_download_url":"{base}/{package}","size":{size}}},
            {{"name":"{package}.sha256","browser_download_url":"{base}/{package}.sha256","size":100}}]}}]"#,
        size = zip.len()
    );
    let routes = HashMap::from([
        ("/releases".to_string(), releases.into_bytes()),
        (format!("/{package}.sha256"), format!("{sha256}  {package}\n").into_bytes()),
        (format!("/{package}"), zip),
    ]);
    serve(listener, routes);
    format!("{base}/releases")
}

fn app_folder() -> (tempfile::TempDir, PathBuf) {
    let folder = tempfile::tempdir().unwrap();
    let home = folder.path().join("PlanejamentoMensal");
    fs::create_dir_all(home.join("backend")).unwrap();
    fs::create_dir_all(home.join("data")).unwrap();
    fs::write(home.join("backend/app.jar"), "old").unwrap();
    fs::write(home.join("LEIA-ME.md"), "old").unwrap();
    fs::write(home.join("data/planejamento-mensal.db"), "user data").unwrap();
    (folder, home)
}

fn read(home: &Path, relative: &str) -> String {
    fs::read_to_string(home.join(relative)).unwrap()
}

fn current() -> Version {
    Version::parse("0.1.0").unwrap()
}

#[test]
fn por_16_downloads_verifies_and_installs_a_release() {
    let (_folder, home) = app_folder();
    let updater = Updater::with_releases_url(&home, release_server(None));
    let found = updater.check(&current()).unwrap().unwrap();
    assert_eq!(found.version.to_string(), "0.2.0");
    assert_eq!(found.notes, "v0.2.0\n- faster import");

    let mut last = (0, 0);
    updater.download(&mut |done, total| last = (done, total)).unwrap();
    assert_eq!(last.0, last.1);
    assert_eq!(read(&home, "backend/app.jar"), "old", "nothing changes while the app runs");

    // next start
    let restarted = Updater::with_releases_url(&home, String::new());
    assert!(restarted.resume());
    assert_eq!(read(&home, "backend/app.jar"), "new");
    assert_eq!(read(&home, "LEIA-ME.md"), "new");
    restarted.confirm_started();
    assert!(!home.join("update").exists());
    assert_eq!(read(&home, "data/planejamento-mensal.db"), "user data");
}

#[test]
fn por_16_download_with_wrong_checksum_is_discarded() {
    let (_folder, home) = app_folder();
    let updater = Updater::with_releases_url(&home, release_server(Some("0".repeat(64))));
    updater.check(&current()).unwrap().unwrap();
    assert!(matches!(updater.download(&mut |_, _| {}), Err(UpdateError::Integrity(_))));
    assert!(!updater.layout.staged().exists());
    assert!(!updater.layout.download().exists());
    assert!(!Updater::with_releases_url(&home, String::new()).resume());
    assert_eq!(read(&home, "backend/app.jar"), "old");
}

#[test]
fn por_16_nothing_to_download_without_a_check() {
    let (_folder, home) = app_folder();
    let updater = Updater::with_releases_url(&home, release_server(None));
    assert!(matches!(updater.download(&mut |_, _| {}), Err(UpdateError::NothingToInstall)));
}

#[test]
fn por_16_offline_check_fails_quietly() {
    let (_folder, home) = app_folder();
    let port = TcpListener::bind("127.0.0.1:0").unwrap().local_addr().unwrap().port();
    let updater = Updater::with_releases_url(&home, format!("http://127.0.0.1:{port}/releases"));
    assert!(updater.check(&current()).is_err());
    assert!(!home.join("update").exists());
}
