#!/usr/bin/env bash
# Source/regression checks only. Deliberately never assembles or signs an APK.
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
python=${TLOU_PYTHON:-python3}
"$python" -m unittest discover -s android/tests -v
bash android/bionic/test_runtime_contracts.sh
bash android/bionic/test_fps.sh
: "${TLOU_NDK:?Set TLOU_NDK to Android NDK r29}"
temporary=$(mktemp -d)
trap 'rm -rf "$temporary"' EXIT
"$TLOU_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android30-clang++" \
  -std=c++20 -O2 -Wall -Wextra -Werror -c gow3_pc/gpu/shim/gow3_mobile.cpp \
  -o "$temporary/mobile-arm64.o"
g++ -std=c++20 -O1 -g -fsanitize=address,undefined -fno-omit-frame-pointer \
  gow3_pc/tests/test_fault_limits.cpp -o "$temporary/fault-bounds-sanitized"
ASAN_OPTIONS=detect_leaks=0 "$temporary/fault-bounds-sanitized"
: "${ANDROID_HOME:?Set ANDROID_HOME to SDK 35}"
: "${JAVA_HOME:?Set JAVA_HOME to a complete JDK 17}"
gradle -p android-launcher -PtlouTestBuild=true :app:testReleaseUnitTest :app:lintRelease --no-daemon --max-workers=2
git diff --check
echo 'Prebuild checks completed; no APK assembled. Full native renderer compilation and device gameplay/FPS validation remain separate.'
