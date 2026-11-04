"""Reads the git history of a repository for the release scripts (POR-17)."""
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def git(repo: Path, *args: str) -> str:
    """Runs `git <args>` in `repo` and returns its output without the trailing newline."""
    return subprocess.run(["git", *args], cwd=repo, check=True, capture_output=True, text=True).stdout.strip()
