# Troubleshooting TLOU-AE

## Create one report

Use **Create diagnostic report** in Settings, the game menu or About. Save the ZIP and share it in [Telegram](https://t.me/xhynports).

There is no separate debug launch or CPU/GPU logging mode to choose. The report includes current/previous logs, device evidence, applied patch information and the running configuration separately from settings saved for the next launch.

Include your phone, Android version, game version, driver, settings, scene and reproduction steps. Review the report before posting it publicly.

## Low FPS or a brief boost after returning to the game

This remains under investigation. Version 0.3 adds correlated focus, clock, display, memory and renderer-wait evidence; it does not establish one cause for every slowdown.

1. Compare the same outdoor scene and an indoor control for several minutes.
2. Note approximately when you open notifications, switch away or return.
3. Create the report after reproducing the transition.

Try one setting at a time. More workers, overlap, forced BC decoding or a higher display refresh rate can increase power/memory use or reduce sustained performance. Reducing render resolution may help GPU pixel work, but CPU and synchronization costs can still dominate.

## Black or garbled textures

Start with **BC texture GPU fallback → Auto** and restart. Auto decodes only formats the selected driver cannot sample. Off requires native format support; Force also decodes supported formats and can cost more GPU work and memory.

If the problem persists, compare a compatible driver and include a screenshot plus the diagnostic ZIP. The BC fallback alone does not prove complete compatibility for every GPU.

## Resolution does not change

Close the game and press Play again; reopening Settings does not reapply executable patches to an already running game.

Verified **01.00** supports custom 10–100% scaling. Verified **01.11** supports **33/50/67/100% presets**. Unsupported intermediate scales or executable signatures are rejected. The report records the actual rounded scene size and validated patches.

**Presentation output size** changes final output buffers, while the render-resolution control changes guest rendering. They affect different stages.

## Startup or driver failure

- Keep the selected base game and update folders accessible.
- Use compatible, recognized executable versions. A title ID alone does not establish patch compatibility.
- Review saved launch variables and recent experimental settings, then compare the previous working configuration.
- Select a Vulkan driver for the correct GPU family and restart. Gen8 V39 is for Adreno 8xx; Snapdragon 8 Gen 3 uses Adreno 750.

If Play cannot start, create any available diagnostic report from the launcher. Avoid uninstalling before exporting saves.

[Installation](INSTALL.md) · [Feature comparison](../FORK-DIFFERENCES.md) · [Testing guide](../../android/TEST_APK.md)
