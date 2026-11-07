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
    static void resolution(Activity a,LinearLayout root,Runnable changed) {
        SharedPreferences p=PortSettings.prefs(a);
        text(a,root,"Custom render scale · restart game to apply");
        toggle(a,root,"custom_scale","Use percentage scale (01.00 game only)",false,()->{
            if(p.getBoolean("custom_scale",false))p.edit().putString("render_path","guest").apply();
            changed.run();
        });
        int value=PortSettings.scale(a);
        TextView label=new TextView(a);label.setTextColor(0xffe7e9df);label.setText(RenderScale.label(value));root.addView(label);
        SeekBar bar=new SeekBar(a);bar.setMax(RenderScale.MAX-RenderScale.MIN);bar.setProgress(value-RenderScale.MIN);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){changed.run();}
            public void onProgressChanged(SeekBar b,int progress,boolean fromUser){
                int percent=progress+RenderScale.MIN;label.setText(RenderScale.label(percent));
                if(fromUser)p.edit().putInt("render_scale",percent).apply();
            }
        });root.addView(bar);
        text(a,root,"100% = original 1080p; dimensions round to supported 16:9 steps. 0% cannot render. Low scales are experimental.");
    }
    static void advanced(Activity a,LinearLayout parent,Runnable changed) {
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);
        parent.addView(root,new LinearLayout.LayoutParams(-1,-2));
        advancedContents(a,root,changed);
    }
    private static void advancedContents(Activity a,LinearLayout root,Runnable changed) {
        text(a,root,"Advanced performance · changes require restarting the game");
        choice(a,root,"perf_overlap","Guest frame overlap (experimental)",new String[]{"Off (default)","1 frame","2 frames"},new String[]{"0","1","2"},"0",changed);
        choice(a,root,"perf_drains","Pipeline draining",new String[]{"Conservative (default)","Nested compute buffers only (experimental)"},new String[]{"conservative","nested"},"conservative",changed);
        text(a,root,"The broader PC mode is unavailable because it can cause rendering or physics errors.");
        toggle(a,root,"perf_cpu_trims","Reduce CPU overhead",true,changed);
        toggle(a,root,"perf_prep_pool","Reuse renderer buffers",true,changed);
        toggle(a,root,"perf_async_shaders","Compile shaders in background",true,changed);
        toggle(a,root,"perf_prefetch","Prefetch shader pipelines",true,changed);
        toggle(a,root,"perf_draw_trim","Reduce draw statistics (experimental)",false,changed);
        toggle(a,root,"perf_compute_memo","Cache repeated compute setup (experimental)",false,changed);
        toggle(a,root,"perf_copy_batch","Batch compatible host copies (experimental)",false,changed);
        toggle(a,root,"perf_warm_textures","Keep recently used textures warm",false,changed);
        toggle(a,root,"perf_mobile_memory","Adapt to mobile RAM pressure",false,changed);
        toggle(a,root,"perf_adpf","Android performance hints and thermal reporting",false,changed);
        toggle(a,root,"perf_stencil","Stencil/decal correction (experimental)",false,changed);
        toggle(a,root,"perf_rain","Reduced rain opacity (experimental)",false,changed);
        choice(a,root,"perf_diagnostics","Diagnostics",new String[]{"Normal","CPU phases","GPU passes (affects performance)"},new String[]{"normal","cpu","gpu"},"normal",changed);
        Button reset=new Button(a);reset.setText("Reset advanced performance settings");
        reset.setOnClickListener(v->{reset(PortSettings.prefs(a));root.removeAllViews();advancedContents(a,root,changed);changed.run();});root.addView(reset);
    }
    static void reset(SharedPreferences p) {
        SharedPreferences.Editor edit=p.edit();
        for(String key:p.getAll().keySet())if(key.startsWith(PREFIX))edit.remove(key);
        edit.apply();
    }
}
