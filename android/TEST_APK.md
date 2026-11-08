# Testing TLOU-AE 0.3

Author: **XHYN_PH**. Install over the previous **TLOU AE Test** app: the package and signing identity are retained. Its launcher name becomes **TLOU-AE**. Keep saves backed up with the existing save export function.

1. Keep the same scene and driver for the first comparison. Select **30 FPS** manually when testing the 30 FPS goal. No Stable 30 preset was added.
2. Use the single render-resolution slider or its 33/50/67/100% presets. Game **01.00** supports custom percentages. Verified **01.11** executables support **33/50/67/100%** presets; intermediate custom percentages are rejected. Close the game and press Play to apply; reopening Settings does not restart it.
3. Initially leave new experiments at their defaults. Existing experiments remain saved: check them before comparing. Overlap, draining, driver profiles and worker counts can increase or decrease speed. Change one at a time and restart.
4. Reproduce the outdoor slowdown, an indoor control and the notification/pop-up transition. Use **Create diagnostic report** in Settings or the game menu. No CPU/GPU diagnostic mode selection is needed.
5. The ZIP separates the running configuration (`tlou/active-settings.json`) from next-launch choices (`performance-settings.json`). It includes display events, device samples, validated patch information and current/previous logs. Percentiles measure surface submissions, not GPU execution time. Long gaps remain counted; background intervals are excluded.
6. **Inspect caches and warmup** reports sizes and the latest shader backlog. Clearing caches requires closing the game and can increase first-run stutter. It does not reset saves or game content.

## Advanced launch variables

Open Settings → Advanced launch variables. Enter one `NAME=VALUE` per line, then **Save launch variables**. Blank lines and comments beginning with `#` are allowed. Malformed entries and duplicate names are rejected. Overrides take precedence over matching normal controls until cleared.

```text
SNAFU_RAW_ERROR_MESSAGES=1
GOW3_FRAMES_IN_FLIGHT=0
```

The first preserves raw PKG-extractor error chains; the second disables runtime overlap. They belong to different processes and are routed accordingly. Neither translates messages or guarantees higher FPS.

`TLOU_FIXES=none` is a diagnostic control disabling Android's base stability fixes during preparation. It can restore crashes/hangs. Graphics/resolution choices remain separate, and executable/original-byte guards stay enforced. Clear it or use `TLOU_FIXES=default` to restore base fixes.

Managed library paths, cache locations, resolution plumbing and known unsafe global no-wait/readback modes cannot be overridden here. Unknown runtime names can be passed through, but only implemented variables have an effect. Profiling/debug flags can slow the game. Credential-like values are redacted from reports.

For Snapdragon 8 Gen 3 / Adreno 750, compare drivers explicitly compatible with Adreno 7xx. The bundled Gen8 V38 package is labeled for Adreno **8xx**, not Snapdragon **8 Gen 3**. No universal fastest driver is claimed.

Report the diagnostic ZIP, exact scene and approximate time of the notification/pop-up transition. No measured 30 FPS guarantee, AOT backend or automatic dynamic resolution is included.

## New performance comparisons on the 0.1.1 base

- **BC texture GPU fallback:** Auto is the default. Off uses native BC support and stops with a clear message if the driver lacks it. Force decodes supported formats too; this is diagnostic and can add GPU work and 2–8× decoded texture storage. Use Auto for the first Adreno 750 comparison.
- **Display refresh policy:** choose Match, System or a supported 60/90/120 Hz mode. The game FPS cap stays independent. Android may override the request; unsupported rates fall back and Android 11 FIFO safeguards remain. Higher refresh can consume more power.
- **Recover native resource walkers:** experimental, off by default. Compare loading, scene transitions and visual correctness as well as frame time.
- **Batch EOP GPU submissions:** experimental, off by default. Compare actual submits per flip and host-copy waits; close/restart if a scene stalls or physics/rendering changes.
- **Presentation output size:** Physical display is the default. Follow render size reduces final output buffers while Android scales the completed picture. It does not change the render slider or touch-coordinate space, and may soften the image.
- **Android performance hints:** the single native hint session now handles late thread enrollment and focus transitions. The diagnostic ZIP correlates Java/native focus with monotonic timestamps, clocks, waits and submissions. A brief resume boost still needs device evidence; no cause is assumed.

Compare each change over several minutes in the same outdoor scene, then reproduce the brief background/return transition. Do not judge a setting by its first seconds of FPS.
