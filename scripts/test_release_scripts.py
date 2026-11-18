"""Tests of the release scripts (POR-17): version computing and injection, release notes.

Usage: python3 -m unittest discover -s scripts -p 'test_*.py'
"""
import json
import shutil
import tempfile
import unittest
from pathlib import Path

import release_notes
import version
from git_history import ROOT, git


class Repository:
    """A temporary git repository; branches are merged with merge commits, as GitHub merges pull requests."""

    def __init__(self, folder: Path):
        self.path = folder
        git(folder, "init", "--quiet", "--initial-branch=main")

    def commit(self, subject: str, files: dict[str, str] | None = None) -> None:
        for name, content in (files or {}).items():
            (self.path / name).write_text(content, encoding="utf-8")
        git(self.path, "add", "--all")
        git(self.path, "-c", "user.name=Test", "-c", "user.email=test@example.com",
            "commit", "--quiet", "--allow-empty", "-m", subject)

    def tag(self, name: str) -> None:
        git(self.path, "tag", name)

    def branch(self, name: str) -> None:
        git(self.path, "switch", "--quiet", "--create", name)

    def switch(self, name: str) -> None:
        git(self.path, "switch", "--quiet", name)

    def merge(self, branch: str, subject: str, body: str = "") -> None:
        messages = ["-m", subject] + (["-m", body] if body else [])
        git(self.path, "-c", "user.name=Test", "-c", "user.email=test@example.com",
            "merge", "--quiet", "--no-ff", *messages, branch)


class RepositoryTest(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory()
        self.addCleanup(folder.cleanup)
        self.repo = Repository(Path(folder.name))


class ComputeTest(RepositoryTest):
    def test_patch_counts_commits_since_version_file_changed(self):
        self.repo.commit("Start", {"VERSION": "0.2\n"})
        self.assertEqual(version.compute(self.repo.path, dev=False), "0.2.0")
        self.repo.commit("Fix a bug")
        self.repo.commit("Add a screen")
        self.assertEqual(version.compute(self.repo.path, dev=False), "0.2.2")

    def test_new_minor_starts_at_patch_zero(self):
        self.repo.commit("Start", {"VERSION": "0.2\n"})
        self.repo.commit("Fix a bug")
        self.repo.commit("Start 0.3", {"VERSION": "0.3\n"})
        self.assertEqual(version.compute(self.repo.path, dev=False), "0.3.0")

    def test_builds_off_main_are_pre_releases(self):
        self.repo.commit("Start", {"VERSION": "1.0\n"})
        sha = git(self.repo.path, "rev-parse", "--short=7", "HEAD")
        self.assertEqual(version.compute(self.repo.path, dev=True), f"1.0.0-dev.{sha}")

    def test_version_file_must_hold_major_and_minor(self):
        self.repo.commit("Start", {"VERSION": "0.2.1\n"})
        with self.assertRaises(ValueError):
            version.compute(self.repo.path, dev=False)


class ApplyTest(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory()
        self.addCleanup(folder.cleanup)
        self.copy = Path(folder.name)
        for relative in version.BUILD_FILES:
            (self.copy / relative).parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / relative, self.copy / relative)

    def read(self, relative: str) -> str:
        return (self.copy / relative).read_text(encoding="utf-8")

    def test_writes_version_into_every_build_file(self):
        version.apply(self.copy, "0.2.7")
        self.assertEqual(json.loads(self.read("src-tauri/tauri.conf.json"))["version"], "0.2.7")
        self.assertEqual(json.loads(self.read("frontend/package.json"))["version"], "0.2.7")
        lock = json.loads(self.read("frontend/package-lock.json"))
        self.assertEqual((lock["version"], lock["packages"][""]["version"]), ("0.2.7", "0.2.7"))
        self.assertRegex(self.read("src-tauri/Cargo.toml"), r'(?m)^name = "planejamento-mensal"\nversion = "0.2.7"')
        pom = self.read("backend/pom.xml")
        self.assertIn("<artifactId>planejamento-mensal-backend</artifactId>\n    <version>0.2.7</version>", pom)

    def test_only_the_version_changes(self):
        before = {relative: self.read(relative) for relative in ("src-tauri/Cargo.toml", "backend/pom.xml")}
        version.apply(self.copy, "0.2.7")
        for relative, text in before.items():
            changed = [line for line in self.read(relative).splitlines() if line not in text.splitlines()]
            self.assertEqual(len(changed), 1, relative)
            self.assertIn("0.2.7", changed[0])

    def test_refuses_what_is_not_a_version(self):
        with self.assertRaises(ValueError):
            version.apply(self.copy, "latest")


class ReleaseNotesTest(RepositoryTest):
    def test_lists_commits_since_previous_release(self):
        self.repo.commit("Start", {"VERSION": "0.2\n"})
        self.repo.tag("v0.2.0")
        self.repo.commit("Fix the import of closed bills")
        self.repo.commit("Show the update notice")
        self.assertEqual(release_notes.notes(self.repo.path),
                         "- Show the update notice\n- Fix the import of closed bills")

    def test_first_release_lists_every_commit(self):
        self.repo.commit("Start", {"VERSION": "0.2\n"})
        self.repo.commit("Add the overview")
        self.assertEqual(release_notes.notes(self.repo.path), "- Add the overview\n- Start")

    def test_tag_of_the_released_commit_itself_is_skipped(self):
        self.repo.commit("Start", {"VERSION": "0.2\n"})
        self.repo.tag("v0.2.0")
        self.repo.commit("Fix totals")
        self.repo.tag("v0.2.1")
        self.assertEqual(release_notes.notes(self.repo.path), "- Fix totals")

    def test_merged_pull_request_lists_its_title_and_commits(self):
        self.repo.commit("Start", {"VERSION": "0.3\n"})
        self.repo.tag("v0.3.3")
        self.repo.branch("feature/export")
        self.repo.commit("Export the database")
        self.repo.commit("Import a database file")
        self.repo.switch("main")
        self.repo.merge("feature/export", "Merge pull request #4 from owner/feature/export", "Export and import the database")
        self.assertEqual(release_notes.notes(self.repo.path),
                         "- Export and import the database (#4)\n"
                         "  - Import a database file\n"
                         "  - Export the database")

    def test_merges_of_main_into_the_branch_are_left_out(self):
        self.repo.commit("Start", {"VERSION": "0.3\n"})
        self.repo.tag("v0.3.3")
        self.repo.branch("feature/debug")
        self.repo.commit("Add a debug mode")
        self.repo.switch("main")
        self.repo.commit("Fix the overview")
        self.repo.switch("feature/debug")
        self.repo.merge("main", "Merge branch 'main' into feature/debug")
        self.repo.switch("main")
        self.repo.merge("feature/debug", "Merge pull request #5 from owner/feature/debug", "Debug mode")
        self.assertEqual(release_notes.notes(self.repo.path),
                         "- Debug mode (#5)\n"
                         "  - Add a debug mode\n"
                         "- Fix the overview")

    def test_merge_without_a_pull_request_title_keeps_its_subject(self):
        self.repo.commit("Start", {"VERSION": "0.3\n"})
        self.repo.branch("hotfix")
        self.repo.commit("Fix the backup name")
        self.repo.switch("main")
        self.repo.merge("hotfix", "Merge branch 'hotfix'")
        self.assertEqual(release_notes.notes(self.repo.path),
                         "- Merge branch 'hotfix'\n  - Fix the backup name\n- Start")

    def test_notes_of_an_earlier_release_are_rebuilt_from_its_tag(self):
        self.repo.commit("Start", {"VERSION": "0.3\n"})
        self.repo.tag("v0.3.3")
        self.repo.commit("Fix totals")
        self.repo.tag("v0.3.4")
        self.repo.commit("Show the update notice")
        self.assertEqual(release_notes.notes(self.repo.path, "v0.3.4"), "- Fix totals")


if __name__ == "__main__":
    unittest.main()
