package ro.cobrabm.fexdroid;

import java.util.Locale;

/** Translate native faults into gameplay-friendly guidance. No raw logs in UI. */
final class GameFailureMessage {
    final String code,title,description,action;
    private GameFailureMessage(String code,String title,String description,String action) {
        this.code=code;this.title=title;this.description=description;this.action=action;
    }
    static GameFailureMessage classify(String reason,String log) {
        String source=((reason==null?"":reason)+"\n"+(log==null?"":log))
            .toLowerCase(Locale.ROOT);
        if(source.contains("bc textures are unsupported") || source.contains("decoded bc texture format")) {
            return new GameFailureMessage("TLOU-BC-05","TEXTURE FORMAT UNAVAILABLE",
                "The selected driver cannot use the requested BC texture path.",
                "Select Auto in BC texture GPU fallback, or choose a compatible Vulkan driver, then restart.");
        }
        if(source.contains("device lost during submit") ||
            source.contains("vk_error_device_lost") ||
            source.contains("device lost")) {
            return new GameFailureMessage("TLOU-GPU-01","GAME INTERRUPTED",
                "The graphics device stopped responding. This session cannot continue.",
                "Restart the game. If this repeats, select another compatible Vulkan driver in Settings.");
        }
        if(source.contains("job system has been inactive") ||
           source.contains("jobarray waiting on counter")) {
            return new GameFailureMessage("TLOU-JOB-02","GAME STOPPED RESPONDING",
                "An internal game task stopped responding and the session ended.",
                "Restart and load your last save. The diagnostic report includes the failure details.");
        }
        if(source.contains("libvulkan_freedreno.so") &&
           (source.contains("host fault") || source.contains("signal 11"))) {
            return new GameFailureMessage("TLOU-DRV-03","GRAPHICS DRIVER FAILURE",
                "The Vulkan driver encountered a fault during gameplay.",
                "Restart the game. If this repeats, choose another compatible Turnip version in Settings.");
        }
        if(source.contains("prepared image missing")||source.contains("boot-linked.bin")) {
            return new GameFailureMessage("TLOU-DATA-04","GAME FILES UNAVAILABLE",
                "Required prepared game files could not be found.",
                "Return to the launcher and select the game folder to prepare it again.");
        }
        return new GameFailureMessage("TLOU-RUN-00","CANNOT CONTINUE",
            "The game encountered a problem and this session ended.",
            "Return to the launcher and try again. Use Create diagnostic report to share the details.");
    }
}
