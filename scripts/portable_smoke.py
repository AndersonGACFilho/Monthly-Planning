"""Smoke test of the portable folder (IMP-07, POR-03, POR-08).

Copies the folder elsewhere, starts the backend with the bundled JRE exactly as the shell does, waits for
health, calls the API, stops it through the shutdown endpoint and checks that:
  - the backend ends with code 0, releasing the lock and the WAL file;
  - nothing was written outside the copied folder (HOME and temp dirs point to an empty sentinel).

Usage: python scripts/portable_smoke.py <portable folder> [--os linux|windows] [--in-place] [--java <java>]

--in-place runs the folder where it is (such as an exFAT drive mounted noexec, POR-02) instead of a copy;
--java runs the backend with another java, as start-linux.sh does with its temporary copy (POR-09).
"""
import os
import secrets
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request
from pathlib import Path


def free_port() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def call(port: int, method: str, path: str, token: str | None = None) -> int:
    request = urllib.request.Request(f"http://127.0.0.1:{port}{path}", method=method)
    if token:
        request.add_header("X-Session-Token", token)
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            return response.status
    except urllib.error.HTTPError as e:
        return e.code


def main(source: Path, os_name: str, in_place: bool = False, java_override: Path | None = None) -> None:
    with tempfile.TemporaryDirectory() as work:
        home = source.resolve() if in_place else Path(work) / "copied" / "PlanejamentoMensal"
        if not in_place:
            shutil.copytree(source, home)
        sentinel = Path(work) / "outside"
        sentinel.mkdir()
        env = {k: v for k, v in os.environ.items() if k in ("PATH", "SYSTEMROOT", "WINDIR", "LANG")}
        for key in ("HOME", "USERPROFILE", "TMP", "TEMP", "TMPDIR", "APPDATA", "LOCALAPPDATA",
                    "XDG_CONFIG_HOME", "XDG_DATA_HOME", "XDG_CACHE_HOME", "XDG_STATE_HOME"):
            env[key] = str(sentinel)
        token = secrets.token_hex(32)
        env["APP_SESSION_TOKEN"] = token
        port = free_port()
        java = java_override or home / "backend" / f"runtime-{os_name}" / "bin" / ("java.exe" if os_name == "windows" else "java")
        (home / "data" / "tmp").mkdir(parents=True, exist_ok=True)
        backend = subprocess.Popen(
            [str(java), f"-Djava.io.tmpdir={home / 'data' / 'tmp'}",
             "-jar", str(home / "backend" / "app.jar"), f"--app.home={home}", f"--server.port={port}",
             f"--app.parent-pid={os.getpid()}"],
            env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
        )
        deadline = time.time() + 120
        while time.time() < deadline:
            if backend.poll() is not None:
                sys.exit(f"backend ended during startup:\n{backend.stdout.read()}")
            try:
                if call(port, "GET", "/actuator/health") == 200:
                    break
            except OSError:
                pass
            time.sleep(0.5)
        else:
            backend.kill()
            sys.exit("backend did not become healthy")

        checks = {
            "API without token is refused": call(port, "GET", "/api") == 401,
            "API with token answers": call(port, "GET", "/api", token) == 200,
            "shutdown accepted": call(port, "POST", "/actuator/shutdown", token) == 200,
        }
        code = backend.wait(timeout=60)
        data = home / "data"
        checks |= {
            "backend exit code is 0": code == 0,
            "database created in data/": (data / "planejamento-mensal.db").is_file(),
            "lock released": not (data / "instance.lock").exists(),
            "WAL checkpointed": not (data / "planejamento-mensal.db-wal").exists()
            or (data / "planejamento-mensal.db-wal").stat().st_size == 0,
            "nothing written outside the folder": not any(sentinel.iterdir()),
        }
        if os_name == "linux" and (home / "start-linux.sh").exists():
            scratch = Path(work) / "start-linux-tmp"
            scratch.mkdir()
            run = subprocess.run(["sh", str(home / "start-linux.sh")], capture_output=True, text=True, timeout=300,
                                 env={**env, "TMPDIR": str(scratch), "PM_START_DRY_RUN": "1"})
            checks["start-linux.sh prepares a temporary copy (POR-09)"] = run.returncode == 0 and "would run" in run.stdout
            checks["start-linux.sh removes its copy"] = not any(scratch.iterdir())
        for name, ok in checks.items():
            print(f"{'OK  ' if ok else 'FAIL'} {name}")
        if not all(checks.values()):
            print(backend.stdout.read())
            leaked = [str(p.relative_to(sentinel)) for p in sentinel.rglob("*")]
            if leaked:
                print("Written outside:", leaked)
            sys.exit(1)


if __name__ == "__main__":
    os_arg = sys.argv[sys.argv.index("--os") + 1] if "--os" in sys.argv else ("windows" if os.name == "nt" else "linux")
    java_arg = Path(sys.argv[sys.argv.index("--java") + 1]) if "--java" in sys.argv else None
    main(Path(sys.argv[1]), os_arg, "--in-place" in sys.argv, java_arg)
