# How TLOU-AE runs the game

TLOU-AE is an Android adaptation of the PS4 The Last of Us Remastered port. **AE means Android Edition.**

| Component | How it runs |
| --- | --- |
| Game CPU code | The game's x86-64 instructions run through FEX translation on ARM64 Android |
| Runtime | Native ARM64/Bionic components provide the required game system services |
| Graphics | The native ARM64 renderer translates PS4 graphics work into Vulkan commands |
| Presentation | Android hardware buffers and SurfaceControl deliver frames to the display |

FEX translation, renderer CPU work, Vulkan GPU execution, synchronization, memory pressure and presentation can each limit performance. Changing the resolution primarily reduces pixel work; it cannot remove every CPU or synchronization bottleneck. A successful build does not demonstrate sustained 30 or 60 FPS.

## Source lineage

- PC reference: [ravencaio/tlou-remastered-pc](https://github.com/ravencaio/tlou-remastered-pc).
- Android base: [Erick Mendes / Daedalus Emulator's verified 0.1.1 commit](https://github.com/erickmendes22/tlou-remastered-android/tree/7256020d485dabbd3a3250d1dd12506b23455c45).
- BC decoder reference: [pavelgajdo's universal fork](https://github.com/pavelgajdo/tlou-remastered-android-universal/commit/90731d11e41b60b0b418a4a9beef2950f3cc744b).
- AE 0.3 release source: [`a040cd08bac9f6fa5650e3263042dcd9ab4e5897`](https://github.com/XHYN-PH/TLOU-AE/commit/a040cd08bac9f6fa5650e3263042dcd9ab4e5897).
- Repository landing branch: [`main`](https://github.com/XHYN-PH/TLOU-AE/tree/main).
- Release-development branch: [`android-0.1.1-ae`](https://github.com/XHYN-PH/TLOU-AE/tree/android-0.1.1-ae).

The repository retains historical PC code and documentation. Android development and packaging use the [Android build guide](../android/README.md). Dependencies keep their licenses and credits; no game files are included.
