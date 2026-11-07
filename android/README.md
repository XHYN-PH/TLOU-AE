# Android build and validation

**Base release: v0.1, ARM64 Bionic / FEX / Vulkan.** This fork adds experimental
performance controls. See the implementation status for this fork's actual
build checks and outstanding phone validation; upstream gameplay results do
not validate these changes.
User installation and compatibility: [English](../README.md) /
[Português](../README.pt-BR.md). See [release notes](../RELEASE_NOTES.md).

## Android source

This fork now includes the Android implementation in `android/`,
`android-launcher/` and `gow3_pc/`. Use the current checkout to develop these
changes. The upstream branch below is the reference base, not a replacement
for this fork's modified source. See [implementation status](IMPLEMENTATION_STATUS.md)
for completed work, validation limits and remaining investigations.

## Prebuild checks for this fork

With Python 3.12 (`unicorn==2.1.4`), GCC/G++, a full JDK 17, Gradle 8.14.3,
Android SDK/platform and build-tools 35, and Android NDK r29 (`TLOU_NDK`) configured:

```sh
bash tools/android_prebuild.sh
```

This runs preparation/patch tests, host native contract tests, FPS/save tests,
Android UI/unit tests and release lint. It does **not** assemble or sign an APK.
The complete Android native library build and phone gameplay remain separate
checks; a host contract test cannot validate Vulkan execution on Adreno.

The percentage scale applies to the SHA-256-guarded **01.00** profile only.
It accepts 10–100%, rounds dimensions to the same 16:9 geometry units used by
the verified presets and shows the actual dimensions. Sub-360p scales are
experimental. 01.11 still requires Original resolution. No game bytes are
patched without matching the existing executable and instruction guards.

### ARM64 renderer profile-guided optimization

`android/bionic/build_gpu.sh` accepts `TLOU_ANDROID_PGO=off` (default),
`generate`, or `use`, and `TLOU_PGO_DIR`. Do not mix instrumentation and use
objects; use separate `TLOU_BIONIC_GPU_BUILD` directories. An instrumented
test APK is required to collect a representative outdoor and indoor run.
After closing the game, log export includes up to eight complete raw profiles
from the runtime's `profiles/` folder (up to 64 MiB each). Normal builds do
not produce training profiles.

```sh
# Merge raw profiles collected from the Android ARM64 training build:
"$TLOU_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-profdata" merge \
  -o /path/to/pgo/tlou.profdata /path/to/exported/profiles/*.profraw
python3 android/bionic/pgo.py record /path/to/pgo/tlou.profdata --ndk "$TLOU_NDK"
# A later use build validates profile format, target metadata, source,
# compiler identity and checksum before configuration.
```

The manifest guards against accidental profile mismatches; recording a manifest
does not prove where a profile was collected. Record only profiles from the
matching Android training build. No Android PGO gain has been measured yet.

## Upstream reference checkout

The [upstream Android source tree](https://github.com/erickmendes22/tlou-remastered-android/tree/tlou-ui-approval-log32-20261105)
is retained as a reference. The historical Windows branch is not the Android
APK implementation. The following clone example is for studying that reference;
use this fork's existing checkout to retain the changes above.

```sh
git clone --recurse-submodules --branch tlou-ui-approval-log32-20261105 https://github.com/erickmendes22/tlou-remastered-android.git
cd tlou-remastered-android
```

The existing branch name is a development identifier, not a user-facing setting.

## Build workflow

The [verified Android workflow](https://github.com/erickmendes22/tlou-remastered-android/tree/tlou-ui-approval-log32-20261105/.github/workflows/tlou-native-apk.yml)
selects a full native build when native inputs change, or reuses a pinned,
verified native payload when allowed inputs remain compatible. It runs
preparation tests, Android UI tests and release lint, assembles and signs the
APK, then verifies ELF dependencies, JNI, installer components and drivers.

The source checks also execute the production x86 submesh-guard bytes under
Unicorn. APK signing identity is preserved for compatible updates.

For an executable-patch-table update, only the explicitly allowed Python source
member is refreshed in the installer archive. Other installer members and the
native/runtime/driver payload are compared byte-for-byte against the pinned
build. A renderer or runtime change still requires a native build.

Documentation edits do not require a new APK. This repository does not publish
a new APK solely because a microbenchmark is faster.

## Local native build

The scripts require a Linux build host, Android SDK/NDK, JDK 17, Gradle and the
build dependencies configured by the workflow. Consult that workflow for exact
versions and setup before running the commands.

```sh
export TLOU_NDK=/path/to/android-ndk-r29
bash android/bionic/build_deps.sh
bash android/bionic/build_fex.sh
bash android/bionic/build_gpu.sh
bash android/bionic/build_runtime.sh --link-only
bash android/bionic/build_python.sh
bash android/bionic/build_pkg.sh
python3 android/bionic/stage_tools.py
bash android/bionic/package_apk.sh
```

For a development APK installed alongside the upstream app, use
`TLOU_APK_CHANNEL=test bash android/bionic/package_apk.sh`. This produces
`out/release/TLOU-AE-test-arm64.apk`, package `com.xhyn.tlou.ae.test`, labeled
**TLOU AE Test**. It has separate app settings and private saves. Keep the
existing installation; select the game folder again in the test app. Retain
`out/android-bionic/ae-test-development.jks` privately for compatible future
test updates. Never include the private key in a shared artifact.

When rebuilding the unchanged CPython/PKG dependencies is unavailable, the
installer-only cache can be restored with
`python3 android/bionic/restore_installer.py /path/to/upstream-v0.1.apk`.
It accepts only the pinned upstream SHA-256. This replaces the Python/PKG build
steps above; current preparation scripts are still staged from this checkout,
and all modified native runtime/renderer components still require compilation.

These are build steps, not instructions for players: the APK includes the
on-device preparation tools. Do not substitute another game's native binaries.

## Validation without game files

```sh
python3 -m pip install unicorn==2.1.4
python3 -m unittest discover -s android/tests -v
bash android/bionic/test_runtime_contracts.sh
bash android/bionic/test_fps.sh
python3 gow3_pc/tests/test_android_save_choice.py
gradle -p android-launcher :app:testReleaseUnitTest :app:lintVitalRelease
```

The Java tests require Android SDK setup. The final APK validation is:

```sh
python3 android/bionic/check_tools_apk.py out/release/TheLastOfUsRemastered-Android-v0.1-arm64.apk
```

The validated executable profile is SHA-256 checked and every patch requires
matching original instruction bytes. Unknown executables do not acquire known
patches merely through a matching title ID. Game files are neither test fixtures
nor release artifacts.

## Runtime boundaries

- Readback 1 is retained. The observed 01.00 physics output has its selective
  EOP readback path; do not restore global Readback 2.
- App language selection is independent of the game's own language menu.
- Save metadata and thumbnail handling must retain the selected native slot.
- The static-ray-hit bounds guard skips only invalid results and keeps the
  original path for valid hits; its source audit is retained in the Android tree.
- Automated tests and successful builds do not replace device regression tests.

CPU translation uses FEX; the TLOU runtime, renderer, Android presenter and
installer retain their respective source and third-party licensing obligations.
