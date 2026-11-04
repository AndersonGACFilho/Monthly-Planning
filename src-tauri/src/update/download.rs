//! Downloads the release zip and checks it against the size and SHA-256 published with it (POR-16).

use super::error::UpdateError;
use super::net;
use super::release::Asset;
use crate::hex;
use sha2::{Digest, Sha256};
use std::fs::{self, File};
use std::io::{self, BufWriter, Read, Write};
use std::path::{Path, PathBuf};

/// Largest checksum file read: one hash and one file name.
const CHECKSUM_LIMIT: u64 = 4096;
const COPY_BUFFER_BYTES: usize = 64 * 1024;
const SHA256_HEX_LENGTH: usize = 64;

/// Called with the bytes written so far and the expected total.
pub type Progress<'a> = &'a mut dyn FnMut(u64, u64);

/// Reads the SHA-256 published in `checksum` (the `sha256sum` format: hash, spaces, file name).
pub fn fetch_checksum(checksum: &Asset) -> Result<String, UpdateError> {
    let text = net::api_agent()
        .get(&checksum.url)
        .call()?
        .body_mut()
        .with_config()
        .limit(CHECKSUM_LIMIT)
        .read_to_string()?;
    parse_checksum(&text).ok_or_else(|| UpdateError::Release(format!("{} holds no SHA-256", checksum.name)))
}

/// Downloads `package` to `target` and keeps it only when its size and SHA-256 match.
///
/// The bytes go to `<target>.part` first, so an interrupted download never looks complete.
pub fn download(package: &Asset, sha256: &str, target: &Path, progress: Progress) -> Result<(), UpdateError> {
    let part = part_path(target);
    let reader = net::download_agent().get(&package.url).call()?.into_body().into_with_config().limit(package.size + 1).reader();
    let checked = save_hashing(reader, &part, package.size, progress)
        .map_err(UpdateError::from)
        .and_then(|(size, hash)| verify(package, size, &hash, sha256));
    if let Err(e) = checked {
        let _ = fs::remove_file(&part);
        return Err(e);
    }
    fs::rename(&part, target)?;
    Ok(())
}

fn part_path(target: &Path) -> PathBuf {
    let mut name = target.file_name().unwrap_or_default().to_os_string();
    name.push(".part");
    target.with_file_name(name)
}

/// Copies `reader` to the file `path`, returning the bytes written and their SHA-256 in lowercase hex.
fn save_hashing(mut reader: impl Read, path: &Path, total: u64, progress: Progress) -> io::Result<(u64, String)> {
    let mut file = BufWriter::new(File::create(path)?);
    let mut hasher = Sha256::new();
    let mut buffer = vec![0u8; COPY_BUFFER_BYTES];
    let mut written = 0u64;
    loop {
        let read = reader.read(&mut buffer)?;
        if read == 0 {
            break;
        }
        hasher.update(&buffer[..read]);
        file.write_all(&buffer[..read])?;
        written += read as u64;
        progress(written, total);
    }
    file.into_inner().map_err(|e| e.into_error())?.sync_all()?;
    Ok((written, hex::encode(&hasher.finalize())))
}

fn verify(package: &Asset, size: u64, hash: &str, expected: &str) -> Result<(), UpdateError> {
    if size != package.size {
        return Err(UpdateError::Integrity(format!("{} has {size} bytes, expected {}", package.name, package.size)));
    }
    if hash != expected {
        return Err(UpdateError::Integrity(format!("{} SHA-256 is {hash}, expected {expected}", package.name)));
    }
    Ok(())
}

fn parse_checksum(text: &str) -> Option<String> {
    let hash = text.split_whitespace().next()?;
    (hash.len() == SHA256_HEX_LENGTH && hash.chars().all(|c| c.is_ascii_hexdigit())).then(|| hash.to_ascii_lowercase())
}

#[cfg(test)]
mod tests {
    use super::*;

    const HELLO_SHA256: &str = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    fn package(size: u64) -> Asset {
        Asset { name: "PlanejamentoMensal-v0.2.0.zip".into(), url: String::new(), size }
    }

    #[test]
    fn por_16_reads_sha256sum_format() {
        let line = format!("{}  PlanejamentoMensal-v0.2.0.zip\n", HELLO_SHA256.to_uppercase());
        assert_eq!(parse_checksum(&line).as_deref(), Some(HELLO_SHA256));
        assert_eq!(parse_checksum("abc  file.zip"), None);
        assert_eq!(parse_checksum(""), None);
    }

    #[test]
    fn por_16_saves_bytes_with_their_hash_and_progress() {
        let folder = tempfile::tempdir().unwrap();
        let path = folder.path().join("file.part");
        let mut seen = Vec::new();
        let (size, hash) = save_hashing(&b"hello"[..], &path, 5, &mut |done, total| seen.push((done, total))).unwrap();
        assert_eq!((size, hash.as_str()), (5, HELLO_SHA256));
        assert_eq!(fs::read(&path).unwrap(), b"hello");
        assert_eq!(seen.last(), Some(&(5, 5)));
    }

    #[test]
    fn por_16_refuses_wrong_size_or_hash() {
        assert!(verify(&package(5), 5, HELLO_SHA256, HELLO_SHA256).is_ok());
        assert!(matches!(verify(&package(6), 5, HELLO_SHA256, HELLO_SHA256), Err(UpdateError::Integrity(_))));
        let other = "0".repeat(SHA256_HEX_LENGTH);
        assert!(matches!(verify(&package(5), 5, HELLO_SHA256, &other), Err(UpdateError::Integrity(_))));
    }

    #[test]
    fn por_16_partial_download_has_its_own_name() {
        assert_eq!(part_path(Path::new("/app/update/download/a.zip")), Path::new("/app/update/download/a.zip.part"));
    }
}
