# Testing TLOU-AE 0.2

Author: **XHYN_PH**. Install over the previous **TLOU AE Test** app: the package and signing identity are retained. Its launcher name becomes **TLOU-AE**. Keep saves backed up with the existing save export function.

1. Keep the same scene and driver for the first comparison. Select **30 FPS** manually when testing the 30 FPS goal. No Stable 30 preset was added.
2. Use the single render-resolution slider or its 33/50/67/100% presets. Scaling below 100% supports verified game **01.00** only. Use 100% for **01.11**. Close the game and press Play to apply; reopening Settings does not restart it.
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
