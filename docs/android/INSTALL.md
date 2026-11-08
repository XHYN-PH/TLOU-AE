# Install TLOU-AE

TLOU-AE is **The Last of Us Remastered — Android Edition**, maintained by **XHYN_PH**.

## Requirements

- Android 11 or newer and a 64-bit ARM Android environment.
- A compatible Vulkan GPU and driver.
- Your own compatible PS4 The Last of Us Remastered game files, with a recognized executable.
- Space for extraction, preparation and caches.

## First installation

1. Download the APK from the [AE release page](https://github.com/XHYN-PH/TLOU-AE/releases/tag/v0.3-ae-test.1).
2. Install and open TLOU-AE. Grant the file access it requests.
3. Choose **Install from PKG** or select the extracted game folder containing `eboot.bin` and `sce_sys/param.sfo`.
4. Let preparation finish, then press **Play**.

Use the launcher's game-update controls to apply a compatible update PKG or an extracted update folder. A game update and an APK update are separate operations. Keep the selected base/update folders accessible; preparation may need them again after an app upgrade.

## Update an existing AE installation

Install the newer compatible AE APK over the existing **TLOU AE Test / TLOU-AE** app. The package and signing identity remain compatible with the earlier AE test releases. Uninstalling or clearing app data removes app-private data, so export saves first if either is necessary.

The upstream Android app uses a different package. Use save export/import when transferring between it and AE.

## Start with a consistent comparison

- Use **BC texture GPU fallback → Auto**.
- Leave the new experimental native-walker, EOP-batching and reduced-output options off for the first run. Earlier saved settings are preserved, so review them after an upgrade.
- Use a driver intended for your GPU family. Snapdragon 8 Gen 3 has Adreno 750; its relevant Turnip family is Adreno 7xx.
- Adjust the single render-resolution control in Settings. Verified 01.00 accepts custom percentages; verified 01.11 accepts 33/50/67/100% presets.

Close the game and press Play again after changing render patches, the driver or native performance settings. Compare the same scene for several minutes and change one setting at a time.

[Troubleshooting](TROUBLESHOOTING.md) · [Detailed testing guide](../../android/TEST_APK.md)
