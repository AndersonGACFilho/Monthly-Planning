"""POR-17: the version of a build, computed in one place and written into the build files by CI.

VERSION holds <major>.<minor>; the patch is the number of first-parent commits since VERSION last changed, so every
commit on main has exactly one version and a re-run computes the same one. Builds off main get a pre-release
suffix, which the in-app updater never offers.

Usage:
  python3 scripts/version.py compute [--dev]   prints 0.2.7, or 0.2.7-dev.1a2b3c4 with --dev
  python3 scripts/version.py apply <version>   writes it into the build files (the CI working tree, never committed)
"""
import json
import re
import sys
from collections.abc import Callable
from pathlib import Path

from git_history import ROOT, git

VERSION_FILE = "VERSION"
MAJOR_MINOR = re.compile(r"\d+\.\d+")
SEMVER = re.compile(r"\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?")

Edit = Callable[[str, str], str]


def compute(repo: Path, dev: bool) -> str:
    major_minor = (repo / VERSION_FILE).read_text(encoding="utf-8").strip()
    if not MAJOR_MINOR.fullmatch(major_minor):
        raise ValueError(f"{VERSION_FILE} must hold <major>.<minor>, found {major_minor!r}")
    since = git(repo, "log", "-1", "--format=%H", "--", VERSION_FILE)
    if not since:
        raise ValueError(f"{VERSION_FILE} is not committed")
    patch = git(repo, "rev-list", "--count", "--first-parent", f"{since}..HEAD")
    version = f"{major_minor}.{patch}"
    return f"{version}-dev.{git(repo, 'rev-parse', '--short=7', 'HEAD')}" if dev else version


def json_fields(*paths: tuple[str, ...]) -> Edit:
    """Sets the version at each key path of a JSON file."""
    def edit(text: str, version: str) -> str:
        document = json.loads(text)
        for path in paths:
            parent = document
            for key in path[:-1]:
                parent = parent[key]
            parent[path[-1]] = version
        return json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    return edit


def pattern(regex: str) -> Edit:
    """Replaces the version between the two groups of `regex`, which must match exactly once."""
    compiled = re.compile(regex, re.M | re.S)

    def edit(text: str, version: str) -> str:
        result, count = compiled.subn(lambda match: f"{match[1]}{version}{match[2]}", text)
        if count != 1:
            raise ValueError(f"expected one version to replace, found {count}")
        return result
    return edit


BUILD_FILES: dict[str, Edit] = {
    "src-tauri/tauri.conf.json": json_fields(("version",)),
    "src-tauri/Cargo.toml": pattern(r'(^\[package\][^\[]*?^version = ")[^"]*(")'),
    "frontend/package.json": json_fields(("version",)),
    "frontend/package-lock.json": json_fields(("version",), ("packages", "", "version")),
    "backend/pom.xml": pattern(r"(<artifactId>planejamento-mensal-backend</artifactId>\s*<version>)[^<]*(</version>)"),
}


def apply(repo: Path, version: str) -> None:
    if not SEMVER.fullmatch(version):
        raise ValueError(f"not a version: {version!r}")
    for relative, edit in BUILD_FILES.items():
        path = repo / relative
        path.write_text(edit(path.read_text(encoding="utf-8"), version), encoding="utf-8")


def main(args: list[str]) -> None:
    match args:
        case ["compute"]:
            print(compute(ROOT, dev=False))
        case ["compute", "--dev"]:
            print(compute(ROOT, dev=True))
        case ["apply", version]:
            apply(ROOT, version)
            print(f"Build files set to {version}")
        case _:
            sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
