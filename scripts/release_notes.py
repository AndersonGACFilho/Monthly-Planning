"""POR-17: release notes of the commit being released, made of the commit subjects since the previous release.

Usage: python3 scripts/release_notes.py > notes.md
"""
import subprocess
from pathlib import Path

from git_history import ROOT, git


def previous_release(repo: Path) -> str | None:
    """The newest `v*` tag before HEAD, or None for the first release."""
    try:
        return git(repo, "describe", "--tags", "--abbrev=0", "--match", "v*", "HEAD^")
    except subprocess.CalledProcessError:
        return None


def notes(repo: Path) -> str:
    since = previous_release(repo)
    subjects = git(repo, "log", "--first-parent", "--format=%s", f"{since}..HEAD" if since else "HEAD")
    return "\n".join(f"- {subject}" for subject in subjects.splitlines())


if __name__ == "__main__":
    print(notes(ROOT))
