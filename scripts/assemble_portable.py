"""Assembles the portable folder (POR-01, SPEC section 12).

Usage:
  python scripts/assemble_portable.py --os linux|windows --shell <binary> --jar <app.jar>
                                      --runtime <jlink dir> --out <folder>

Running it once per OS on the same --out folder gives one folder for both systems: a single app.jar,
one JRE per OS and a shared, empty data/ folder.
"""
import argparse
import shutil
import stat
import sys
from pathlib import Path

PACKAGING = Path(__file__).resolve().parent.parent / "packaging"

SHELL_NAMES = {"windows": "PlanejamentoMensal.exe", "linux": "PlanejamentoMensal"}


def exfat_problems(folder: Path) -> list[str]:
    """POR-02: an exFAT drive has no symlinks, no POSIX permissions and ignores case in names."""
    problems = []
    seen: dict[str, Path] = {}
    for path in sorted(folder.rglob("*")):
        if path.is_symlink():
            problems.append(f"symlink: {path.relative_to(folder)}")
        key = str(path.relative_to(folder)).lower()
        if key in seen:
            problems.append(f"names differ only by case: {seen[key].relative_to(folder)} / {path.relative_to(folder)}")
        seen[key] = path
        if any(ch in path.name for ch in '<>:"|?*'):
            problems.append(f"name not allowed on exFAT: {path.relative_to(folder)}")
    return problems


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--os", choices=SHELL_NAMES, required=True)
    parser.add_argument("--shell", type=Path, required=True)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--runtime", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--appimage", type=Path, help="Linux AppImage to ship next to the plain binary")
    args = parser.parse_args()

    out: Path = args.out
    backend = out / "backend"
    runtime = backend / f"runtime-{args.os}"
    backend.mkdir(parents=True, exist_ok=True)
    (out / "data").mkdir(exist_ok=True)

    shell = out / SHELL_NAMES[args.os]
    shutil.copy2(args.shell, shell)
    shell.chmod(shell.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    shutil.copy2(args.jar, backend / "app.jar")
    if runtime.exists():
        shutil.rmtree(runtime)
    shutil.copytree(args.runtime, runtime, symlinks=False)
    if args.os == "linux":
        shutil.copy2(PACKAGING / "start-linux.sh", out / "start-linux.sh")
        if args.appimage:
            appimage = out / "PlanejamentoMensal.AppImage"
            shutil.copy2(args.appimage, appimage)
            appimage.chmod(appimage.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    shutil.copy2(PACKAGING / "LEIA-ME.md", out / "LEIA-ME.md")
    problems = exfat_problems(out)
    if problems:
        sys.exit("Portable folder is not exFAT-safe (POR-02):\n  " + "\n  ".join(problems))
    print(f"Portable folder ready for {args.os}: {out}")


if __name__ == "__main__":
    main()
