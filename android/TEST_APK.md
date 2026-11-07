# Testing TLOU AE Test

The development APK uses package `com.xhyn.tlou.ae.test` and the launcher name
**TLOU AE Test**. It installs alongside the upstream app. Keep the original
installation and saves. This is an experimental build, not a measured 60 FPS
release.

1. Install `TLOU-AE-test-arm64.apk`, grant its requested game-folder access,
   and select your existing game folder. App settings and private saves are
   separate. If needed, export saves from the original app and import that
   ZIP in the test app's Settings → Saves section.
2. Leave advanced experiments at their defaults and keep the FPS limit at
   **60**. Compare the same outdoor scene and an indoor scene after shaders
   have warmed up. Record FPS and any visual/physics problems.
3. For game **01.00**, enable **Use percentage scale** in Settings and compare
   **100%**, **50% (960×540)** and **33% (640×360)**. Fully stop and restart the
   game after each change. The supported range is **10–100%**; zero cannot
   render. For **01.11**, keep Original resolution: custom patches are guarded
   and intentionally unsupported.
4. If testing frame overlap, start with **1 frame**. Test pipeline draining
   separately. Change one option per run and restart the game; check aiming,
   geometry, physics, streaming, movies and save/load as well as FPS. Use
   **Reset advanced performance settings** to restore the defaults.
5. To capture evidence, select **Diagnostics → CPU phases**, restart, reproduce
   the slow scene briefly, then stop the game and use **Export logs ZIP** in
   Settings or the in-game menu. GPU-pass diagnostics are a separate optional
   capture and can lower FPS. Return to Normal for performance comparisons.

When reporting results, include the ZIP, exact phone model, game version,
scene, selected driver and resolution. The ZIP contains the launch settings;
there is no separate "Play with debug" button to find.

The included controls cover resolution, overlap/draining, existing renderer
experiments, optional mobile memory/ADPF policies and stencil/rain changes.
New AOT recompilation, live dynamic resolution and shadow caching are not
implemented. See [implementation status](IMPLEMENTATION_STATUS.md).
