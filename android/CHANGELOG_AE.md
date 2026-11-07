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
