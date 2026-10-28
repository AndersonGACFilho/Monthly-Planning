"""POR-16: a release tag must match the version built into the app.

The app compares its own version with the newest GitHub release; if the tag and the built version differ, every
install would see itself as outdated, or never see the update. Checks src-tauri/tauri.conf.json (the version the
shell reports), src-tauri/Cargo.toml and frontend/package.json.

Usage: python3 scripts/check_version.py v0.2.0
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def versions() -> dict[str, str]:
    cargo = (ROOT / "src-tauri/Cargo.toml").read_text(encoding="utf-8")
    package = re.search(r'^\[package\][^\[]*?^version = "([^"]+)"', cargo, re.M | re.S)
    return {
        "src-tauri/tauri.conf.json": json.loads((ROOT / "src-tauri/tauri.conf.json").read_text(encoding="utf-8"))["version"],
        "src-tauri/Cargo.toml": package.group(1) if package else "",
        "frontend/package.json": json.loads((ROOT / "frontend/package.json").read_text(encoding="utf-8"))["version"],
    }


def main(tag: str) -> None:
    expected = tag.removeprefix("v")
    wrong = {file: version for file, version in versions().items() if version != expected}
    if wrong:
        sys.exit(f"Tag {tag} does not match: " + ", ".join(f"{file} has {version!r}" for file, version in wrong.items()))
    print(f"Version {expected} matches the tag")


if __name__ == "__main__":
    main(sys.argv[1])
