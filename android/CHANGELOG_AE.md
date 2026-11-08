# TLOU-AE 0.3 — XHYN_PH

8 November 2026 (Asia/Manila). Based directly on the verified Android **0.1.1** source commit `7256020d485dabbd3a3250d1dd12506b23455c45`, with previous AE modifications reapplied. The published 0.2 branch remains available.

- Integrated 0.1.1 guarded game-update handling, verified 01.11 resolution presets/movie-plane repair, retail-memory reporting, Vulkan semaphore bounds/stage masks and 3D-copy extent fixes.
- Adopted targeted readback page protection, required-sequence deferred-copy waits, reduced profiler overhead and completed-thread stack/TLS cleanup. Full geometry/physics readbacks remain enabled.
- Adapted the universal port's GPU BC1–BC7 decoder with **Auto / Off / Force** settings, signed/HDR/sRGB support checks and an actionable English unsupported-format error. Auto retains native sampling on capable drivers.
- Replaced the older CPU-time hint path with one native frame-deadline ADPF session, late thread enrollment and foreground reset. Added Java/native monotonic markers and actual Vulkan submits per flip for resume investigations.
- Added independent advertised display-rate choices, retaining Android 11 FIFO safeguards.
- Updated pinned bundled drivers to Adreno 6xx/7xx R2 and Adreno 8xx V39; preserved custom selections. Removed obsolete generated driver bundles from APK staging.
- Added exact pending-write word-span checks and avoided unchanged index rebuilds.
- Added optional bounded native ARM64 resource-walker recovery, lazy EOP submission batching and reduced presentation-output buffers. These experiments default off and require phone validation.
- Retained AE identity/signing continuity, author XHYN_PH, English, one resolution control, unified diagnostic ZIP, validated launch variables, save/cache guards, manual FPS choice and prior controls.

Prebuild: 68 Python and 40 Java tests, host native ordering/memory/filter/retry/ADPF tests, ARM64 mobile-module compilation, sanitizer checks and release lint passed with zero lint errors. All ten BC shader specializations passed SPIR-V validation. Final build/payload checks are recorded separately in the release report. No phone FPS improvement or stable-30 guarantee is claimed.

The previously excluded Stable 30 preset and configuration-profile/startup-recovery feature remain excluded. See [0.1.1 integration status](REBASE_011_STATUS.md) and [testing instructions](TEST_APK.md).

---

# TLOU-AE 0.2 — XHYN_PH

Release notes dated 8 November 2026 (Asia/Manila). Experimental Android ARM64 build.

- Renamed the app, launcher/menu headings and About to TLOU-AE; author XHYN_PH. Kept the package/signing identity for upgrades and retained upstream credits.
- Replaced competing resolution controls with one saved percentage slider and presets. Revalidate patches at every launch and use one immutable configuration for patches and native flags.
- Fixed mobile memory-budget recovery and accounting: include live allocations plus available headroom, preserve free RAM and respect driver limits.
- Added compatible display-mode requests, bounded recovery after focus/surface changes, optional supported sustained-performance hints, and transition-time clock/thermal/memory evidence.
- Corrected the FPS counter to exclude offscreen discards; added bounded foreground presentation percentiles, worst intervals, over-33.3-ms counts and long-gap counts.
- Combined diagnostics into one Create diagnostic report action with current/previous logs, running versus next-launch settings, applied patch metadata, device evidence and cache status.
- Made app-generated interface, errors and preparation messages English. The game retains its own language selection.
- Added validated launch variables with explicit override precedence and correct installer/preparation/runtime routing. Supports raw SNAFU installer errors, frame-overlap overrides and an explicit diagnostic TLOU_FIXES control; executable/byte guards remain.
- Added explained buffer/worker controls and optional Turnip memory/small-pass profiles. Performance benefits depend on the scene and driver.
- Added cache inspection and targeted disposable-cache clearing, guarded against a running game, save-transfer conflicts and redirected paths.
- Added Adreno-family guidance: Snapdragon 8 Gen 3 uses Adreno 750; an Adreno 8xx Gen8 package is a different target.
- Fixed three older full-lint errors and updated language/diagnostic regression checks.

The requested Stable 30 FPS preset and configuration-profile/startup-recovery feature were excluded. Existing manual 30/60 FPS selection and save backup/import remain.

Prebuild checks: 43 Python tests, 26 Java tests, host contracts, new memory/benchmark checks, sanitizer checks and full release lint passed with zero errors. Full native build, signature/identity/payload verification and post-build checks accompany the release artifacts.

No measured stable-30-FPS guarantee or new shader-math/workgroup optimization is claimed. Fresh target-phone results are needed to select further GPU changes. See [refinement status](REFINEMENT_STATUS.md) and [testing instructions](TEST_APK.md).

---

# TLOU AE Test — 0.1-ae-test

Built 2026-11-07 for Android ARM64. This is a test build for performance
investigation on Snapdragon 8 Gen 3; 60 FPS has not been demonstrated.

- Integrated the upstream Android runtime, launcher and unified
  scene/depth/HUD/aim resolution implementation into this fork.
- Added saved 10–100% render scaling with displayed rounded dimensions.
  Custom scaling supports game 01.00; 01.11 requires Original resolution.
- Added independent frame overlap (Off/1/2) and pipeline draining
  (Conservative/Nested compute buffers) controls.
- Exposed individual CPU-overhead, renderer-buffer reuse, background shader
  compilation, pipeline-prefetch, compute-cache, copy-batch and draw-statistics
  controls.
- Added optional warm-texture retention, mobile RAM-pressure budgeting,
  Android performance hints and thermal reporting.
- Added optional stencil/decal correction and reduced rain opacity.
- Added Normal/CPU-phase/GPU-pass diagnostics and settings/in-game log ZIP
  export with selected performance settings.
- Fixed fault-buffer initialization/bounds handling, DMA shader-cache
  compatibility and texture-cleanup budgeting.
- Fixed reset refresh, conflicting resolution overrides and bounded log export.
- Added guarded ARM64 PGO build/profile workflows. This APK uses LTO; PGO is off.
- Packaged a separately signed `com.xhyn.tlou.ae.test` application labeled
  **TLOU AE Test**, which installs alongside the upstream application.

Experimental controls default to Off. Settings changes require a game restart.
Existing upstream license and attribution notices remain in the source.

Validation: 42 Python tests, 21 Java tests, host native contract checks, release
vital lint, complete ARM64 compilation/linking, signing, alignment and packaged
native/JNI/installer/driver checks passed. The packaged native outputs and
preparation scripts were verified against the current build/checkout.

Phone gameplay, visual correctness, sustained performance and thermal behavior
remain unverified. AOT recompilation, automatic dynamic resolution and engine
shadow caching are not implemented. See [implementation status](IMPLEMENTATION_STATUS.md)
and [testing instructions](TEST_APK.md).

APK: `TLOU-AE-test-arm64.apk`, 45,000,120 bytes.
SHA-256: `6e72eed0d97daee4416ac89eff3e05c1269a93bd5664f8ca233a943253cf57fc`.
