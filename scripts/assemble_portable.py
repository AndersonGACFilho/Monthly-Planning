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
from pathlib import Path

SHELL_NAMES = {"windows": "PlanejamentoMensal.exe", "linux": "PlanejamentoMensal"}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--os", choices=SHELL_NAMES, required=True)
    parser.add_argument("--shell", type=Path, required=True)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--runtime", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
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
    print(f"Portable folder ready for {args.os}: {out}")


if __name__ == "__main__":
    main()
