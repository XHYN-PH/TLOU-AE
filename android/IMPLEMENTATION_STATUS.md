# Historical 0.1 implementation and test APK status

For the current 0.2 work, see [refinement status](REFINEMENT_STATUS.md).

Development work requested for Snapdragon 8 Gen 3 / Adreno 750. An ARM64 test
APK was built, signed and verified on 2026-11-07. This is not a 60 FPS result;
target-phone gameplay and performance remain unmeasured.

Artifact: `out/release/TLOU-AE-test-arm64.apk` (45,000,120 bytes).
SHA-256: `6e72eed0d97daee4416ac89eff3e05c1269a93bd5664f8ca233a943253cf57fc`.
Package: `com.xhyn.tlou.ae.test`, label **TLOU AE Test**. It installs alongside
the existing app and uses separate app settings/private saves.
See [testing instructions](TEST_APK.md).

The complete Android implementation was integrated from upstream commit
`b9e2fecbe9d7293215590b9996f14c8a015375c6` into the previously Windows-focused
checkout. This includes the upstream unified scene/depth/post/HUD/aim fix,
FEX/Bionic runtime, launcher, presenter and existing regression tests.
Upstream credits and GPL notices remain intact. The original checkout had
no user modifications before integration.

## Requested items

| # | Item | Actual state |
| --- | --- | --- |
| 1 | Correct resolution scaling | Integrated upstream's guarded 01.00 unified-resolution implementation; covered by preparation tests. Game/device validation pending. |
| 2 | Percentage slider | Implemented 10–100%, 1% requested steps with rounded actual dimensions displayed, saved settings, and legacy presets. 33/50/67% produce exactly the existing 360p/540p/720p patch bytes. Sub-360p scales are experimental; zero is rejected. |
| 3 | Configurable frame overlap | Implemented independent Off/1/2 frame settings, default Off, saved across launches and included in reports. Experimental until device retirement/load/physics tests. |
| 4 | Configurable pipeline draining | Implemented Conservative/default and opt-in nested-compute-buffer policy. The known unsafe global PC drain/no-wait modes are explicitly kept off. The global race is not claimed fixed. |
| 5 | Diagnostics/export | Implemented normal/CPU-phase/GPU-pass modes, in-game export, settings entry, bounded log snapshots and private-path filtering for preference exports. GPU profiling is never silently enabled. Reset refreshes controls immediately. |
| 6 | PC safety/cache fixes | Backported fault bitmap initialization, bounded list consumption, and DMA cache compatibility. Added a shared 64-bit-safe bound helper and sanitizer boundary checks. |
| 7 | Texture cleanup/retention | Fixed deletion allowance spent on skipped entries and reversed critical image age. Added bounded scanning and an opt-in warm-texture grace period that yields to pressure. Tested shared cleanup policy with the real LRU implementation. |
| 8 | Mobile memory policy | Added opt-in RAM/swap/PSI budget adjustment with active-usage floor, slow recovery, driver-budget cap, and missing-data fallback. Host policy tests and NDK compilation pass. Device effect is unmeasured. |
| 9 | Adreno driver comparison | Existing compatible driver selection/import retained. Reports include the selected driver. A real same-scene comparison requires the phone; no winning driver is claimed. |
| 10 | Draw/resource CPU optimization | Existing CPU trims, renderer buffer reuse, compute-specialization memoization and copy batching are individually configurable. New FastBind/DrawReplay algorithms are not implemented without TLOU reuse/phase measurements. |
| 11 | Optimize measured GPU shaders | GPU-pass diagnostics are selectable. No new shader-math/workgroup rewrite is claimed; fresh target-device timings/shader coverage are required. Previously neutral experiments are not presented as new gains. |
| 12 | Upscaling/dynamic resolution | Fixed/custom internal scaling is implemented. New temporal/spatial-upscaler integration and automatic live resolution switching are not implemented; correct GPU inputs, image lifetimes and device validation are prerequisites. Existing temporal injections remain off. |
| 13 | Shadow/light optimization investigation | Reviewed PT4Droid/SotC mechanisms. Engine-level shadow/light caching is not implemented: TLOU needs verified light/caster/resource identity and coverage first. |
| 14 | Performance hints/thermal/pacing | Added opt-in native ADPF sessions for actual main/command/draw/recorder threads, CPU-work reporting excluding waits, API-failure fallback and thermal status logging. Existing FPS/VSync pacing retained. No thermal limits are bypassed. |
| 15 | ARM64 PGO | Added off/generate/use build modes, profile-format/compiler/source/checksum guards, profile-output routing and complete post-exit raw-profile export. A training APK/phone run, merged real profile and measured use build remain outstanding. |
| 16 | Investigate AOT ARM64 recompilation | Feasibility review complete. No TLOU AOT backend is implemented. It requires TLOU-specific executable/module/control-flow/import/patch coverage and game/device tests; Bloodborne's generated library cannot substitute for it. |
| 17 | Stencil/rain fixes | Added PC stencil-copy helpers and hash-specific rain correction behind independent Android settings, default Off. Cache metadata is rejected for the rain shader to prevent cross-mode reuse. Full ARM64 renderer compilation/linking passed; visual validation remains pending. |

## Source and regression checks

- Python preparation/patch suite: 42 tests passed, including every supported
  percentage, exact legacy-preset parity, malformed input, executable guards,
  atomic patch-file behavior and PGO manifest mismatches.
- Java UI/unit tests: 21 passed, zero failed, zero skipped; includes reset
  refresh, full-size profile export and bounded log tails.
- Release vital lint passed. The final aggregate prebuild script passed.
- Existing host runtime contracts and FPS/save suites passed.
- New GC, RAM and fault-boundary tests passed; the fault helper also ran with
  AddressSanitizer/UndefinedBehaviorSanitizer.
- `gow3_mobile.cpp` compiled for both the host and Android ARM64/API 30 with
  strict warnings using NDK r29. This is not compilation of the full renderer.
- Settings layout rendered under Robolectric native graphics and was inspected.

The complete native GPU/FEX build, shader compilation, runtime/presenter link,
release assembly and signing subsequently passed with Android NDK r29. LTO is
enabled; ARM64 PGO is off. The new package identity also passed all 21 Java
tests and release vital lint.

Native dependency and strong-symbol checks passed. The final APK passed
signature verification, ZIP alignment/integrity, ARM64/JNI/installer/driver
checks and identity checks. Its loader matches the fresh output byte for
byte; all packaged runtime libraries match the fresh outputs after the same
NDK stripping operation. Every preparation-script archive member matches the
current checkout. No modified native payload was reused from the old APK.

Unchanged CPython and PKG extractor dependencies were restored from the
SHA-256-pinned upstream v0.1 APK after python.org rejected source download
with HTTP 403. Current preparation scripts were staged separately. The two
bundled driver ZIPs retain their pinned digests.

Build checks cannot establish that the Vulkan integration renders correctly
or improves FPS. Phone/device validation remains necessary; this build must
not be called regression-free.

## Corrections found during implementation

- Native-payload reuse now considers local edits and untracked native files;
  an unavailable baseline conservatively requires a fresh native build.
- Log snapshots have a fixed read extent, preventing a live log from growing
  an export indefinitely. Binary training profiles are not truncated.
- Selecting a fixed resolution or compatibility path disables the custom
  scale override. Reset restores the displayed advanced controls immediately.
- A missing Java import and an Android-incompatible Java test API call were
  caught by compilation and corrected.
- The CPU-diagnostics regression test now checks both the normal/off path
  and explicit bounded-CPU mode, reflecting the requested configurability.
- Failure to create an optional PGO-output directory does not prevent a
  normal game launch.

## Before calling this a performance release

The complete ARM64 build and APK dependency/JNI checks have passed. Test the
same outdoor scene and indoor control on the phone,
after shader warmup and through a sustained session. Validate HUD/aim, movies,
physics, streaming, save/load, crashes, thermal behavior and shader variants.
Change one experimental option at a time. Keep the stable defaults available.

Items needing target-device profiles cannot be completed **before the first
instrumented test build**. No supplied game dump, phone connection, current
device profile or matching shader capture is available in this environment.
These are explicit outstanding requirements, not completed optimizations.
