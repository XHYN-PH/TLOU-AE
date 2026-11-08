#!/usr/bin/env python3
"""Restore unchanged installer dependencies from the pinned upstream release.

Only CPython and the PKG extractor are cached here. The loader, CPU adapter,
GPU renderer and presenter must still be built from the current source. Run
stage_tools.py afterwards to bundle this checkout's preparation scripts.
"""
import argparse
import hashlib
import io
from pathlib import Path, PurePosixPath
import zipfile

UPSTREAM_SHA256 = "ccbdbfeeb1ef807b853a85102f7d148d9fcb7c6c13c779823e10621ec9197f85"


def restore(apk_path: Path, root: Path):
    if hashlib.sha256(apk_path.read_bytes()).hexdigest() != UPSTREAM_SHA256:
        raise ValueError("Installer cache APK does not match pinned upstream SHA-256")
    native = root / "out/android-bionic"
    with zipfile.ZipFile(apk_path) as apk:
        with zipfile.ZipFile(io.BytesIO(apk.read("assets/tools.zip"))) as archive:
            for name in archive.namelist():
                parts = PurePosixPath(name).parts
                if not parts or ".." in parts or PurePosixPath(name).is_absolute():
                    raise ValueError(f"Unsafe archive entry: {name}")
                if name.startswith("python/") and not name.endswith("/"):
                    target = native / name
                elif name == "bin/orbis-pkg-util":
                    target = native / name
                elif name == "licenses/LICENSE-Python.txt":
                    target = native / "python/LICENSE-Python.txt"
                elif name in ("licenses/orbis-LICENSE-MIT", "licenses/orbis-LICENSE-APACHE"):
                    target = root / ".local-deps/android/orbis-pkg" / name.removeprefix("licenses/orbis-")
                else:
                    continue
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(name))
                if name in ("python/bin/python3.13", "bin/orbis-pkg-util"):
                    target.chmod(0o755)
    print("Restored checksum-pinned CPython/PKG installer cache; current scripts are staged separately")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    restore(args.apk, Path(__file__).resolve().parents[2])
