> Historical AE 0.2 status. Current 0.1.1-based integration: [AE 0.3 status](REBASE_011_STATUS.md).

# TLOU-AE 0.2 refinement status

Author: **XHYN_PH**. Package/signing identity is retained for upgrading the previous TLOU AE Test installation. App label, launcher/menu headings and About identify **TLOU-AE**. Upstream source/license credits remain.

| Item | Result |
| --- | --- |
| 1. Unified diagnostics | One Create diagnostic report action exports current/previous logs, applied patches, running versus next-launch settings, device/thermal/memory/display evidence, cache status and presentation statistics. Bounded CPU sampling is automatic; expensive full GPU profiling is off by default. |
| 2. Resolution | One slider/preset control replaces competing selectors. Existing choices migrate. Every launch refreshes guarded patches, including direct Android activity recreation. An immutable snapshot supplies patches and runtime flags. |
| 3. Memory budget | Recompute the ceiling as conditions change. Count existing allocations plus available headroom, preserve a system reserve and respect the driver cap. Recover from a small startup allowance without growing under real pressure. |
| 4/13. Display pacing | Request compatible advertised modes without changing physical resolution; distinguish physical/app-visible refresh, reapply after focus/surface changes and bound recovery attempts. Avoid requesting a 30 Hz FIFO display on API 30 when a compatible higher mode exists. Sample clocks/memory around focus transitions. |
| 5. FPS reporting | Count surface submissions rather than offscreen-discarded frames. Reset HUD intervals across lost focus. This measures submissions, not physical scanout or GPU execution time. |
| 6. Stable 30 preset | **Excluded at the user's request.** Existing manual 30/60 FPS selection remains; no forced FPS migration. |
| 7. Explanations | Explain CPU/GPU effects, memory/power/latency costs, visual changes and restart requirements. |
| 8. Extra controls | Add presentation buffer count and draw-preparation worker count; preserve the previous Android defaults. |
| 9. Drivers | User evidence identifies Adreno 750, a startup exit 139 with Gen8 V38 and a longer run with v27 r2. Show family guidance and record driver identity. No cloud test establishes a fastest driver. |
| 10. GPU bottlenecks | Correct a demonstrated budget/eviction issue and expose measured scheduling/memory/driver comparisons. Preserve geometry/readback correctness. No unmeasured shader-math/workgroup rewrite is claimed; further selection requires phone profiles. |
| 11. English | App UI, errors, preparation messages and summaries use English despite a stale saved locale. Game language stays the player's in-game choice. |
| 12. Variables | Validate NAME=VALUE without shell interpretation. Show/redact overrides, route installer/preparation versus renderer variables and apply overrides after normal controls. Add explicit Android TLOU_FIXES semantics. Managed paths/resolution and known unsafe global synchronization modes stay guarded. |
| 14. Sustained performance | Optional supported Android power hint; no second limiter or thermal bypass. |
| 15. Turnip profiles | Driver-default/system-memory/tile-memory/small-pass hints adapted from PT4Droid, applied before driver initialization. Driver support/performance varies. |
| 16. Benchmark | Bounded recent foreground intervals include p50/p95/p99, worst, over-33.3-ms intervals and long gaps. Long gaps remain counted; they are not silently labeled loading. Pausing for report export retains the last sample history. |
| 17. Caches | Inspect sizes and shader backlog. Clear disposable renderer or FEX JIT cache only with the game stopped and its transfer lock available. Reject redirected roots; preserve saves, drivers and game content. |
| 18. Profiles/recovery | **Excluded at the user's request.** No configuration-profile import/export or automatic rollback. Existing save backup/import remains. |

The supplied vivo V2425A / Adreno 750 log contains a 36% override alongside the old 540p preference, repeated eviction near 4.3 GiB usage against a roughly 2.2 GiB critical threshold, substantial GPU waits and an Android low-memory exit. These identify things to correct/measure; they do not prove one cause for every fluctuation.

Before APK packaging, 43 Python tests, 26 Java tests, native contracts including new memory/benchmark checks, sanitizer bounds checks, Android ARM64 module compilation and full release lint passed with zero lint errors. Three older lint errors and an assertion expecting Portuguese were corrected. Existing legacy-storage/SDK warnings remain; checks were not disabled to obtain a pass.

Full native and final APK results accompany the release artifacts. Phone gameplay, sustained minimum FPS, thermals and driver comparisons still require the device. Stable 30 FPS in every scene is not established by building successfully.

References: PT4Droid `2f43a90e3f910be4d59804f3b46a5132cc951d8e`, TLOU PC `212fb04aaf81613326efcb9b9985326c612799cc`, and the previously reviewed Uncharted/Bloodborne/GoW3/SotC sources. Fast condition-variable code was already present and is not presented as a new optimization.
