"""POR-17: release notes of the commit being released, made of the commits since the previous release.

A merged pull request appears as its title and number, followed by the commits it brought (IMP-74).

Usage: python3 scripts/release_notes.py [revision] > notes.md   (revision defaults to HEAD, such as a past tag)
"""
import re
import subprocess
import sys
from pathlib import Path

from git_history import ROOT, git

PULL_REQUEST = re.compile(r"Merge pull request #(\d+) from ")


def previous_release(repo: Path, revision: str = "HEAD") -> str | None:
    """The newest `v*` tag before `revision`, or None for the first release."""
    try:
        return git(repo, "describe", "--tags", "--abbrev=0", "--match", "v*", f"{revision}^")
    except subprocess.CalledProcessError:
        return None


def notes(repo: Path, revision: str = "HEAD") -> str:
    since = previous_release(repo, revision)
    commits = git(repo, "log", "--first-parent", "--format=%H", f"{since}..{revision}" if since else revision)
    return "\n".join(line for commit in commits.splitlines() for line in entry(repo, commit))


def entry(repo: Path, commit: str) -> list[str]:
    """One commit of the main line: a plain commit is its subject; a merge is its title over the merged commits."""
    parents = git(repo, "log", "-1", "--format=%P", commit).split()
    subject = git(repo, "log", "-1", "--format=%s", commit)
    if len(parents) < 2:
        return [f"- {subject}"]
    merged = git(repo, "log", "--no-merges", "--format=%s", f"{parents[0]}..{parents[1]}")
    return [f"- {merge_title(repo, commit, subject)}", *(f"  - {line}" for line in merged.splitlines())]


def merge_title(repo: Path, commit: str, subject: str) -> str:
    """The pull request title GitHub writes under a merge subject, with its number; otherwise the subject."""
    match = PULL_REQUEST.match(subject)
    title = git(repo, "log", "-1", "--format=%b", commit).splitlines()
    return f"{title[0]} (#{match.group(1)})" if match and title else subject


if __name__ == "__main__":
    print(notes(ROOT, sys.argv[1] if len(sys.argv) > 1 else "HEAD"))
