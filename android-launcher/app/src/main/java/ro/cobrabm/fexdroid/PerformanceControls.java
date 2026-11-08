package ro.cobrabm.fexdroid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.*;

/** Shared launcher/game settings. Every native option is applied on next launch. */
final class PerformanceControls {
    private static final String PREFIX="perf_";
    static void text(Activity a,LinearLayout root,String value) {
        TextView t=new TextView(a);t.setText(value);t.setTextColor(0xffe7e9df);
        t.setPadding(0,14,0,10);root.addView(t);
    }
    static void toggle(Activity a,LinearLayout root,String key,String title,boolean fallback,Runnable changed) {
        SharedPreferences p=PortSettings.prefs(a);
        Switch view=new Switch(a);view.setText(title);view.setTextColor(0xffe7e9df);
        view.setChecked(p.getBoolean(key,fallback));
        view.setOnCheckedChangeListener((v,on)->{p.edit().putBoolean(key,on).apply();changed.run();});root.addView(view);
    }
    static void choice(Activity a,LinearLayout root,String key,String title,String[] labels,String[] values,String fallback,Runnable changed) {
        text(a,root,title);SharedPreferences p=PortSettings.prefs(a);
        Spinner view=new Spinner(a);view.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,labels));
        String value=p.getString(key,fallback);int selected=0;
        for(int i=0;i<values.length;i++)if(values[i].equals(value))selected=i;
        view.setSelection(selected);view.setTag(selected);
        view.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View child,int index,long id){
                if((Integer)view.getTag()==index)return;view.setTag(index);
                p.edit().putString(key,values[index]).apply();changed.run();
            }
        });root.addView(view);
    }
    static String effectHelp(String key){
        switch(key){
            case "ssao":return "Disabling ambient occlusion may reduce GPU work; contact shadows become flatter.";
            case "motion_blur":return "Disabling motion blur sharpens movement and may reduce postprocessing work.";
            case "depth_of_field":return "Disabling depth of field removes cinematic background blur and may reduce GPU work.";
            case "bloom":return "Disabling bloom removes bright-light glow; performance changes depend on the scene.";
            case "sunlight_shadows":return "Disabling sunlight shadows can reduce outdoor rendering work but removes directional shadows.";
            case "subsurface":return "Disabling subsurface changes skin/material lighting and may reduce shader work.";
            default:return "Requires restarting the game. Performance effects depend on the scene.";
        }
    }
    static void resolution(Activity a,LinearLayout root,Runnable changed) {
        PortSettings.migrateResolution(a);
        text(a,root,"Render resolution · one setting for scene, depth and aiming");
        text(a,root,"Lower values reduce GPU pixel work and image quality. 01.00 supports custom percentages; verified 01.11 supports 33/50/67/100% presets. Close the game and press Play to apply; reopening a menu does not apply patches.");
        int value=PortSettings.scale(a);
        TextView label=new TextView(a);label.setTextColor(0xffe7e9df);label.setText(RenderScale.label(value));root.addView(label);
        SeekBar bar=new SeekBar(a);bar.setMax(RenderScale.MAX-RenderScale.MIN);bar.setProgress(value-RenderScale.MIN);
        bar.setContentDescription("Render resolution percentage");
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){changed.run();}
            public void onProgressChanged(SeekBar b,int progress,boolean fromUser){
                int percent=progress+RenderScale.MIN;label.setText(RenderScale.label(percent));
                if(fromUser)PortSettings.setScale(a,percent);
            }
        });root.addView(bar);
        LinearLayout presets=new LinearLayout(a);
        for(int percent:new int[]{33,50,67,100}){
            Button button=new Button(a);button.setText(percent+"%");
            button.setOnClickListener(v->{PortSettings.setScale(a,percent);bar.setProgress(percent-RenderScale.MIN);changed.run();});
            presets.addView(button,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(presets);
        text(a,root,"100% = 1920×1080; 67% = 1280×720; 50% = 960×540; 33% = 640×360. Dimensions round to supported steps. Minimum 10%; 0% cannot render.");
    }
    private static void option(Activity a,LinearLayout root,String key,String title,boolean fallback,String help,Runnable changed){
        toggle(a,root,key,title,fallback,changed);text(a,root,help);
    }
    static void advanced(Activity a,LinearLayout parent,Runnable changed) {
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);
        parent.addView(root,new LinearLayout.LayoutParams(-1,-2));
        advancedContents(a,root,changed);
    }
    private static void advancedContents(Activity a,LinearLayout root,Runnable changed) {
        text(a,root,"Performance · game restart required");
        choice(a,root,"perf_overlap","Frame overlap",new String[]{"Off (conservative)","1 frame (experimental)","2 frames (experimental)"},new String[]{"0","1","2"},"0",changed);
        text(a,root,"Overlapping CPU/GPU work may increase throughput, memory use and input delay. It can cause timing or physics regressions; more overlap is not always faster.");
        choice(a,root,"perf_drains","Pipeline draining",new String[]{"Conservative","Nested compute buffers only (experimental)"},new String[]{"conservative","nested"},"conservative",changed);
        text(a,root,"Fewer nested waits may improve CPU throughput but can expose ordering errors. Broad no-wait modes stay unavailable.");
        choice(a,root,"perf_display_rate","Display refresh policy",new String[]{"Match game FPS (default)","System default","60 Hz","90 Hz","120 Hz"},new String[]{"match","system","60","90","120"},"match",changed);
        text(a,root,"Requests a refresh cadence compatible with the FPS cap and reapplies it after surface/resume changes. Android may override it. Does not force GPU clocks.");
        option(a,root,"perf_cpu_trims","Reduce CPU overhead",true,"Usually reduces renderer bookkeeping. Compare if a scene develops timing problems.",changed);
        option(a,root,"perf_prep_pool","Reuse renderer buffers",true,"Reduces allocation churn and CPU work; retains a small buffer pool.",changed);
        option(a,root,"perf_async_shaders","Compile shaders in background",true,"Can reduce shader hitches. Newly encountered effects can be delayed while compilation finishes.",changed);
        option(a,root,"perf_prefetch","Prefetch shader pipelines",true,"Can reduce later shader stalls; increases startup work and temporary CPU/memory use.",changed);
        option(a,root,"perf_draw_trim","Reduce draw statistics",true,"Reduces draw-counter work. Expected benefit is small; does not reduce game draw calls.",changed);
        option(a,root,"perf_compute_memo","Cache repeated compute setup (experimental)",false,"May reduce repeated CPU setup. Compare output correctness; no measured gain is promised.",changed);
        option(a,root,"perf_copy_batch","Batch compatible host copies (experimental)",false,"May reduce copy submission overhead. Can change timing and needs scene validation.",changed);
        option(a,root,"perf_warm_textures","Keep recently used textures warm",false,"Can reduce repeated uploads; uses more memory. Retention yields to real memory pressure.",changed);
        option(a,root,"perf_mobile_memory","Adapt to mobile RAM pressure",false,"Adjusts cache allowance using available RAM, swap and pressure. Can reduce eviction churn; lower budgets can also cause stutter.",changed);
        option(a,root,"perf_adpf","Android performance hints and thermal reporting",false,"Hints about CPU work may help supported phones. Can increase power use; thermal limits remain active.",changed);
        option(a,root,"perf_stencil","Stencil/decal correction (experimental)",false,"Visual correction that may add GPU work and lower FPS. Leave off when measuring speed.",changed);
        option(a,root,"perf_rain","Reduced rain opacity (experimental)",false,"Changes rain appearance. Lower opacity does not necessarily reduce GPU work or improve FPS.",changed);
        choice(a,root,"perf_bc_decode","BC texture GPU fallback",new String[]{"Auto (recommended)","Off · native textures only","Force GPU decode (diagnostic)"},new String[]{"auto","off","force"},"auto",changed);
        text(a,root,"Auto decodes only formats the driver cannot sample. Usually unnecessary on Adreno 750. Force can add GPU work and use 2–8× more texture storage. Off stops with an explanation if the driver lacks BC support. Restart required.");
        option(a,root,"perf_srt_retry","Recover native resource walkers (experimental)",false,"Retries native ARM64 resource decoding after temporary faults with bounded backoff. May reduce interpreter work; compare correctness and frame times. Off preserves the previous permanent fallback.",changed);
        option(a,root,"perf_lazy_eop","Batch EOP GPU submissions (experimental)",false,"May reduce Vulkan submission overhead. Changes scheduling and can cause stalls or rendering/physics regressions. Required readbacks remain; off uses immediate submission.",changed);
        choice(a,root,"perf_output_size","Presentation output size",new String[]{"Physical display (default)","Follow render size (experimental)"},new String[]{"native","scene"},"native",changed);
        text(a,root,"Changes the final output buffers only. Following render size lets Android scale the completed image and may reduce bandwidth/power. It can soften the picture; scene resolution is still controlled by the single render slider.");
        text(a,root,"Additional renderer options");
        choice(a,root,"perf_present_buffers","Presentation buffers",new String[]{"2 (less queued work)","3 (default)","4 (more buffering)"},new String[]{"2","3","4"},"3",changed);
        text(a,root,"More buffers may smooth scheduling but increase memory and latency. Fewer can limit throughput.");
        choice(a,root,"perf_draw_workers","Draw preparation workers",new String[]{"Off (default)","1 worker","2 workers","4 workers"},new String[]{"0","1","2","4"},"0",changed);
        text(a,root,"Extra workers may help CPU-bound scenes, but compete with FEX/game threads and increase power use. Off keeps the previous Android baseline.");
        option(a,root,"perf_sustained","Android sustained performance",false,"Supported phones may trade peak speed for steadier long-session performance. This is a power hint, not another FPS limiter.",changed);
        choice(a,root,"perf_turnip_profile","Turnip rendering profile",new String[]{"Driver default","Prefer system memory","Prefer tile memory","Tune small passes","Tile memory + small passes"},new String[]{"default","sysmem","gmem","small","gmem_small"},"default",changed);
        text(a,root,"Experimental driver hints adapted from PT4Droid. Compare the same scene after restarting. Older drivers can ignore these hints; no profile is universally fastest.");
        text(a,root,"Diagnostics are collected automatically in one report. Use Create diagnostic report from Settings or the game menu; there is no CPU/GPU mode to choose.");
        text(a,root,"Advanced launch variables · NAME=VALUE, one per line");
        text(a,root,"Saved overrides take precedence over matching toggles on the next launch. SNAFU_RAW_ERROR_MESSAGES=1 applies to installer errors; GOW3_FRAMES_IN_FLIGHT=0 controls runtime overlap. TLOU_FIXES=none disables base stability fixes during preparation and can restore hangs; graphics choices remain separate.");
        EditText variables=new EditText(a);variables.setTextColor(0xffe7e9df);variables.setMinLines(3);variables.setSingleLine(false);
        variables.setContentDescription("Advanced launch variables");variables.setText(PortSettings.prefs(a).getString("launch_overrides",""));root.addView(variables);
        TextView effective=new TextView(a);effective.setTextColor(0xffe7e9df);
        try{effective.setText("Active overrides: "+LaunchOverrides.report(a));}catch(IllegalArgumentException error){effective.setText(error.getMessage());}root.addView(effective);
        Button save=new Button(a);save.setText("Save launch variables");save.setOnClickListener(v->{
            try{LaunchOverrides.parse(variables.getText().toString());PortSettings.prefs(a).edit().putString("launch_overrides",variables.getText().toString()).apply();effective.setText("Active overrides: "+LaunchOverrides.report(a));changed.run();}
            catch(IllegalArgumentException error){new android.app.AlertDialog.Builder(a).setTitle("Invalid launch variable").setMessage(error.getMessage()).setPositiveButton("OK",null).show();}
        });root.addView(save);
        Button clear=new Button(a);clear.setText("Clear launch variables");clear.setOnClickListener(v->{PortSettings.prefs(a).edit().remove("launch_overrides").apply();variables.setText("");effective.setText("Active overrides: none");changed.run();});root.addView(clear);
        CacheTools.controls(a,root);
        Button reset=new Button(a);reset.setText("Reset advanced performance settings");
        reset.setOnClickListener(v->{reset(PortSettings.prefs(a));root.removeAllViews();advancedContents(a,root,changed);changed.run();});root.addView(reset);
    }
    static void reset(SharedPreferences p) {
        SharedPreferences.Editor edit=p.edit().remove("launch_overrides");
        for(String key:p.getAll().keySet())if(key.startsWith(PREFIX))edit.remove(key);
        edit.apply();
    }
}
