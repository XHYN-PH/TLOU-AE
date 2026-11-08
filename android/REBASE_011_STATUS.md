# TLOU-AE 0.3: Android 0.1.1 integration

Author **XHYN_PH**. The branch starts directly at `7256020d485dabbd3a3250d1dd12506b23455c45`, the successful Android 0.1.1 source build. Previous AE changes were reapplied as an overlay, followed by the selected improvements. The upstream release tag itself points to the older default/PC branch snapshot and was not used as the Android baseline.

| Selected item | Implementation and limits |
| --- | --- |
| 1. 0.1.1 base | Native/runtime/preparer/UI fixes integrated; AE package/signing, branding, English, save/cache tools, one resolution control, launch snapshot and overrides retained. Verified 01.11 gets 33/50/67/100% presets, movie repair and retail-memory reporting. Intermediate 01.11 scales are rejected. |
| 2. Targeted protection | Blanket asynchronous protection removed; explicit EOP/physics and full geometry readbacks retained. |
| 3. Copy waits | Wait for the required completion sequence, wake on completion, and help execute copies when parallel workers are disabled. |
| 4. Profiler cost | Gate serializing drain clocks behind actual profiling; trim routine draw statistics by default while retaining the saved control. |
| 5. Memory cleanup | Completed Android worker host stacks, FEX guest stacks and full 16 KiB TLS mappings are reclaimed. |
| 6. Resume/hints | One native frame-deadline hint session replaces the old CPU-time path. Native GPU thread enrollment, foreground reset, monotonic event correlation, existing clocks/thermal/wait evidence and actual Vulkan submits per flip are included. This does not establish the cause of the user's resume boost. |
| 7. Display | Independent Match/System/60/90/120 Hz requests. Only advertised same-resolution modes are used; Android may override them. Unsupported choices fall back; Android 11 FIFO safeguards remain. |
| 8. Drivers | Checksum-pinned Adreno 6xx/7xx R2 and Adreno 8xx V39, with original custom selections preserved. V39 Gen8 is not an Adreno 750 driver. |
| 9. BC decoding | Universal renderer adaptation with Auto/Off/Force. Auto retains supported native sampling; Off rejects unsupported driver formats clearly; Force is diagnostic. Added sRGB and decoded-target capability checks. Memory grows with actual decoded allocations; 2–8× texture storage is possible. |
| 10. Pending writes | Exact word-span bloom operations and no-op retirement avoidance, with randomized equivalence tests. Existing AE ring batching/indexing remains. |
| 11. Native walker recovery | Optional bounded retry/backoff after native ARM64 resource-walker faults; safe interpreter fallback. Off by default, preserving permanent fallback behavior until selected. |
| 12. Output bandwidth | Optional presentation buffers following render size, preserving physical aspect and a supported nonzero minimum. Android scales the completed picture. Physical output is the default; one guest-render slider remains. |
| 13. Submission batching | Optional lazy EOP/RELEASE_MEM path on both event paths; immediate submission is the default. Existing guest-stall progress and readbacks remain. Must validate device correctness and actual submission/wait gains. |

Experimental options are included for controlled phone comparisons, not silently enabled as universal optimizations. The previous exclusions still apply: no Stable 30 preset and no configuration-profile/startup-recovery feature. Game files and upstream license/credit notices are retained separately from app branding; no game data is bundled.

## Source and test evidence

- Android base: https://github.com/erickmendes22/tlou-remastered-android/tree/7256020d485dabbd3a3250d1dd12506b23455c45
- BC source: https://github.com/pavelgajdo/tlou-remastered-android-universal/commit/90731d11e41b60b0b418a4a9beef2950f3cc744b
- Native ADPF reference: `344456eec0e014f001d904139b4604a48dcb9d86`; pending-filter reference: `6e6db809dfaa843b6f8a71333f8fa66f722b0d6c`; SRT reference: `1d29c6b166cbdbdcad733d7224aebfdd918b7b4b`; EOP candidate: `a70e9c1b07eabac98fb00b055b35c0b26e74bf61`. Only relevant changes were adapted, not entire older experimental branch snapshots.
- PT4Droid and PC source were rechecked; their tips were unchanged from the prior AE review. Existing transferable controls are retained rather than advertised as new.

Prebuild passed 68 Python tests, 40 Java tests, full release lint (zero errors), native ordering/memory/8-million pending-filter operations/1-million retirement queries/retry/hint-session contracts, ARM64 mobile-module compilation and sanitizer checks. Ten BC compute specializations passed SPIR-V validation. Final native and signed APK results are in `release-build.json`. Host checks do not validate real Adreno performance, frame presentation or visual correctness. A new target-phone diagnostic report is needed to assess stable 30 FPS and the resume transition.
