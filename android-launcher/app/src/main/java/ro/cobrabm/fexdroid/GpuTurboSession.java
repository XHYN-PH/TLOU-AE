package ro.cobrabm.fexdroid;

import android.content.Context;
import java.util.Locale;

/**
 * Non-root Adreno Turbo, matching Bachata S4+ and libadrenotools:
 * /dev/kgsl-3d0 -> IOCTL_KGSL_SETPROPERTY(KGSL_PROP_PWRCTRL).
 * Restores normal scaling on session end. Thermal limits still apply.
 */
final class GpuTurboSession {
    private boolean active;

    synchronized boolean start(Context context) {
        if(active || !PortSettings.prefs(context).getBoolean("gpu_turbo",false))return active;
        String gpu;
        try { gpu=BuiltinDrivers.renderer().toLowerCase(Locale.ROOT); }
        catch(Throwable error) { RunLog.write(context,"Adreno Turbo: GPU not detected"); return false; }
        if(!gpu.contains("adreno")) {
            RunLog.write(context,"Adreno Turbo skipped for non-Adreno GPU: "+gpu);
            return false;
        }
        try {
            active=Gow3Present.adrenoTurbo(true);
            RunLog.write(context,active?"Adreno Turbo enabled without root (KGSL PWRCTRL)":
                "Adreno Turbo unavailable: KGSL ioctl rejected; game continues normally");
        } catch(Throwable error) {
            active=false;RunLog.write(context,"Adreno Turbo not enabled: "+error);
        }
        return active;
    }
    synchronized void stop(Context context) {
        if(!active)return;
        boolean restored=Gow3Present.adrenoTurbo(false);
        RunLog.write(context,restored?"Adreno Turbo disabled; normal dynamic scaling restored":
            "Could not restore the KGSL policy; check device state");
        active=false;
    }
    synchronized boolean isActive() {return active;}
}
