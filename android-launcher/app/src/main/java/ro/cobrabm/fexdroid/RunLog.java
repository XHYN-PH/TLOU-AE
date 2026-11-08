package ro.cobrabm.fexdroid;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Durable launch breadcrumbs exist before JNI, AHB or the FEX child is started. */
final class RunLog {
    static final int EXPORT_REQUEST = 6509;
    static void requestExport(Activity activity) {
        Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        save.setType("application/zip");save.addCategory(Intent.CATEGORY_OPENABLE);
        save.putExtra(Intent.EXTRA_TITLE,"TLOU-AE-Logs.zip");
        activity.startActivityForResult(save,EXPORT_REQUEST);
    }
    static File launch(Context c) { return new File(c.getFilesDir(),"launch.log"); }
    static synchronized void begin(Context c) {
        File current=launch(c), previous=new File(c.getFilesDir(),"launch-previous.log");
        if(current.exists()) { previous.delete(); current.renameTo(previous); }
        write(c,"PLAY • "+new Date()+" • Android "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT+" • "+Build.MANUFACTURER+" "+Build.MODEL);
    }
    static synchronized void write(Context c,String message) {
        try(FileOutputStream out=new FileOutputStream(launch(c),true)) {
            out.write((new Date()+" ["+Thread.currentThread().getName()+"] "+message+"\n").getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch(IOException e) { android.util.Log.e("TLOU","Cannot write launch log",e); }
    }
    static void failure(Context c,String label,Throwable error) {
        StringWriter trace=new StringWriter(); error.printStackTrace(new PrintWriter(trace));
        write(c,label+"\n"+trace);
    }
    static File runtime(Context c) { return new File(c.getFilesDir(),"tlou/runtime.log"); }
    static String tail(File file,int bytes) {
        if(!file.isFile())return "Not created yet.";
        try(RandomAccessFile in=new RandomAccessFile(file,"r")) {
            long end=in.length();in.seek(Math.max(0,end-bytes));byte[] data=new byte[(int)(end-in.getFilePointer())];
            int read=0,n;while(read<data.length && (n=in.read(data,read,data.length-read))>0)read+=n;
            return new String(data,0,read,StandardCharsets.UTF_8);
        }catch(IOException e){return e.toString();}
    }
    static void captureActive(Context c,File prepared,java.util.Map<String,String> env) throws Exception {
        org.json.JSONObject active=new org.json.JSONObject();
        active.put("app","TLOU-AE");active.put("author","XHYN_PH");active.put("captured_at",new Date().toString());
        active.put("next_launch_changes_require_restart",true);
        active.put("render_scale",PortSettings.scale(c));active.put("driver",PortSettings.prefs(c).getString("driver_name","System"));
        active.put("overrides",new org.json.JSONObject(LaunchOverrides.report(c)));
        org.json.JSONObject effective=new org.json.JSONObject();
        for(String key:new String[]{"TLOU_BC_DECODE","TLOU_SRT_RETRY","GOW3_RAYCAST_FLUSH","GOW3_SCREEN_W","GOW3_SCREEN_H","GOW3_DMEM_REPORT_MB","GOW3_FPS_LIMIT","GOW3_VSYNC","GOW3_FRAMES_IN_FLIGHT","GOW3_SKIP_IB_DRAIN","GOW3_PRESENT_BUFFERS","GOW3_PREP_WORKERS","GOW3_READBACKS","TLOU_EXPECT_SCENE_SIZE","TLOU_CPU_PHASE_SAMPLES","TLOU_MOBILE_MEMORY_POLICY","TLOU_ADPF","TU_AUTOTUNE_ALGO","TU_AUTOTUNE_FLAGS"})if(env.containsKey(key))effective.put(key,env.get(key));
        active.put("effective",effective);
        File validation=new File(prepared,"patch-validation.json");
        if(validation.isFile())active.put("validated_patches",new org.json.JSONObject(new String(java.nio.file.Files.readAllBytes(validation.toPath()),StandardCharsets.UTF_8)));
        File target=new File(c.getFilesDir(),"tlou/active-settings.json");java.nio.file.Files.createDirectories(target.getParentFile().toPath());
        File temp=new File(target.getPath()+".new");java.nio.file.Files.write(temp.toPath(),active.toString(2).getBytes(StandardCharsets.UTF_8));
        java.nio.file.Files.move(temp.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    private static void entry(ZipOutputStream zip,String name,String value) throws IOException{
        zip.putNextEntry(new ZipEntry(name));zip.write(value.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
    }
    static String summary(Context c) {
        return "STARTUP\n"+tail(launch(c),10000)+"\n\nRUNTIME\n"+tail(runtime(c),10000)+
            "\n\nINSTALLATION / PREPARATION\n"+tail(new File(c.getFilesDir(),"installation.log"),5000);
    }
    private static String exitReason(int reason){
        switch(reason){
            case ApplicationExitInfo.REASON_LOW_MEMORY:return "LOW_MEMORY";
            case ApplicationExitInfo.REASON_CRASH_NATIVE:return "NATIVE_CRASH";
            case ApplicationExitInfo.REASON_CRASH:return "JAVA_CRASH";
            case ApplicationExitInfo.REASON_ANR:return "APP_NOT_RESPONDING";
            case ApplicationExitInfo.REASON_USER_REQUESTED:return "USER_OR_SYSTEM_CLEANER_STOP";
            case ApplicationExitInfo.REASON_EXIT_SELF:return "SELF_EXIT";
            case ApplicationExitInfo.REASON_SIGNALED:return "SIGNAL";
            default:return "OTHER";
        }
    }
    static void export(Context c,OutputStream output) throws IOException {
        try(ZipOutputStream zip=new ZipOutputStream(output)) {
            entry(zip,"READ-ME.txt","TLOU-AE diagnostic report · XHYN_PH\nCurrent and previous sessions are included. active-settings.json records the running session; performance-settings.json records choices for the next launch.\nDevice samples and display events are in launch.log. Presentation interval percentiles measure surface submissions, not GPU execution time. Long gaps remain counted; they are not automatically classified as loading. Background intervals are excluded.\n");
            entry(zip,"device-state.txt",DiagnosticState.snapshot(c));
            entry(zip,"cache-state.txt",CacheTools.describe(c));
            for(String path:new String[]{"launch.log","launch-previous.log","tlou/runtime.log","tlou/runtime-previous.log","installation.log","tlou-prepared/device-ready.json","tlou-prepared/patch-validation.json","tlou/active-settings.json","tlou/perf-cs-22e6e19e.bin","tlou/perf-cs-61d67259.bin"}) {
                File file=new File(c.getFilesDir(),path);if(!file.isFile())continue;
                zip.putNextEntry(new ZipEntry(path));
                try(RandomAccessFile in=new RandomAccessFile(file,"r")) {
                    // Snapshot the extent: a running game must not grow an export forever.
                    long end=in.length(),start=Math.max(0,end-4L*1024*1024),remaining=end-start;
                    in.seek(start);byte[] bytes=new byte[65536];int n;
                    while(remaining>0 && (n=in.read(bytes,0,(int)Math.min(remaining,bytes.length)))!=-1){zip.write(bytes,0,n);remaining-=n;}
                }
                zip.closeEntry();
            }
            // Training builds only. Never truncate a binary profile or copy one from a live child.
            int pid=RuntimeProcess.readPid(new File(c.getFilesDir(),"tlou/runtime.pid"));boolean running=false;
            if(pid>0)try{android.system.Os.kill(pid,0);running=true;}catch(android.system.ErrnoException ignored){}
            if(!running){
                File[] profiles=new File(c.getFilesDir(),"tlou/profiles").listFiles((dir,name)->name.endsWith(".profraw"));
                if(profiles!=null){
                    java.util.Arrays.sort(profiles,java.util.Comparator.comparingLong(File::lastModified).reversed());
                    for(int index=0;index<Math.min(8,profiles.length);index++){
                        File file=profiles[index];if(!file.isFile() || file.length()>64L*1024*1024)continue;
                        zip.putNextEntry(new ZipEntry("profiles/"+file.getName()));
                        try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)zip.write(buffer,0,n);}
                        zip.closeEntry();
                    }
                }
            }
            zip.putNextEntry(new ZipEntry("performance-settings.json"));
            org.json.JSONObject settings=new org.json.JSONObject();
            for(java.util.Map.Entry<String,?> entry:PortSettings.prefs(c).getAll().entrySet()) {
                String key=entry.getKey();
                if(key.startsWith("perf_") || key.equals("resolution") || key.equals("custom_scale") ||
                   key.equals("render_scale") || key.equals("fps_limit") || key.equals("driver_name") ||
                   key.equals("vsync") || java.util.Arrays.asList(PortSettings.EFFECTS).contains(key)) {
                    try{settings.put(key,entry.getValue());}catch(org.json.JSONException e){throw new IOException(e);}
                }
            }
            try{settings.put("launch_overrides",new org.json.JSONObject(LaunchOverrides.report(c)));}catch(org.json.JSONException | IllegalArgumentException error){throw new IOException(error);}
            zip.write(settings.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            zip.putNextEntry(new ZipEntry("android-process-exits.txt"));
            try {
                ActivityManager manager=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
                if(manager!=null)for(ApplicationExitInfo exit:manager.getHistoricalProcessExitReasons(c.getPackageName(),0,5)) {
                    String line=new Date(exit.getTimestamp())+" process="+exit.getProcessName()+" reason="+exitReason(exit.getReason())+" ("+exit.getReason()+")"+" status="+exit.getStatus()+" description="+exit.getDescription()+"\n";
                    zip.write(line.getBytes(StandardCharsets.UTF_8));
                }
            } catch(RuntimeException e) { zip.write(e.toString().getBytes(StandardCharsets.UTF_8)); }
            zip.closeEntry();
        }
    }
}
