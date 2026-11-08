<h1 align="center">TLOU-AE</h1>

<p align="center"><strong>The Last of Us Remastered — Android Edition</strong></p>

<p align="center">
  <a href="https://github.com/XHYN-PH/TLOU-AE/releases/download/v0.3-ae-test.1/TLOU-AE-v0.3-arm64.apk"><strong>Download APK</strong></a> ·
  <a href="https://github.com/XHYN-PH/TLOU-AE/releases/tag/v0.3-ae-test.1">What's new</a> ·
  <a href="https://t.me/xhynports">Telegram</a> ·
  <a href="https://youtube.com/@xhyn_ph">YouTube</a> ·
  <a href="https://www.playstation.com/en-us/games/the-last-of-us-remastered/">Official game page</a>
</p>

**AE means Android Edition.** Maintained by **XHYN_PH**, TLOU-AE brings The Last of Us Remastered to Android with touch/controller support, explained graphics and performance settings, and one shareable diagnostic report.

**Current version: 0.3-ae-test, based on Android 0.1.1.** This is a testing release. Performance work continues; stable 30 FPS across every scene has not been demonstrated.

**Bring your own compatible PS4 The Last of Us Remastered files.** The APK and repository do not include the game.

## How this fork differs from the original

TLOU-AE starts from [Erick Mendes / Daedalus Emulator's Android 0.1.1 source](https://github.com/erickmendes22/tlou-remastered-android/tree/7256020d485dabbd3a3250d1dd12506b23455c45), based on [ravencaio's PC port](https://github.com/ravencaio/tlou-remastered-pc). That foundation already provides game preparation, PKG installation, touch/controller input, Vulkan drivers and guarded game patches.

This edition adds:

- **TLOU-AE branding and English UI**, settings and app-generated errors.
- **One render-resolution control**, explained performance options and validated advanced launch variables.
- **One diagnostic ZIP** with current/previous logs, running settings, device evidence and frame-presentation statistics.
- **Memory, readback and scheduling work**, plus display-refresh choices and Android performance hints.
- **BC texture GPU fallback: Auto / Off / Force**, adapted from the [universal Android fork](https://github.com/pavelgajdo/tlou-remastered-android-universal).
- Optional native resource-walker recovery, submission batching and smaller presentation buffers for controlled performance comparisons.

More options do not guarantee higher FPS. The new experimental paths default off, and existing saved choices remain in place during upgrades.

[Compare the base and AE features](docs/FORK-DIFFERENCES.md) · [Full changelog](android/CHANGELOG_AE.md) · [Implementation and validation status](android/REBASE_011_STATUS.md)

## Get started

1. [Download the APK](https://github.com/XHYN-PH/TLOU-AE/releases/download/v0.3-ae-test.1/TLOU-AE-v0.3-arm64.apk) and install it on your phone.
2. Grant the requested file access, then choose your extracted game folder or use **Install from PKG**.
3. Wait for preparation to finish and press **Play**.
4. Open **Settings** to adjust resolution, performance options, controls and your Vulkan driver. Swipe right from the left edge during gameplay to open the game menu.

Updating? Install over your existing **TLOU AE Test / TLOU-AE** app. The package and signing identity are retained. Export your saves before uninstalling or clearing app data.

[Installation guide](docs/android/INSTALL.md) · [Testing guide](android/TEST_APK.md) · [Troubleshooting](docs/android/TROUBLESHOOTING.md)

## What you need

| Item | Requirement |
| --- | --- |
| Phone | Android 11 or newer, with a 64-bit ARM Android environment |
| Graphics | A compatible Vulkan GPU and driver; support varies by device |
| Game | Your own PS4 The Last of Us Remastered files with a recognized executable |
| Versions | Verified 01.00 and 01.11 executable profiles; a matching title ID alone is insufficient |
| Storage | Space for the extracted game, preparation and caches; PKG installation also needs extraction space |

Custom 10–100% render scaling supports verified **01.00**. Verified **01.11** supports the **33/50/67/100% presets**. Patches require matching executable hashes and original instruction bytes.

The game CPU code runs through **FEX translation**. The Android runtime and Vulkan renderer run as native ARM64 components. See [how this port works](docs/SOURCE.md).

## Find your settings

| What you want to change | Where to go |
| --- | --- |
| Render resolution, FPS limit and VSync | **Settings → Video and Display** |
| Frame overlap, workers, memory policy and performance hints | Performance controls inside **Video and Display** |
| Display refresh and presentation output size | Performance controls inside **Video and Display** |
| BC fallback | **BC texture GPU fallback** in performance controls |
| Launch variables | **Advanced launch variables** in performance controls |
| Shadows, SSAO, blur and other effect patches | **Patches** tab |
| Vulkan driver | **Settings → Vulkan Driver** |
| Touch controls and FPS display | **Settings → Controls and HUD**; use the in-game menu to edit button positions |
| Back up or restore saves | **Settings → Save Data** |
| Share a diagnostic report | **Settings → Create diagnostic report**, or the same action in the game menu/About |

Close and restart the game after changing render patches, the driver or native performance options. The report records running settings separately from choices saved for the next launch.

## Drivers and BC textures

| GPU family | Bundled driver |
| --- | --- |
| Adreno 6xx / 7xx | Turnip v27 R2 |
| Adreno 8xx | Turnip Gen8 V39 Patched |

**Snapdragon 8 Gen 3 uses Adreno 750.** Choose a driver compatible with Adreno 7xx. Imported drivers remain selectable; no driver is universally fastest.

For BC textures, start with **Auto**. It retains native compressed sampling where supported and decodes unsupported formats on the GPU. **Off** requires native BC support; **Force** is diagnostic and can increase GPU work and decoded texture storage. This fallback improves format compatibility, but does not establish complete Mali/Xclipse support or an Adreno speedup.

## Having a problem?

1. Reproduce the issue, then choose **Create diagnostic report** in Settings, the game menu or About.
2. Save the single ZIP. No separate debug launch or CPU/GPU logging mode is required.
3. Share it in [Telegram](https://t.me/xhynports) or [open an issue](https://github.com/XHYN-PH/TLOU-AE/issues).

Include your phone, Android version, game version, selected driver, settings and the scene or steps that triggered the issue. A screenshot or short video helps with visual problems. Review the ZIP before posting it publicly.

Shader compilation can cause first-visit stutter. Background/return FPS fluctuations are under investigation, and the 0.3 changes need confirmation on the affected phone. See [troubleshooting](docs/android/TROUBLESHOOTING.md).

## Development and checks

The current Android source is on [`android-0.1.1-ae`](https://github.com/XHYN-PH/TLOU-AE/tree/android-0.1.1-ae). See the [build guide](android/README.md), [release verification report](https://github.com/XHYN-PH/TLOU-AE/releases/download/v0.3-ae-test.1/BUILD_RESULT.json) and [integration status](android/REBASE_011_STATUS.md).

Version 0.3 passed 68 Python tests, 40 Java tests, native/sanitizer checks, BC shader validation and signed APK/payload checks. These checks do not measure phone gameplay, sustained FPS or visual correctness.

## Credits and license

- **XHYN_PH** for this edition's Android refinements, interface and releases.
- [Erick Mendes / Daedalus Emulator](https://github.com/erickmendes22/tlou-remastered-android) for the Android foundation.
- [ravencaio/tlou-remastered-pc](https://github.com/ravencaio/tlou-remastered-pc) for the PC reference.
- [pavelgajdo's universal fork](https://github.com/pavelgajdo/tlou-remastered-android-universal) for BC texture compatibility.
- [FEX-Emu](https://github.com/FEX-Emu/FEX), shadPS4, [Mesa / Turnip](https://gitlab.freedesktop.org/mesa/mesa) and [libadrenotools](https://github.com/bylaws/libadrenotools).
- Runtime foundations and contributions from BilalReloaded / bc160201983, FelipePrado0 / SpartanPort, SuperMedo and HayatoG. SDL3, FFmpeg, CPython, LibAtrac9 and other bundled components retain their licenses and credits.

[Original Android README](README_UPSTREAM.md) · [Historical PC documentation](docs/PC_PORT_REFERENCE.md) · [License](LICENSE)

Project code is licensed under **GPL-2.0-or-later**. Artwork and trademarks belong to their respective owners. This unofficial project is unaffiliated with Sony Interactive Entertainment or Naughty Dog.
