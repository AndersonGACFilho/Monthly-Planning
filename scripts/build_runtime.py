"""Builds the minimal JRE for the current OS with jdeps + jlink (POR-01, SPEC section 12).

Usage: python scripts/build_runtime.py <app.jar> <output-dir>
Needs JAVA_HOME pointing to JDK 25.
"""
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

# Needed at runtime but invisible to jdeps: locale data for pt-BR/en-US formats (I18N-05), zip file
# system used by Spring Boot's nested jars, and the SSL/naming providers used by the mail client (RF-86).
EXTRA_MODULES = ["jdk.localedata", "jdk.zipfs", "jdk.crypto.cryptoki", "jdk.naming.dns"]


def tool(name: str) -> str:
    exe = name + (".exe" if os.name == "nt" else "")
    return str(Path(os.environ["JAVA_HOME"]) / "bin" / exe)


def main(jar: Path, output: Path) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        with zipfile.ZipFile(jar) as archive:
            archive.extractall(tmp)
        libs = os.pathsep.join(str(p) for p in Path(tmp, "BOOT-INF", "lib").glob("*.jar"))
        modules = subprocess.run(
            [tool("jdeps"), "--ignore-missing-deps", "--print-module-deps", "--multi-release", "25",
             "--recursive", "--class-path", libs, str(Path(tmp, "BOOT-INF", "classes"))],
            check=True, capture_output=True, text=True,
        ).stdout.strip().split(",")
    available = subprocess.run([tool("java"), "--list-modules"], check=True, capture_output=True, text=True).stdout
    extra = [m for m in EXTRA_MODULES if f"{m}@" in available]
    all_modules = sorted(set(m for m in modules if m) | set(extra))
    if output.exists():
        shutil.rmtree(output)
    subprocess.run(
        [tool("jlink"), "--add-modules", ",".join(all_modules), "--strip-debug", "--no-man-pages",
         "--no-header-files", "--compress", "zip-6", "--output", str(output)],
        check=True,
    )
    print(f"Runtime with {len(all_modules)} modules: {','.join(all_modules)}")


if __name__ == "__main__":
    main(Path(sys.argv[1]), Path(sys.argv[2]))
