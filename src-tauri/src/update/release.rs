//! Releases published on GitHub (POR-16): the newest one above the running version, its zip, its checksum and the
//! notes of every version in between.

use super::error::UpdateError;
use super::{net, version};
use semver::Version;
use serde::Deserialize;

/// Releases of the project, newest first, as the GitHub API lists them.
pub const RELEASES_URL: &str = "https://api.github.com/repos/AndersonGACFilho/Monthly-Planning/releases?per_page=20";

/// A file attached to a release.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Asset {
    pub name: String,
    pub url: String,
    pub size: u64,
}

/// A release newer than the running version.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Release {
    pub version: Version,
    /// Notes of this release and of every skipped one, newest first.
    pub notes: String,
    /// The portable zip for Windows and Linux.
    pub package: Asset,
    /// Its `.sha256` file.
    pub checksum: Asset,
}

#[derive(Deserialize)]
struct GithubRelease {
    tag_name: String,
    #[serde(default)]
    body: Option<String>,
    #[serde(default)]
    draft: bool,
    #[serde(default)]
    prerelease: bool,
    #[serde(default)]
    assets: Vec<GithubAsset>,
}

#[derive(Deserialize)]
struct GithubAsset {
    name: String,
    browser_download_url: String,
    size: u64,
}

/// Asks GitHub for the newest release above `current`; `None` when the app is up to date.
pub fn fetch_newer(url: &str, current: &Version) -> Result<Option<Release>, UpdateError> {
    let json = net::api_agent()
        .get(url)
        .header("Accept", "application/vnd.github+json")
        .call()?
        .body_mut()
        .read_to_string()?;
    newest_after(&json, current)
}

/// Picks the newest published release above `current` from the API answer.
pub fn newest_after(json: &str, current: &Version) -> Result<Option<Release>, UpdateError> {
    let releases: Vec<GithubRelease> =
        serde_json::from_str(json).map_err(|e| UpdateError::Release(format!("unreadable release list: {e}")))?;
    let mut newer: Vec<(Version, GithubRelease)> = releases
        .into_iter()
        .filter(|release| !release.draft && !release.prerelease)
        .filter_map(|release| version::newer_than(current, &release.tag_name).map(|v| (v, release)))
        .collect();
    newer.sort_by(|(a, _), (b, _)| b.cmp(a));
    let Some((version, newest)) = newer.first() else {
        return Ok(None);
    };
    let package_name = format!("PlanejamentoMensal-{}.zip", newest.tag_name);
    let package = asset(newest, &package_name)?;
    let checksum = asset(newest, &format!("{package_name}.sha256"))?;
    Ok(Some(Release { version: version.clone(), notes: joined_notes(&newer), package, checksum }))
}

fn asset(release: &GithubRelease, name: &str) -> Result<Asset, UpdateError> {
    release
        .assets
        .iter()
        .find(|asset| asset.name == name)
        .map(|asset| Asset { name: asset.name.clone(), url: asset.browser_download_url.clone(), size: asset.size })
        .ok_or_else(|| UpdateError::Release(format!("{} has no {name}", release.tag_name)))
}

fn joined_notes(releases: &[(Version, GithubRelease)]) -> String {
    releases
        .iter()
        .map(|(_, release)| format!("{}\n{}", release.tag_name, release.body.as_deref().unwrap_or("").trim()))
        .collect::<Vec<_>>()
        .join("\n\n")
}

#[cfg(test)]
mod tests {
    use super::*;

    fn release_json(tag: &str, body: &str, flags: &str, with_checksum: bool) -> String {
        let checksum = if with_checksum {
            format!(r#",{{"name":"PlanejamentoMensal-{tag}.zip.sha256","browser_download_url":"https://x/{tag}.sha256","size":90}}"#)
        } else {
            String::new()
        };
        format!(
            r#"{{"tag_name":"{tag}","body":"{body}",{flags}"assets":[{{"name":"PlanejamentoMensal-{tag}.zip","browser_download_url":"https://x/{tag}.zip","size":1234}}{checksum}]}}"#
        )
    }

    fn current() -> Version {
        Version::parse("0.1.0").unwrap()
    }

    #[test]
    fn por_16_picks_newest_release_with_notes_of_every_skipped_version() {
        let json = format!(
            "[{},{},{},{},{}]",
            release_json("v0.3.0", "- third", "", true),
            release_json("v0.4.0-rc.1", "- candidate", r#""prerelease":true,"#, true),
            release_json("v0.5.0", "- draft", r#""draft":true,"#, true),
            release_json("v0.2.0", "- second", "", true),
            release_json("v0.1.0", "- first", "", true),
        );
        let release = newest_after(&json, &current()).unwrap().unwrap();
        assert_eq!(release.version, Version::parse("0.3.0").unwrap());
        assert_eq!(release.notes, "v0.3.0\n- third\n\nv0.2.0\n- second");
        assert_eq!(release.package.url, "https://x/v0.3.0.zip");
        assert_eq!(release.package.size, 1234);
        assert_eq!(release.checksum.name, "PlanejamentoMensal-v0.3.0.zip.sha256");
    }

    #[test]
    fn por_16_up_to_date_when_nothing_is_newer() {
        let json = format!("[{}]", release_json("v0.1.0", "- first", "", true));
        assert_eq!(newest_after(&json, &current()).unwrap(), None);
        assert_eq!(newest_after("[]", &current()).unwrap(), None);
    }

    #[test]
    fn por_16_release_without_checksum_is_refused() {
        let json = format!("[{}]", release_json("v0.2.0", "- second", "", false));
        assert!(matches!(newest_after(&json, &current()), Err(UpdateError::Release(_))));
    }

    #[test]
    fn por_16_unexpected_answer_is_an_error() {
        assert!(matches!(newest_after(r#"{"message":"rate limited"}"#, &current()), Err(UpdateError::Release(_))));
    }
}
