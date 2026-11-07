package ro.cobrabm.fexdroid;

import android.content.Context;
import android.os.Build;
import java.util.Locale;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Launch defaults plus independently persisted performance controls. */
final class PortSettings {
    static final String[] EFFECTS={"ssao","motion_blur","depth_of_field","bloom","sunlight_shadows","subsurface"};
    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("tlou",Context.MODE_PRIVATE);
    }

    static void setScale(Context context,int percent) {
        RenderScale.units(percent);
        prefs(context).edit().putBoolean("custom_scale",true).putInt("render_scale",percent)
            .putString("resolution","original").putString("render_path","guest").apply();
    }
    static void migrateResolution(Context context) {
        SharedPreferences p=prefs(context);
        if(p.getBoolean("resolution_ui_v2",false))return;
        String old=p.getString("resolution","720");
        int percent=p.getBoolean("custom_scale",false)?scale(context):
            "720".equals(old)?67:"540".equals(old)?50:"360".equals(old)?33:100;
        setScale(context,percent);
        p.edit().putBoolean("resolution_ui_v2",true).apply();
    }
    // Existing player-selected game, driver, patch and resolution preferences survive upgrades.
    // Old experiment preferences cannot silently reactivate discarded modes.
    static void retireTestPreferences(Context context) {
        SharedPreferences.Editor edit=prefs(context).edit();
        for(String key:new String[]{"memory_walk_once","ring_binding_cursor","draw_counter_trim",
            "compute_perm_memo","host_copy_batch","srt_hot_readbacks","fetch_no_block",
            "land_on_guest","static_min_512","stack_mapped_readback","inline_single_copy",
            "split_cs64","mapped_fault_readbacks","mapped_readbacks","gpu_profile",
            "prep_pool","cpu_trims","prefetch_pipelines","parallel_pipelines",
            "prep_workers","frames_ahead","descriptor_bulk","readback","sotc_precise_narrow",
            "compute_wait_fast","relaxed_skinning_fp","perf_diagnostics"}) edit.remove(key);
        String limit=prefs(context).getString("fps_limit","60");
        if(!"30".equals(limit) && !"60".equals(limit))edit.putString("fps_limit","60");
        edit.apply();
    }

    static File patchOptions(Context context) throws Exception {
        SharedPreferences p=prefs(context);
        JSONObject options=new JSONObject();
        // GPU-heavy proxy scaling kept the game native at 1080p and resolved
        // intermediate targets repeatedly. Direct 01.00 executable scaling
        // reduces ALL scene/post allocations; the existing Vulkan UI composer
        // expands the completed LDR background before drawing the HUD at 1080p.
        // "proxy" is the earlier fallback with separately allocated Vulkan
        // targets. The user has tested the direct-performance method at 720p
        // and original 1080p, NOT the proxy fallback in this build.
        // Never silently change the saved resolution.
        final String resolution=p.getString("resolution","original");
        final boolean proxy="proxy".equals(p.getString("render_path","guest"));
        options.put("resolution",proxy?"original":resolution);
        if(p.getBoolean("custom_scale",false)){options.put("resolution","original");options.put("resolution_scale",scale(context));}
        // Xiaomi 23049PCD8G reported a real HALT after 806 seconds. The
        // OnePlus CPH2691 reported only a "long job" warning and a later
        // SIGTERM on user exit; it is NOT evidence of a guest deadlock.
        // Preserve original wake predicates only for the confirmed Xiaomi SoC.
        options.put("job_sleep",!conservativeJobWorkers());
        options.put("fixes",LaunchOverrides.saved(context).getOrDefault("TLOU_FIXES","default"));
        for(String key:EFFECTS) options.put(key,p.getBoolean(key,false));
        File file=new File(context.getFilesDir(),"patch-options.json");
        Files.write(file.toPath(),options.toString().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    static boolean conservativeJobWorkers() {
        final String model=Build.MODEL==null?"":Build.MODEL.toUpperCase(Locale.ROOT);
        final String soc=Build.VERSION.SDK_INT>=31 && Build.SOC_MODEL!=null
            ?Build.SOC_MODEL.toUpperCase(Locale.ROOT):"";
        // Do not apply this device-specific workaround globally to all Adreno.
        return soc.contains("SM7475") || model.equals("23049PCD8G");
    }

    static void environment(Context context,Map<String,String> env) {
        SharedPreferences p=prefs(context);
        String resolution=p.getString("resolution","original");
        // Direct game mode: scene, gameplay HUD, crosshair and depth all
        // use the selected resolution. The game's full/half post targets
        // also follow this resolution, while the source movie decoding stays
        // untouched. No extra Vulkan 720p->1080p HUD composition is inserted.
        // "proxy" remains a separate optional compatibility method.
        final String scaledPreset=resolution.equals("720")?"1":
            resolution.equals("540")?"3":resolution.equals("360")?"4":null;
        final String renderPath=p.getString("render_path","guest");
        final boolean proxy="proxy".equals(renderPath);
        env.put("TLOU_EXPECT_SCENE_SIZE","1920x1080");
        env.put("GOW3_UPSCALER","off");
        env.put("GOW3_OUTPUT_RES","1920x1080");
        if(scaledPreset!=null) {
            final String actualSize=resolution.equals("720")?"1280x720":
                resolution.equals("540")?"960x540":"640x360";
            env.put("TLOU_EXPECT_HOST_SCENE",actualSize);
            if(proxy) {
                env.remove("GOW3_RENDER_RES");
                env.remove("TLOU_UNIFIED_GUEST_UI");
                env.remove("GOW3_MOVIE_SIZE");
                env.put("GOW3_UPSCALE_PRESET",scaledPreset);
                env.put("TLOU_SPATIAL_SCENE","1");
                env.put("TLOU_HOST_SCENE_MODE","1");
            } else {
                // Scene/HUD/aim are patched to the same actualSize in eboot.
                // Deliberately do not request any guest upscaler: that was
                // forcing a second 1920x1080 HUD attachment and mixing it
                // with 720p scene depth even with FSR temporal switched off.
                env.remove("GOW3_RENDER_RES");
                env.remove("GOW3_UPSCALE_PRESET");
                env.remove("TLOU_SPATIAL_SCENE");
                env.remove("TLOU_HOST_SCENE_MODE");
                env.put("TLOU_UNIFIED_GUEST_UI","1");
                env.put("TLOU_EXPECT_SCENE_SIZE",actualSize);
                // Video-only NV12 adaptation: decoder stays native 1080p,
                // while the movie's Y+UV sampling planes use the display's
                // 720p layout. This flag is ignored by every gameplay shader.
                env.put("GOW3_MOVIE_SIZE",actualSize);
            }
        } else {
            env.remove("GOW3_RENDER_RES");
            env.remove("TLOU_UNIFIED_GUEST_UI");
            env.remove("GOW3_MOVIE_SIZE");
            env.remove("TLOU_SPATIAL_SCENE");
            env.remove("TLOU_HOST_SCENE_MODE");
            env.remove("TLOU_EXPECT_HOST_SCENE");
            env.remove("GOW3_UPSCALE_PRESET");
        }

        if(p.getBoolean("custom_scale",false)) {
            int percent=scale(context);String size=RenderScale.size(percent);
            env.put("TLOU_EXPECT_SCENE_SIZE",size);
            env.remove("GOW3_UPSCALE_PRESET");env.remove("TLOU_SPATIAL_SCENE");env.remove("TLOU_HOST_SCENE_MODE");
            if(percent<100){
                env.put("TLOU_UNIFIED_GUEST_UI","1");env.put("TLOU_EXPECT_HOST_SCENE",size);env.put("GOW3_MOVIE_SIZE",size);
            }else{
                env.remove("TLOU_UNIFIED_GUEST_UI");env.remove("TLOU_EXPECT_HOST_SCENE");env.remove("GOW3_MOVIE_SIZE");
            }
        }

        // Exact flags observed in launch.log of TLOU-Android-Logs (32).zip.
        // Its preceding launch-previous.log used different v0.31 trial flags:
        // the more recent actual run had the three below OFF.
        env.put("TLOU_MEMORY_WALK_ONCE","1");
        env.put("TLOU_RING_BINDING_CURSOR","1");
        env.put("TLOU_DRAW_COUNTER_TRIM","0");
        env.put("GOW3_TLOU_COMPUTE_MEMO","0");
        env.put("GOW3_TLOU_HOST_COPY_BATCH","0");
        env.put("TLOU_SRT_HOT_READBACKS","1");
        env.put("TLOU_SRT_DEP_BATCH","0");
        env.put("GOW3_FETCH_NO_BLOCK","1");
        env.put("GOW3_LAND_ON_GUEST","1");
        env.put("GOW3_STATIC_MIN_BYTES","512");
        env.put("TLOU_STACK_MAPPED_READBACK","1");
        env.put("TLOU_INLINE_SINGLE_COPY","1");
        env.put("TLOU_SPLIT_CS64","1");
        env.put("TLOU_MAPPED_FAULT_READBACKS","1");
        env.put("TLOU_MAPPED_READBACKS","1");
        // Eight bounded CPU samples per second per draw/dispatch role. The latest
        // gameplay log has 95% command and 84% draw CPU, but not the phase
        // breakdown needed to avoid another speculative APK. This sampler
        // uses CPU-thread clocks only and remains disableable by an env override.
        // The native Android default is ON unless explicitly disabled.
        env.put("TLOU_CPU_PHASE_SAMPLES","1");
        // Leave profiling truly disabled. Some native switches check presence,
        // so setting "0" instead of removing is incorrect.
        for(String name:new String[]{"GOW3_GPU_PROFILE","GOW3_FRAME_STATS","GOW3_PERF_DIAG"})
            env.remove(name);

        env.put("GOW3_READBACKS","1"); // Precise=2 caused artifacts; never resurrect.
        env.put("GOW3_EOP_READBACK","1"); // Selective readbacks, not Precise/Readback 2.
        // Readback 1 stays on at all levels. Every exact physics EOP result
        // already calls DownloadRangesAsync(..., protect=true, yield_to_cpu=true),
        // which read-protects its own pages without a global switch.
        // Protecting ALL other pending readbacks caused many unrelated page
        // faults and a sharp performance regression on Adreno 830.
        // Only shader-independent Full geometry synchronization is retained:
        // both user-provided 720p and native 1080p logs used Full, without the
        // previously suspected performance regression. No more Fast/Balanced
        // profiles which can return character deformation.
        env.put("TLOU_AUTO_SMALL_CS_READBACK","2");
        env.put("GOW3_ASYNC_READBACK","1");
        // Coverage audit was completed in earlier device logs. Do not run
        // its atomic identity tracking on the hot compute-binding path during
        // normal gameplay; an explicit native diagnostic can still enable it.
        env.remove("TLOU_PHYSICS_COVERAGE");
        env.put("GOW3_ASYNC_SHADERS","1");
        env.put("GOW3_FRAMES_AHEAD","0");
        env.put("TLOU_DESCRIPTOR_BULK","0");
        env.put("GOW3_PREP_POOL","1");
        env.put("GOW3_CPU_TRIMS","1");
        env.put("GOW3_PREFETCH_PIPELINES","1");
        env.put("GOW3_PARALLEL_PIPELINES","1");
        env.put("GOW3_PREP_WORKERS","0");
        env.put("GOW3_DEFERRED_READBACK","0");
        env.put("GOW3_STALE_READBACK","0");

        // Only presentation controls remain user-adjustable.
        env.put("GOW3_VSYNC",p.getBoolean("vsync",true)?"1":"0");
        env.put("GOW3_FPS_LIMIT",fpsLimit(context));
        String overlap=p.getString("perf_overlap","0");
        env.put("GOW3_FRAMES_IN_FLIGHT",overlap.matches("[012]")?overlap:"0");
        // Never expose the known unsafe global drain/no-wait paths.
        env.put("GOW3_FEWER_DRAINS","0");env.put("GOW3_SUBMIT_NO_WAIT","0");
        env.put("GOW3_SKIP_IB_DRAIN","nested".equals(p.getString("perf_drains","conservative"))?"1":"0");
        setting(p,env,"perf_cpu_trims","GOW3_CPU_TRIMS",true);
        setting(p,env,"perf_prep_pool","GOW3_PREP_POOL",true);
        setting(p,env,"perf_async_shaders","GOW3_ASYNC_SHADERS",true);
        setting(p,env,"perf_prefetch","GOW3_PREFETCH_PIPELINES",true);
        setting(p,env,"perf_draw_trim","TLOU_DRAW_COUNTER_TRIM",false);
        setting(p,env,"perf_compute_memo","GOW3_TLOU_COMPUTE_MEMO",false);
        setting(p,env,"perf_copy_batch","GOW3_TLOU_HOST_COPY_BATCH",false);
        setting(p,env,"perf_warm_textures","TLOU_WARM_TEXTURES",false);
        setting(p,env,"perf_mobile_memory","TLOU_MOBILE_MEMORY_POLICY",false);
        setting(p,env,"perf_adpf","TLOU_ADPF",false);
        setting(p,env,"perf_stencil","GOW3_STENCIL_PLANE_COPY",false);
        env.put("GOW3_RAIN_SCALE",p.getBoolean("perf_rain",false)?"0.12":"1");
        // One report path: bounded CPU/frame diagnostics are always available.
        // Full GPU timestamp profiling remains off because it changes performance.
        env.put("TLOU_CPU_PHASE_SAMPLES","1");
        env.remove("GOW3_GPU_PROFILE");
        String buffers=p.getString("perf_present_buffers","3");
        env.put("GOW3_PRESENT_BUFFERS",buffers.matches("[234]")?buffers:"3");
        String workers=p.getString("perf_draw_workers","0");
        env.put("GOW3_PREP_WORKERS",workers.matches("[0124]")?workers:"0");
        String turnip=p.getString("perf_turnip_profile","default");
        env.remove("TU_AUTOTUNE_ALGO");env.remove("TU_AUTOTUNE_FLAGS");
        if(turnip.equals("sysmem"))env.put("TU_AUTOTUNE_ALGO","prefer_sysmem");
        else if(turnip.equals("gmem") || turnip.equals("gmem_small"))env.put("TU_AUTOTUNE_ALGO","prefer_gmem");
        else if(turnip.equals("small"))env.put("TU_AUTOTUNE_ALGO","bandwidth");
        if(turnip.equals("small") || turnip.equals("gmem_small"))env.put("TU_AUTOTUNE_FLAGS","tune_small");
        LaunchOverrides.apply(context,env,false);

    }

    static int scale(Context context) {
        int value;try{value=prefs(context).getInt("render_scale",67);}catch(ClassCastException e){value=67;}
        return Math.max(RenderScale.MIN,Math.min(RenderScale.MAX,value));
    }
    private static void setting(SharedPreferences p,Map<String,String> env,String key,String variable,boolean fallback){
        env.put(variable,p.getBoolean(key,fallback)?"1":"0");
    }

    static String experimentReport(Context context,Map<String,String> env) {
        StringBuilder result=new StringBuilder("TLOU launch settings: internal=")
            .append(prefs(context).getBoolean("custom_scale",false)?RenderScale.label(scale(context)):
                prefs(context).getString("resolution","original"));
        result.append("; renderer=Vulkan; CPU=FEX ARM64");
        if(prefs(context).getBoolean("custom_scale",false))result.append("; render-scale=").append(RenderScale.label(scale(context)));
        result.append("; frame-overlap=").append(env.get("GOW3_FRAMES_IN_FLIGHT"));
        result.append("; nested-drain=").append(env.get("GOW3_SKIP_IB_DRAIN"));
        result.append("; mobile-memory=").append(env.get("TLOU_MOBILE_MEMORY_POLICY"));
        result.append("; adpf=").append(env.get("TLOU_ADPF"));

        result.append("; game-raster=").append(env.getOrDefault("TLOU_EXPECT_SCENE_SIZE","1920x1080"));
        result.append("; game-ui=")
              .append(env.containsKey("TLOU_UNIFIED_GUEST_UI")
                      ?env.getOrDefault("TLOU_EXPECT_SCENE_SIZE","1920x1080")
                      :"1920x1080");
        result.append("; video-decoding=original; movie-plane-layout=")
              .append(env.getOrDefault("GOW3_MOVIE_SIZE","native"));
        result.append("; host-scene=")
              .append(env.getOrDefault("TLOU_EXPECT_HOST_SCENE","native"));
        result.append("; scene-composite=")
              .append(env.containsKey("TLOU_UNIFIED_GUEST_UI")?"unified-direct-no-ui-blit":
                      env.containsKey("TLOU_SPATIAL_SCENE")?"proxy-spatial-compat":"native");
        result.append("; temporal-fsr=off");
        result.append("; geometry-sync=full");
        result.append("; async-global-protect=").append(env.containsKey("GOW3_ASYNC_READBACK")?"on":"off");
        result.append("; geometry-sync policy=2 full only");
        result.append("; guest-job-worker-wake=")
              .append(conservativeJobWorkers()?"original":"patched-sleep");
        return result.toString();
    }

    static String fpsLimit(Context context) {
        return "30".equals(LaunchOverrides.saved(context).getOrDefault("GOW3_FPS_LIMIT",prefs(context).getString("fps_limit","60")))?"30":"60";
    }
    static String summary(Context context) {
        SharedPreferences p=prefs(context);
        String r=p.getString("resolution","original"), limit=fpsLimit(context);
        String res=p.getBoolean("custom_scale",false)?RenderScale.label(scale(context)):
            r.equals("original")?"Original":r+"p";
        return res+"  ·  "+(limit+" FPS")+"  ·  "+
            (p.getBoolean("vsync",true)?"VSync ON":"VSync OFF");
    }
}
