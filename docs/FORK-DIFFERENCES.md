# What TLOU-AE adds

**TLOU-AE means The Last of Us Remastered — Android Edition.** Maintained by **XHYN_PH**, version 0.3 starts directly from the verified Android 0.1.1 source and reapplies the earlier AE changes before adding the new work.

| Area | Android foundation | TLOU-AE 0.3 |
| --- | --- | --- |
| Runtime | FEX game CPU translation, native Android runtime and Vulkan renderer | Retains that architecture and integrates the 0.1.1 fixes |
| Installation | Extracted game folders, PKG preparation and game updates | Retains guarded preparation, update persistence and save handling |
| Interface | Android launcher, controls and menus | TLOU-AE branding, XHYN_PH author credit and English UI/errors |
| Resolution | Guarded unified scene/HUD/aim patches | One percentage control; verified 01.00 custom scales and 01.11 presets |
| Performance | Renderer, readback, memory and synchronization work | Explained controls, validated launch variables, memory-budget recovery and scheduling/presentation work |
| Textures | Native driver texture sampling | Auto/Off/Force BC fallback adapted from the universal fork |
| Diagnostics | Launcher/runtime logs | One ZIP with current/previous logs, running versus next-launch choices, device evidence and presentation statistics |
| Experiments | Version-specific renderer options | Switchable native-walker recovery, EOP batching and reduced output buffers, default off |
| Drivers | System/imported Vulkan drivers and bundled Turnip | Pinned R2/V39 bundles, family guidance and preserved custom selections |

Base functionality and upstream fixes remain credited to their authors. Added options are comparison tools; they do not establish a universal FPS gain. Forced BC decoding can increase GPU work and texture memory use.

The AE test package installs separately from the upstream app. It updates earlier compatible AE builds using the same package/signing identity; the two projects do not automatically share app-private saves or settings. Use save export/import when moving between them.

The Stable 30 preset and configuration-profile/startup-recovery feature were excluded from this work. Manual 30/60 FPS selection and save backup/import remain available.

[Current implementation status](../android/REBASE_011_STATUS.md) · [Changelog](../android/CHANGELOG_AE.md) · [Original Android README](../README_UPSTREAM.md)
