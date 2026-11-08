# TLOU-AE 0.3 — Android Edition

Maintained by **XHYN_PH**. Based directly on the verified Android **v0.1.1** source, with the previous AE changes and the new performance/compatibility work applied on top.

[Download and release assets](https://github.com/XHYN-PH/TLOU-AE/releases/tag/v0.3-ae-test.1) · [Full AE changelog](android/CHANGELOG_AE.md) · [Integration status](android/REBASE_011_STATUS.md)

## Included in this testing release

- Unified English interface, AE branding and one diagnostic-report action.
- One render-resolution control, with guarded 01.00 custom scaling and verified 01.11 presets.
- Targeted readback protection, required-sequence copy waits, reduced profiler overhead and thread-memory cleanup.
- Native Android performance hints, correlated foreground diagnostics and independent display-refresh choices.
- Auto/Off/Force BC texture fallback, with native sampling retained where supported.
- Updated pinned Turnip R2/V39 bundles and preserved custom driver choices.
- Optional native-walker recovery, lazy EOP submission batching and reduced presentation buffers. These experiments default off.
- Previous AE controls, launch variables, save/cache tools and compatible app-signing identity retained.

Pre/post-build validation passed 68 Python and 40 Java tests, native/sanitizer checks, BC shader validation and final signed APK/payload checks. GitHub source and loader checks passed. Gameplay, visual correctness, sustained FPS and the background/return transition still require target-phone testing.

Install over the earlier compatible AE app without uninstalling. Use **Create diagnostic report** after reproducing a problem. See the [installation guide](docs/android/INSTALL.md) and [troubleshooting guide](docs/android/TROUBLESHOOTING.md).

[Earlier AE changelogs](android/CHANGELOG_AE.md) · [Historical upstream 0.1.1 notes](docs/UPSTREAM_RELEASE_NOTES.md)
