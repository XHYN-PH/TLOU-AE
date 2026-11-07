package ro.cobrabm.fexdroid;

import android.content.Context;
import java.util.LinkedHashMap;
import java.util.Map;

/** Values are passed as environment entries, never interpreted as shell code. */
final class LaunchOverrides {
    static Map<String,String> parse(String text) {
        Map<String,String> result=new LinkedHashMap<>();int line=0;
        if(text.length()>16384)throw new IllegalArgumentException("Launch variables exceed 16 KiB");
        for(String raw:text.split("\\n",-1)){
            line++;String value=raw.trim();if(value.isEmpty() || value.startsWith("#"))continue;
            int split=value.indexOf('=');
            if(split<1)throw new IllegalArgumentException("Line "+line+": use NAME=VALUE");
            String key=value.substring(0,split).trim(),setting=value.substring(split+1);
            if(!key.matches("[A-Za-z_][A-Za-z0-9_]*") || setting.indexOf('\0')>=0)
                throw new IllegalArgumentException("Line "+line+": invalid variable name or value");
            if(result.containsKey(key))throw new IllegalArgumentException("Duplicate variable: "+key);
            if(key.startsWith("LD_") || key.startsWith("DYLD_") || key.startsWith("FEX_APP_") ||
               key.startsWith("XDG_") || key.startsWith("TLOU_EXPECT_") ||
               java.util.Set.of("HOME","PATH","TMPDIR","LLVM_PROFILE_FILE","GOW3_PRESENT_SOCKET",
                 "GOW3_PAD_FILE","GOW3_AUDIO_FIFO","GOW3_GPU_USER_DIR","GOW3_GPU_CACHE_DIR",
                 "GOW3_VULKAN_DRIVER","GOW3_SCREEN_W","GOW3_SCREEN_H","GOW3_MOVIE_SIZE",
                 "GOW3_UPSCALE_PRESET","GOW3_RENDER_RES","GOW3_OUTPUT_RES","GOW3_UPSCALER","TLOU_UNIFIED_GUEST_UI","TLOU_SPATIAL_SCENE").contains(key))
                throw new IllegalArgumentException(key+" is managed by the app; use its setting");
            if((key.equals("GOW3_FEWER_DRAINS") || key.equals("GOW3_SUBMIT_NO_WAIT")) && !setting.equals("0"))
                throw new IllegalArgumentException(key+" is unavailable because it can corrupt rendering/physics");
            if(key.equals("GOW3_READBACKS") && !setting.equals("1"))throw new IllegalArgumentException("GOW3_READBACKS must remain 1 for this port");
            if(key.equals("GOW3_FRAMES_IN_FLIGHT") && !setting.matches("[012]"))throw new IllegalArgumentException("Frame overlap must be 0, 1 or 2");
            if(key.equals("GOW3_FPS_LIMIT") && !setting.equals("30") && !setting.equals("60"))throw new IllegalArgumentException("FPS limit must be 30 or 60 for this game");
            if(key.equals("TLOU_FIXES") && !setting.equals("none") && !setting.equals("default"))throw new IllegalArgumentException("TLOU_FIXES must be default or none");
            result.put(key,setting);
        }
        return result;
    }
    static Map<String,String> saved(Context c){return parse(PortSettings.prefs(c).getString("launch_overrides",""));}
    static boolean toolVariable(String key){return key.startsWith("SNAFU_") || key.startsWith("RUST_") || key.equals("TLOU_FIXES");}
    static void apply(Context c,Map<String,String> environment,boolean tools){
        saved(c).forEach((key,value)->{if(toolVariable(key)==tools){
            if(key.equals("GOW3_GPU_PROFILE") && value.equals("0"))environment.remove(key);
            else environment.put(key,value);
        }});
    }
    static Map<String,String> report(Context c){
        Map<String,String> result=new LinkedHashMap<>();
        saved(c).forEach((k,v)->result.put(k,k.matches("(?i).*(TOKEN|PASSWORD|SECRET|KEY|CREDENTIAL).*" )?"[redacted]":v));
        return result;
    }
}
