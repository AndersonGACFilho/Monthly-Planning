//! Release versions: `v<major>.<minor>.<patch>` tags compared with the running version (POR-16).

use semver::Version;

/// Parses a release tag such as `v0.2.0`; `None` for anything else.
pub fn parse_tag(tag: &str) -> Option<Version> {
    Version::parse(tag.strip_prefix('v').unwrap_or(tag)).ok()
}

/// The version of `tag` when it is a final (not pre-release) version newer than `current`.
pub fn newer_than(current: &Version, tag: &str) -> Option<Version> {
    parse_tag(tag).filter(|candidate| candidate.pre.is_empty() && candidate > current)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn version(text: &str) -> Version {
        Version::parse(text).unwrap()
    }

    #[test]
    fn por_16_reads_tags_with_and_without_prefix() {
        assert_eq!(parse_tag("v0.2.0"), Some(version("0.2.0")));
        assert_eq!(parse_tag("1.10.3"), Some(version("1.10.3")));
        assert_eq!(parse_tag("latest"), None);
        assert_eq!(parse_tag("v1.2"), None);
    }

    #[test]
    fn por_16_only_newer_final_versions_count() {
        let current = version("0.9.0");
        assert_eq!(newer_than(&current, "v0.10.0"), Some(version("0.10.0")));
        assert_eq!(newer_than(&current, "v1.0.0"), Some(version("1.0.0")));
        assert_eq!(newer_than(&current, "v0.9.0"), None);
        assert_eq!(newer_than(&current, "v0.8.5"), None);
        assert_eq!(newer_than(&current, "v1.0.0-beta.1"), None);
        assert_eq!(newer_than(&current, "nightly"), None);
    }
}
