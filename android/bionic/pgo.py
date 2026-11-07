#!/usr/bin/env python3
"""Validate provenance before using an ARM64 renderer training profile.

After llvm-profdata merges device-collected raw profiles, run:
  pgo.py record profile.profdata --ndk /path/to/ndk
The record is tied to current native sources and the NDK compiler. This does
not produce training data or convert a desktop profile into an Android one.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def native_digest(root=ROOT):
    digest = hashlib.sha256()
    for directory in ('gow3_pc/gpu', 'gow3_pc/src'):
        for path in sorted((root / directory).rglob('*')):
            if path.is_file() and path.suffix in ('.h', '.hpp', '.c', '.cpp', '.inc'):
                digest.update(str(path.relative_to(root)).encode()+b'\0')
                digest.update(path.read_bytes())
    return digest.hexdigest()


def compiler(ndk):
    executable = Path(ndk)/'toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android30-clang++'
    return subprocess.check_output([str(executable), '--version'], text=True).splitlines()[0]


def validate(profile, metadata, compiler_version, source_digest):
    if metadata.get('target') != 'aarch64-linux-android':
        raise ValueError('PGO requires a device-collected Android ARM64 profile')
    if metadata.get('compiler') != compiler_version or metadata.get('native_sources') != source_digest:
        raise ValueError('PGO compiler/source mismatch; retrain this build')
    if metadata.get('profile_sha256') != hashlib.sha256(Path(profile).read_bytes()).hexdigest():
        raise ValueError('PGO profile checksum mismatch')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['record', 'check'])
    parser.add_argument('profile', type=Path)
    parser.add_argument('--ndk', required=True, type=Path)
    args = parser.parse_args()
    # The actual tool verifies the merged profile's format as well as its manifest.
    tool = args.ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-profdata'
    subprocess.run([str(tool), 'show', str(args.profile)], check=True, stdout=subprocess.DEVNULL)
    manifest = args.profile.with_suffix(args.profile.suffix+'.json')
    version, source = compiler(args.ndk), native_digest()
    if args.action == 'record':
        manifest.write_text(json.dumps({'target':'aarch64-linux-android', 'compiler':version,
            'native_sources':source, 'profile_sha256':hashlib.sha256(args.profile.read_bytes()).hexdigest()}, indent=2)+'\n')
    else:
        validate(args.profile, json.loads(manifest.read_text()), version, source)


if __name__ == '__main__': main()
