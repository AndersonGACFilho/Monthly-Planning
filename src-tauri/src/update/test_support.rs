//! Helpers shared by the update tests: files of a temporary app folder and release zips.

use crate::app_home;
use std::fs;
use std::io::{Cursor, Write};
use std::path::Path;
use zip::write::SimpleFileOptions;
use zip::{CompressionMethod, ZipWriter};

pub const EXECUTABLE: u32 = 0o755;
pub const REGULAR: u32 = 0o644;

/// Writes `content` to `path`, creating its folders.
pub fn write(path: &Path, content: &str) {
    fs::create_dir_all(path.parent().unwrap()).unwrap();
    fs::write(path, content).unwrap();
}

pub fn read(path: &Path) -> String {
    fs::read_to_string(path).unwrap()
}

/// A release zip as CI builds it: everything below a `PlanejamentoMensal/` folder.
pub struct ReleaseZip {
    writer: ZipWriter<Cursor<Vec<u8>>>,
}

impl ReleaseZip {
    pub fn empty() -> ReleaseZip {
        ReleaseZip { writer: ZipWriter::new(Cursor::new(Vec::new())) }
    }

    /// The smallest portable folder for this system: shell, backend jar holding `jar`, JRE and a `data/` that
    /// installing must leave out.
    pub fn portable(jar: &str) -> ReleaseZip {
        let runtime = format!("backend/{}/bin/java", app_home::runtime_dir_name());
        ReleaseZip::empty()
            .file("PlanejamentoMensal", "shell", EXECUTABLE)
            .file("backend/app.jar", jar, REGULAR)
            .file(&runtime, "java", EXECUTABLE)
            .file("data/planejamento-mensal.db", "empty", REGULAR)
    }

    /// Adds `relative` (inside the top folder) with `content` and the unix `mode`.
    pub fn file(mut self, relative: &str, content: &str, mode: u32) -> ReleaseZip {
        let options = SimpleFileOptions::default().compression_method(CompressionMethod::Stored).unix_permissions(mode);
        self.writer.start_file(format!("PlanejamentoMensal/{relative}"), options).unwrap();
        self.writer.write_all(content.as_bytes()).unwrap();
        self
    }

    pub fn symlink(mut self, relative: &str, target: &str) -> ReleaseZip {
        self.writer.add_symlink(format!("PlanejamentoMensal/{relative}"), target, SimpleFileOptions::default()).unwrap();
        self
    }

    pub fn bytes(self) -> Vec<u8> {
        self.writer.finish().unwrap().into_inner()
    }

    pub fn save(self, path: &Path) {
        fs::write(path, self.bytes()).unwrap();
    }
}
